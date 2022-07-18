package com.forum.rbac.controller;

import com.forum.common.api.ApiResponse;
import com.forum.rbac.dto.RolePermissionVO;
import com.forum.rbac.dto.RoleVO;
import com.forum.rbac.entity.PermissionCodes;
import com.forum.rbac.service.RbacService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * Role and permission administration.
 *
 * <p>Reading the matrix requires {@code log:view}; every mutation requires
 * {@code user:role:assign} or {@code sensitive:manage}.
 */
@Validated
@RestController
@RequestMapping("/api/admin/rbac")
@RequiredArgsConstructor
public class RbacController {

    private final RbacService rbacService;

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('" + PermissionCodes.OPERATION_LOG_VIEW + "')")
    public ApiResponse<List<RoleVO>> listRoles() {
        return ApiResponse.success(rbacService.listRoles());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('" + PermissionCodes.OPERATION_LOG_VIEW + "')")
    public ApiResponse<List<RolePermissionVO>> listPermissions() {
        return ApiResponse.success(rbacService.listPermissions());
    }

    @PutMapping("/roles/{roleId}/permissions")
    @PreAuthorize("hasAuthority('" + PermissionCodes.SENSITIVE_MANAGE + "')")
    public ApiResponse<Void> grantPermissions(@PathVariable Long roleId,
                                              @Valid @RequestBody GrantPermissionsRequest request) {
        rbacService.grantPermissions(roleId, request.getPermissionIds());
        return ApiResponse.success();
    }

    @PostMapping("/users/{userId}/roles/{roleId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_ROLE_ASSIGN + "')")
    public ApiResponse<Void> assignRole(@PathVariable Long userId, @PathVariable Long roleId) {
        rbacService.assignRole(userId, roleId);
        return ApiResponse.success();
    }

    @DeleteMapping("/users/{userId}/roles/{roleId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.USER_ROLE_ASSIGN + "')")
    public ApiResponse<Void> revokeRole(@PathVariable Long userId, @PathVariable Long roleId) {
        rbacService.revokeRole(userId, roleId);
        return ApiResponse.success();
    }

    /** Request body for replacing a role's permission set. */
    @Data
    public static class GrantPermissionsRequest {

        private List<Long> permissionIds;
    }
}
