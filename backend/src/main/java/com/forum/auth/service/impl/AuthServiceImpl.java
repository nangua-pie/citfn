package com.forum.auth.service.impl;

import com.forum.auth.dto.ChangePasswordRequest;
import com.forum.auth.dto.LoginRequest;
import com.forum.auth.dto.RefreshTokenRequest;
import com.forum.auth.dto.RegisterRequest;
import com.forum.auth.dto.SendCodeRequest;
import com.forum.auth.dto.TokenResponse;
import com.forum.auth.entity.TokenRecord;
import com.forum.auth.mapper.TokenMapper;
import com.forum.auth.service.AuthService;
import com.forum.auth.service.LoginAttemptService;
import com.forum.auth.service.VerifyCodeService;
import com.forum.common.exception.BusinessException;
import com.forum.common.security.JwtTokenProvider;
import com.forum.common.security.TokenBlacklistService;
import com.forum.rbac.entity.RoleCodes;
import com.forum.rbac.entity.UserRole;
import com.forum.rbac.mapper.RoleMapper;
import com.forum.rbac.mapper.UserRoleMapper;
import com.forum.sensitive.service.SensitiveWordService;
import com.forum.user.dto.UserProfileVO;
import com.forum.user.entity.User;
import com.forum.user.entity.UserStatus;
import com.forum.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Authentication flows.
 *
 * <p>Passwords are only ever compared through BCrypt and never written to a
 * log. Tokens are recorded as SHA-256 fingerprints so the database cannot be
 * used to impersonate a session.
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final String SCENE_REGISTER = "REGISTER";
    private static final String SCENE_LOGIN = "LOGIN";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";

    private final UserMapper userMapper;
    private final TokenMapper tokenMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final LoginAttemptService loginAttemptService;
    private final VerifyCodeService verifyCodeService;
    private final SensitiveWordService sensitiveWordService;

    public AuthServiceImpl(UserMapper userMapper,
                           TokenMapper tokenMapper,
                           RoleMapper roleMapper,
                           UserRoleMapper userRoleMapper,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider tokenProvider,
                           TokenBlacklistService tokenBlacklistService,
                           LoginAttemptService loginAttemptService,
                           VerifyCodeService verifyCodeService,
                           SensitiveWordService sensitiveWordService) {
        this.userMapper = userMapper;
        this.tokenMapper = tokenMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
        this.loginAttemptService = loginAttemptService;
        this.verifyCodeService = verifyCodeService;
        this.sensitiveWordService = sensitiveWordService;
    }

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userMapper.countByUsername(username) > 0) {
            throw BusinessException.conflict("username is already taken");
        }
        if (userMapper.countByEmail(email) > 0) {
            throw BusinessException.conflict("email is already registered");
        }
        if (!verifyCodeService.consume(email, SCENE_REGISTER, request.getCode())) {
            throw BusinessException.badRequest("verification code is invalid or expired");
        }

        String nickname = request.getNickname() == null || request.getNickname().isBlank()
                ? username
                : sensitiveWordService.mask(request.getNickname().trim());
        if (nickname.length() > 64) {
            throw BusinessException.badRequest("nickname must not exceed 64 characters");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(nickname);
        user.setLevel(1);
        user.setPoints(0);
        user.setIsAdmin(0);
        user.setStatus(UserStatus.ACTIVE.getCode());
        user.setIsDeleted(0);
        userMapper.insert(user);

        assignDefaultRole(user.getId());
        log.info("registered user id={} username={}", user.getId(), username);
        return issueTokens(user);
    }

    @Override
    public TokenResponse login(LoginRequest request, String clientIp) {
        String identifier = request.getUsername().trim();
        loginAttemptService.assertNotLocked(identifier);

        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                        .eq("username", identifier)
                        .or()
                        .eq("email", identifier.toLowerCase()));
        if (user == null || user.getIsDeleted() != null && user.getIsDeleted() == 1) {
            loginAttemptService.recordFailure(identifier);
            // One generic message for unknown user and wrong password so the
            // endpoint does not confirm which usernames exist.
            throw BusinessException.unauthorized("invalid username or password");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(identifier);
            throw BusinessException.unauthorized("invalid username or password");
        }
        if (!UserStatus.of(user.getStatus()).canLogin()) {
            throw BusinessException.forbidden("this account has been banned");
        }
        if (UserStatus.of(user.getStatus()) == UserStatus.MUTED && request.getCode() != null
                && !verifyCodeService.consume(user.getEmail(), SCENE_LOGIN, request.getCode())) {
            throw BusinessException.badRequest("a verification code is required for this account");
        }

        loginAttemptService.recordSuccess(identifier);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(clientIp);
        userMapper.updateById(user);
        return issueTokens(user);
    }

    @Override
    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        Long userId = tokenProvider.parseRefreshTokenSubject(request.getRefreshToken());
        String fingerprint = tokenProvider.fingerprint(request.getRefreshToken());
        TokenRecord record = tokenMapper.selectActiveByHash(fingerprint);
        if (record == null || !record.getUserId().equals(userId)) {
            throw BusinessException.unauthorized("refresh token is no longer valid");
        }
        // Rotate: the presented refresh token is spent as the new pair is issued.
        tokenMapper.revokeByHash(fingerprint, LocalDateTime.now());

        User user = userMapper.selectById(userId);
        if (user == null || !UserStatus.of(user.getStatus()).canLogin()) {
            throw BusinessException.unauthorized("account is no longer active");
        }
        return issueTokens(user);
    }

    @Override
    public void logout(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        tokenBlacklistService.revoke(accessToken);
        tokenMapper.revokeByHash(tokenProvider.fingerprint(accessToken), LocalDateTime.now());
        log.info("user logged out");
    }

    @Override
    @Transactional
    public void logoutAll(Long userId) {
        tokenMapper.revokeAllForUser(userId, LocalDateTime.now());
        log.info("revoked all tokens for user id={}", userId);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("user not found");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw BusinessException.badRequest("current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw BusinessException.badRequest("new password must differ from the current one");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        // Every existing session must re-authenticate with the new password.
        tokenMapper.revokeAllForUser(userId, LocalDateTime.now());
        log.info("password changed for user id={}", userId);
    }

    @Override
    public void sendCode(SendCodeRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (SCENE_REGISTER.equals(request.getScene()) && userMapper.countByEmail(email) > 0) {
            throw BusinessException.conflict("email is already registered");
        }
        String code = verifyCodeService.issue(email, request.getScene());
        // No mail transport is wired up in this build. Deployments replace this
        // with the real sender; the value is logged so local flows remain usable.
        log.info("verification code for scene={}: {}", request.getScene(), code);
    }

    private void assignDefaultRole(Long userId) {
        var role = roleMapper.selectByCode(RoleCodes.USER);
        if (role != null && userRoleMapper.selectRoleId(userId, role.getId()) == null) {
            userRoleMapper.insert(UserRole.of(userId, role.getId()));
        }
    }

    private TokenResponse issueTokens(User user) {
        List<String> authorities = new ArrayList<>(userMapper.selectRoleCodes(user.getId()));
        authorities.addAll(userMapper.selectAuthorities(user.getId()));

        String accessToken = tokenProvider.createAccessToken(user.getId(), user.getUsername(), authorities);
        String refreshToken = tokenProvider.createRefreshToken(user.getId(), user.getUsername());
        persistRefreshToken(user.getId(), refreshToken);

        UserProfileVO profile = UserProfileVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .level(user.getLevel())
                .points(user.getPoints())
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .build();
        return new TokenResponse(accessToken, refreshToken, tokenProvider.getAccessTokenTtlSeconds(), profile);
    }

    private void persistRefreshToken(Long userId, String refreshToken) {
        TokenRecord record = new TokenRecord();
        record.setUserId(userId);
        record.setTokenHash(tokenProvider.fingerprint(refreshToken));
        record.setTokenType(TOKEN_TYPE_REFRESH);
        record.setExpiresAt(LocalDateTime.now().plusSeconds(tokenProvider.getRefreshTokenTtlSeconds()));
        record.setRevoked(0);
        record.setCreateTime(LocalDateTime.now());
        tokenMapper.insert(record);
    }
}
