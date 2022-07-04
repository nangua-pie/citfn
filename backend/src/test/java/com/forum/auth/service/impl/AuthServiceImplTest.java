package com.forum.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.forum.auth.dto.ChangePasswordRequest;
import com.forum.auth.dto.LoginRequest;
import com.forum.auth.dto.RegisterRequest;
import com.forum.auth.dto.TokenResponse;
import com.forum.auth.entity.TokenRecord;
import com.forum.auth.mapper.TokenMapper;
import com.forum.auth.service.LoginAttemptService;
import com.forum.auth.service.VerifyCodeService;
import com.forum.common.exception.BusinessException;
import com.forum.common.security.JwtTokenProvider;
import com.forum.common.security.TokenBlacklistService;
import com.forum.rbac.entity.Role;
import com.forum.rbac.entity.RoleCodes;
import com.forum.rbac.mapper.RoleMapper;
import com.forum.rbac.mapper.UserRoleMapper;
import com.forum.sensitive.service.SensitiveWordService;
import com.forum.user.entity.User;
import com.forum.user.entity.UserStatus;
import com.forum.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for registration and login.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    private static final String SECRET = "unit-test-secret-that-is-long-enough-for-hs256";

    @Mock
    private UserMapper userMapper;
    @Mock
    private TokenMapper tokenMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private VerifyCodeService verifyCodeService;
    @Mock
    private SensitiveWordService sensitiveWordService;

    private PasswordEncoder passwordEncoder;
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        JwtTokenProvider tokenProvider =
                new JwtTokenProvider(SECRET, "forum-server-test", 3600, 7200);
        service = new AuthServiceImpl(userMapper, tokenMapper, roleMapper, userRoleMapper,
                passwordEncoder, tokenProvider, tokenBlacklistService, loginAttemptService,
                verifyCodeService, sensitiveWordService);
        when(sensitiveWordService.mask(anyString())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    @DisplayName("register stores a BCrypt hash, never the plain password")
    void registerHashesPassword() {
        when(userMapper.countByUsername("alice")).thenReturn(0);
        when(userMapper.countByEmail("alice@example.com")).thenReturn(0);
        when(verifyCodeService.consume("alice@example.com", "REGISTER", "123456")).thenReturn(true);
        when(userMapper.selectRoleCodes(any())).thenReturn(Collections.singletonList(RoleCodes.USER));
        when(userMapper.selectAuthorities(any())).thenReturn(Collections.emptyList());
        when(userMapper.insert(any(User.class))).thenAnswer(call -> {
            ((User) call.getArgument(0)).setId(7L);
            return 1;
        });
        Role role = new Role();
        role.setId(1L);
        role.setCode(RoleCodes.USER);
        when(roleMapper.selectByCode(RoleCodes.USER)).thenReturn(role);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("secret123");
        request.setEmail("alice@example.com");
        request.setCode("123456");

        TokenResponse response = service.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        String stored = captor.getValue().getPasswordHash();
        assertThat(stored).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", stored)).isTrue();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        // Nickname defaults to the username when the client omits it.
        assertThat(captor.getValue().getNickname()).isEqualTo("alice");
    }

    @Test
    @DisplayName("register refuses a taken username")
    void registerRefusesDuplicateUsername() {
        when(userMapper.countByUsername("alice")).thenReturn(1);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("secret123");
        request.setEmail("alice@example.com");
        request.setCode("123456");

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40900);

        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("register refuses an invalid verification code")
    void registerRefusesBadCode() {
        when(userMapper.countByUsername("alice")).thenReturn(0);
        when(userMapper.countByEmail("alice@example.com")).thenReturn(0);
        when(verifyCodeService.consume("alice@example.com", "REGISTER", "000000")).thenReturn(false);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("secret123");
        request.setEmail("alice@example.com");
        request.setCode("000000");

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40000);

        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("login succeeds with the right password and clears the failure counter")
    void loginSucceeds() {
        User user = activeUser();
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);
        when(userMapper.selectRoleCodes(7L)).thenReturn(Collections.singletonList(RoleCodes.USER));
        when(userMapper.selectAuthorities(7L)).thenReturn(Collections.emptyList());

        TokenResponse response = service.login(loginRequest("secret123"), "10.0.0.1");

        assertThat(response.getAccessToken()).isNotBlank();
        verify(loginAttemptService).recordSuccess("alice");
        // A refresh token is persisted as a fingerprint, not as raw text.
        ArgumentCaptor<TokenRecord> captor = ArgumentCaptor.forClass(TokenRecord.class);
        verify(tokenMapper).insert(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo(response.getRefreshToken());
        assertThat(captor.getValue().getTokenType()).isEqualTo("REFRESH");
    }

    @Test
    @DisplayName("login records a failure and stays quiet about which part was wrong")
    void loginRecordsFailureOnWrongPassword() {
        User user = activeUser();
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);

        assertThatThrownBy(() -> service.login(loginRequest("wrong-pass"), "10.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("invalid username or password")
                .extracting("code")
                .isEqualTo(40100);

        verify(loginAttemptService).recordFailure("alice");
    }

    @Test
    @DisplayName("an unknown account gets the same message as a wrong password")
    void loginUnknownUserIsIndistinguishable() {
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> service.login(loginRequest("secret123"), "10.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("invalid username or password");

        verify(loginAttemptService).recordFailure("alice");
    }

    @Test
    @DisplayName("a banned account cannot log in")
    void loginRefusedWhenBanned() {
        User user = activeUser();
        user.setStatus(UserStatus.BANNED.getCode());
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);

        assertThatThrownBy(() -> service.login(loginRequest("secret123"), "10.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40300);
    }

    @Test
    @DisplayName("changing the password revokes every existing session")
    void changePasswordRevokesSessions() {
        User user = activeUser();
        when(userMapper.selectById(7L)).thenReturn(user);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("secret123");
        request.setNewPassword("newsecret456");

        service.changePassword(7L, request);

        verify(userMapper).updateById(any(User.class));
        verify(tokenMapper).revokeAllForUser(eq(7L), any());
    }

    @Test
    @DisplayName("changing the password requires the correct current one")
    void changePasswordRefusedOnWrongCurrent() {
        User user = activeUser();
        when(userMapper.selectById(7L)).thenReturn(user);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("not-it");
        request.setNewPassword("newsecret456");

        assertThatThrownBy(() -> service.changePassword(7L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40000);

        verify(tokenMapper, never()).revokeAllForUser(any(), any());
    }

    private User activeUser() {
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPasswordHash(passwordEncoder.encode("secret123"));
        user.setNickname("alice");
        user.setLevel(1);
        user.setPoints(0);
        user.setStatus(UserStatus.ACTIVE.getCode());
        user.setIsDeleted(0);
        return user;
    }

    private LoginRequest loginRequest(String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword(password);
        return request;
    }
}
