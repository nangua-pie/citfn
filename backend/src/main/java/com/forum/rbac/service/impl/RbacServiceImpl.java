package com.forum.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.forum.common.exception.BusinessException;
import com.forum.rbac.dto.RolePermissionVO;
import com.forum.rbac.dto.RoleVO;
import com.forum.rbac.entity.Permission;
import com.forum.rbac.entity.Role;
import com.forum.rbac.entity.RoleCodes;
import com.forum.rbac.entity.RolePermission;
import com.forum.rbac.entity.UserRole;
import com.forum.rbac.mapper.PermissionMapper;
import com.forum.rbac.mapper.RoleMapper;
import com.forum.rbac.mapper.RolePermissionMapper;
import com.forum.rbac.mapper.UserRoleMapper;
import com.forum.rbac.service.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Role and permission administration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RbacServiceImpl implements RbacService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    public List<RoleVO> listRoles() {
        List<Role> roles = roleMapper.selectList(new QueryWrapper<Role>().orderByAsc("code"));
        Map<Long, List<RolePermission>> byRole = new LinkedHashMap<>();
        for (RolePermission link : rolePermissionMapper.selectList(null)) {
            byRole.computeIfAbsent(link.getRoleId(), key -> new ArrayList<>()).add(link);
        }

        // One lookup for every permission, then group in memory, so listing
        // roles does not fan out into a query per role.
        Map<Long, Permission> permissions = permissionMapper.selectList(null).stream()
                .collect(Collectors.toMap(Permission::getId, java.util.function.Function.identity(),
                        (a, b) -> a, LinkedHashMap::new));

        List<RoleVO> result = new ArrayList<>(roles.size());
        for (Role role : roles) {
            List<String> codes = new ArrayList<>();
            for (RolePermission link : byRole.getOrDefault(role.getId(), List.of())) {
                Permission permission = permissions.get(link.getPermissionId());
                if (permission != null) {
                    codes.add(permission.getCode());
                }
            }
            result.add(RoleVO.builder()
                    .id(role.getId())
                    .code(role.getCode())
                    .name(role.getName())
                    .description(role.getDescription())
                    .permissions(codes)
                    .build());
        }
        return result;
    }

    @Override
    public List<RolePermissionVO> listPermissions() {
        return permissionMapper.selectAll().stream()
                .map(permission -> RolePermissionVO.builder()
                        .id(permission.getId())
                        .code(permission.getCode())
                        .name(permission.getName())
                        .module(permission.getModule())
                        .description(permission.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> authoritiesOf(Long userId) {
        List<String> authorities = new ArrayList<>();
        for (Role role : roleMapper.selectByUserId(userId)) {
            authorities.add(role.getCode());
        }
        return authorities;
    }

    @Override
    @Transactional
    public void grantPermissions(Long roleId, List<Long> permissionIds) {
        if (roleMapper.selectById(roleId) == null) {
            throw BusinessException.notFound("role not found");
        }
        rolePermissionMapper.delete(new QueryWrapper<RolePermission>().eq("role_id", roleId));
        Set<Long> unique = new LinkedHashSet<>(permissionIds == null ? List.of() : permissionIds);
        for (Long permissionId : unique) {
            if (permissionMapper.selectById(permissionId) == null) {
                throw BusinessException.badRequest("unknown permission: " + permissionId);
            }
            rolePermissionMapper.insert(RolePermission.of(roleId, permissionId));
        }
        log.info("role {} now grants {} permission(s)", roleId, unique.size());
    }

    @Override
    @Transactional
    public void assignRole(Long targetUserId, Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw BusinessException.notFound("role not found");
        }
        if (userRoleMapper.selectRoleId(targetUserId, roleId) != null) {
            return;
        }
        userRoleMapper.insert(UserRole.of(targetUserId, roleId));
        log.info("role {} assigned to user {}", role.getCode(), targetUserId);
    }

    @Override
    @Transactional
    public void revokeRole(Long targetUserId, Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw BusinessException.notFound("role not found");
        }
        if (RoleCodes.ADMIN.equals(role.getCode())) {
            // Removing the last administrator would lock everyone out of the
            // admin console, so the built-in admin role is never revoked.
            Long admins = userRoleMapper.selectCount(new QueryWrapper<UserRole>().eq("role_id", roleId));
            if (admins != null && admins <= 1) {
                throw BusinessException.conflict("the last administrator cannot lose the admin role");
            }
        }
        userRoleMapper.delete(new QueryWrapper<UserRole>()
                .eq("user_id", targetUserId)
                .eq("role_id", roleId));
        log.info("role {} revoked from user {}", role.getCode(), targetUserId);
    }
}
