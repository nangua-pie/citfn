package com.forum.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.rbac.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * User to role assignment access.
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    @Select("SELECT * FROM sys_user_role WHERE user_id = #{userId}")
    List<UserRole> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId} AND role_id = #{roleId}")
    Long selectRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
