package com.forum.common.security;

import com.forum.common.api.ApiResponse;
import com.forum.common.api.ResultCode;
import com.forum.common.exception.BusinessException;
import com.forum.rbac.entity.PermissionCodes;
import com.forum.rbac.entity.RoleCodes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Authorisation behaviour of the security helpers.
 *
 * <p>Covers the distinction the rest of the code relies on: a missing session
 * is {@code 40100}, a present session lacking a permission is {@code 40300},
 * and a resource that is not yours reads as {@code 40400} so an id cannot be
 * probed for existence.
 */
class AuthorizationTest {

    private static final String SECRET = "unit-test-secret-that-is-long-enough-for-hs256";

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(SECRET, "forum-server-test", 3600, 7200);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void signIn(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @Nested
    @DisplayName("CurrentUser")
    class CurrentUserTest {

        @Test
        @DisplayName("requireUserId rejects a request with no session")
        void requireUserIdWithoutSession() {
            assertThatThrownBy(CurrentUser::requireUserId)
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ResultCode.UNAUTHORIZED.getCode());
        }

        @Test
        @DisplayName("find is empty rather than throwing for an anonymous caller")
        void findIsEmptyForAnonymous() {
            assertThat(CurrentUser.find()).isEmpty();
            assertThat(CurrentUser.findUserId()).isEmpty();
        }

        @Test
        @DisplayName("a signed in principal is exposed by id")
        void exposesUserId() {
            signIn(new AuthenticatedUser(7L, "alice", List.of(RoleCodes.USER)));

            assertThat(CurrentUser.requireUserId()).isEqualTo(7L);
            assertThat(CurrentUser.require().getUsername()).isEqualTo("alice");
        }
    }

    @Nested
    @DisplayName("authority checks")
    class AuthorityTest {

        @Test
        @DisplayName("hasAuthority matches an exact code and rejects anything else")
        void exactMatchOnly() {
            AuthenticatedUser user = new AuthenticatedUser(
                    7L, "alice", List.of(PermissionCodes.POST_TOP, RoleCodes.ADMIN));

            assertThat(user.hasAuthority(PermissionCodes.POST_TOP)).isTrue();
            assertThat(user.hasAuthority(PermissionCodes.POST_AUDIT)).isFalse();
        }

        @Test
        @DisplayName("a user without a permission is forbidden, not unauthorized")
        void missingPermissionIsForbidden() {
            signIn(new AuthenticatedUser(7L, "alice", List.of()));

            boolean allowed = CurrentUser.find()
                    .map(user -> user.hasAuthority(PermissionCodes.REPORT_HANDLE))
                    .orElse(false);
            assertThat(allowed).isFalse();

            // The distinction matters: the caller is signed in.
            assertThat(BusinessException.forbidden("nope").getCode())
                    .isEqualTo(ResultCode.FORBIDDEN.getCode())
                    .isNotEqualTo(ResultCode.UNAUTHORIZED.getCode());
        }
    }

    @Nested
    @DisplayName("JwtTokenProvider")
    class TokenTest {

        @Test
        @DisplayName("an access token round trips id, username and authorities")
        void accessTokenRoundTrip() {
            String token = tokenProvider.createAccessToken(
                    7L, "alice", List.of(RoleCodes.ADMIN, PermissionCodes.POST_AUDIT));

            AuthenticatedUser parsed = tokenProvider.parseAccessToken(token);

            assertThat(parsed.getUserId()).isEqualTo(7L);
            assertThat(parsed.getUsername()).isEqualTo("alice");
            assertThat(parsed.getAuthorities())
                    .containsExactly(RoleCodes.ADMIN, PermissionCodes.POST_AUDIT);
        }

        @Test
        @DisplayName("a refresh token is refused where an access token is expected")
        void tokenTypesAreNotInterchangeable() {
            String refresh = tokenProvider.createRefreshToken(7L, "alice");

            assertThatThrownBy(() -> tokenProvider.parseAccessToken(refresh))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code")
                    .isEqualTo(ResultCode.UNAUTHORIZED.getCode());

            assertThat(tokenProvider.parseRefreshTokenSubject(refresh)).isEqualTo(7L);
        }

        @Test
        @DisplayName("a tampered token is rejected")
        void tamperedTokenRejected() {
            String token = tokenProvider.createAccessToken(7L, "alice", List.of());
            String tampered = token.substring(0, token.length() - 2) + "xy";

            assertThatThrownBy(() -> tokenProvider.parseAccessToken(tampered))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("a token signed with another key is rejected")
        void foreignSignatureRejected() {
            JwtTokenProvider other = new JwtTokenProvider(
                    "a-completely-different-secret-of-32-bytes-plus", "forum-server-test", 3600, 7200);
            String foreign = other.createAccessToken(7L, "alice", List.of());

            assertThatThrownBy(() -> tokenProvider.parseAccessToken(foreign))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("the issuer is checked, so a token from another app is rejected")
        void issuerIsChecked() {
            JwtTokenProvider otherIssuer =
                    new JwtTokenProvider(SECRET, "some-other-service", 3600, 7200);
            String foreign = otherIssuer.createAccessToken(7L, "alice", List.of());

            assertThatThrownBy(() -> tokenProvider.parseAccessToken(foreign))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("fingerprints are stable and hide the raw token")
        void fingerprintIsStable() {
            String token = tokenProvider.createAccessToken(7L, "alice", List.of());

            assertThat(tokenProvider.fingerprint(token)).isEqualTo(tokenProvider.fingerprint(token));
            assertThat(tokenProvider.fingerprint(token)).isNotEqualTo(token);
            assertThat(tokenProvider.fingerprint(token)).hasSize(64);
        }

        @Test
        @DisplayName("a short signing secret fails fast at construction")
        void shortSecretRejected() {
            assertThatThrownBy(() -> new JwtTokenProvider("too-short", "forum-server-test", 3600, 7200))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("32 bytes");
        }
    }

    @Nested
    @DisplayName("result codes")
    class CodesTest {

        @Test
        @DisplayName("the documented codes are the ones the factories produce")
        void codesMatchTheContract() {
            assertThat(ApiResponse.failure(ResultCode.UNAUTHORIZED).getCode()).isEqualTo(40100);
            assertThat(ApiResponse.failure(ResultCode.FORBIDDEN).getCode()).isEqualTo(40300);
            assertThat(ApiResponse.failure(ResultCode.NOT_FOUND).getCode()).isEqualTo(40400);
            assertThat(ApiResponse.failure(ResultCode.CONFLICT).getCode()).isEqualTo(40900);
            assertThat(ApiResponse.failure(ResultCode.TOO_MANY_REQUESTS).getCode()).isEqualTo(42900);
            assertThat(ApiResponse.failure(ResultCode.INTERNAL_ERROR).getCode()).isEqualTo(50000);
            assertThat(ApiResponse.success().getCode()).isZero();
        }
    }
}
