package com.forum.user.service;

import com.forum.user.dto.UpdateProfileRequest;
import com.forum.user.dto.UserProfileVO;

import java.util.List;

/**
 * User profiles, avatars, levels and points.
 */
public interface UserService {

    /** Public profile of any user. */
    UserProfileVO getProfile(Long userId);

    /** Profile of the signed in user, including account status. */
    UserProfileVO getCurrentProfile(Long currentUserId);

    /** Updates the signed in user's nickname, avatar and biography. */
    UserProfileVO updateProfile(Long currentUserId, UpdateProfileRequest request);

    /**
     * Awards points, for example for publishing a post or receiving a like.
     *
     * <p>Level is derived from the accumulated points rather than stored
     * separately, so the two can never disagree.
     */
    void addPoints(Long userId, int points);

    /** Administrator imposed status change: mute or ban an account. */
    void changeStatus(Long operatorId, Long targetUserId, int status, String reason);

    /** Users the caller has blocked, as profile summaries. */
    List<UserProfileVO> listBlockedUsers(Long currentUserId);
}
