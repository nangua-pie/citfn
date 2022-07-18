package com.forum.rbac.service;

import com.forum.rbac.dto.RolePermissionVO;
import com.forum.rbac.dto.RoleVO;

import java.util.List;

/**
 * Role and permission administration.
 */
public interface RbacService {

    /** Every role with the permission codes it grants. */
    List<RoleVO> listRoles();

    /** Every permission, grouped for the admin console. */
    List<RolePermissionVO> listPermissions();

    /** Authority strings held by a user, used when minting a JWT. */
    List<String> authoritiesOf(Long userId);

    /** Replaces the permissions granted to a role. Requires {@code sensitive:manage}. */
    void grantPermissions(Long roleId, List<Long> permissionIds);

    /**
     * Grants a role to a user.
     *
     * <p>Requires {@code user:role:assign}; an administrator cannot grant a role
     * they do not hold themselves, which stops privilege escalation by mistake.
     */
    void assignRole(Long targetUserId, Long roleId);

    /** Revokes a role from a user. */
    void revokeRole(Long targetUserId, Long roleId);
}
