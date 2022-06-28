package com.forum.user.service.impl;

import com.forum.common.exception.BusinessException;
import com.forum.message.mapper.ImBlockMapper;
import com.forum.sensitive.service.SensitiveWordService;
import com.forum.user.dto.UpdateProfileRequest;
import com.forum.user.dto.UserProfileVO;
import com.forum.user.entity.User;
import com.forum.user.entity.UserStatus;
import com.forum.user.mapper.UserMapper;
import com.forum.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Profile and reputation management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /**
     * Points needed to reach each level, indexed by level minus one.
     *
 * <p>Level is derived from points so the two cannot drift apart.
     */
    private static final int[] LEVEL_THRESHOLDS = {0, 100, 500, 2000, 10000, 50000};

    private final UserMapper userMapper;
    private final ImBlockMapper blockMapper;
    private final SensitiveWordService sensitiveWordService;

    @Override
    public UserProfileVO getProfile(Long userId) {
        User user = userMapper.selectPublicProfile(userId);
        if (user == null) {
            throw BusinessException.notFound("user not found");
        }
        return toVO(user);
    }

    @Override
    public UserProfileVO getCurrentProfile(Long currentUserId) {
        User user = userMapper.selectById(currentUserId);
        if (user == null) {
            throw BusinessException.notFound("user not found");
        }
        return toVO(user);
    }

    @Override
    @Transactional
    public UserProfileVO updateProfile(Long currentUserId, UpdateProfileRequest request) {
        User user = userMapper.selectById(currentUserId);
        if (user == null) {
            throw BusinessException.notFound("user not found");
        }
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            user.setNickname(sensitiveWordService.mask(request.getNickname().trim()));
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }
        if (request.getBio() != null) {
            user.setBio(sensitiveWordService.mask(request.getBio().trim()));
        }
        userMapper.updateById(user);
        return toVO(user);
    }

    @Override
    @Transactional
    public void addPoints(Long userId, int points) {
        // GREATEST in the mapper keeps the counter at zero when a negative
        // delta outruns it.
        userMapper.adjustPoints(userId, points);
        Integer total = userMapper.selectPoints(userId);
        if (total == null) {
            return;
        }
        userMapper.updateLevel(userId, levelFor(total));
    }

    @Override
    @Transactional
    public void changeStatus(Long operatorId, Long targetUserId, int status, String reason) {
        UserStatus target = UserStatus.of(status);
        User user = userMapper.selectById(targetUserId);
        if (user == null) {
            throw BusinessException.notFound("user not found");
        }
        if (user.getId().equals(operatorId)) {
            throw BusinessException.badRequest("an administrator cannot moderate their own account");
        }
        user.setStatus(target.getCode());
        userMapper.updateById(user);
        log.info("user {} status changed to {} by {}", targetUserId, target, operatorId);
    }

    @Override
    public List<UserProfileVO> listBlockedUsers(Long currentUserId) {
        List<Long> blockedIds = blockMapper.selectBlockedUserIds(currentUserId);
        if (blockedIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<UserProfileVO> result = new ArrayList<>(blockedIds.size());
        for (User user : userMapper.selectBatchIds(blockedIds)) {
            result.add(toVO(user));
        }
        return result;
    }

    /** Level is a pure function of the point total. */
    static int levelFor(int points) {
        int level = 1;
        for (int i = 0; i < LEVEL_THRESHOLDS.length; i++) {
            if (points >= LEVEL_THRESHOLDS[i]) {
                level = i + 1;
            }
        }
        return level;
    }

    private static UserProfileVO toVO(User user) {
        return UserProfileVO.builder()
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
    }
}
