package com.forum.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.rbac.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Role to permission assignment access.
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    @Select("SELECT * FROM sys_role_permission WHERE role_id = #{roleId}")
    List<RolePermission> selectByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT rp.* FROM sys_role_permission rp JOIN sys_user_role ur ON ur.role_id = rp.role_id "
            + "WHERE ur.user_id = #{userId}")
    List<RolePermission> selectByUserId(@Param("userId") Long userId);
}
