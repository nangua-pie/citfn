package com.forum.user.controller;

import com.forum.common.api.ApiResponse;
import com.forum.common.security.CurrentUser;
import com.forum.rbac.entity.PermissionCodes;
import com.forum.user.dto.UpdateProfileRequest;
import com.forum.user.dto.UserProfileVO;
import com.forum.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * User profile API. Profiles are readable by anyone; everything else requires a
 * signed in user, and moderation additionally requires a permission.
 */
@Validated
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}/profile")
    public ApiResponse<UserProfileVO> profile(@PathVariable Long userId) {
        return ApiResponse.success(userService.getProfile(userId));
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileVO> me() {
        return ApiResponse.success(userService.getCurrentProfile(CurrentUser.requireUserId()));
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileVO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(CurrentUser.requireUserId(), request));
    }

    @GetMapping("/me/blocked")
    public ApiResponse<List<UserProfileVO>> blocked() {
        return ApiResponse.success(userService.listBlockedUsers(CurrentUser.requireUserId()));
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_BAN + "')")
    public ApiResponse<Void> changeStatus(@PathVariable Long userId,
                                          @RequestParam int status,
                                          @RequestParam(required = false) String reason) {
        userService.changeStatus(CurrentUser.requireUserId(), userId, status, reason);
        return ApiResponse.success();
    }
}
