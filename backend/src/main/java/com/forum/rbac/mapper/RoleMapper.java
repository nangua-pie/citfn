package com.forum.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.rbac.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Role table access.
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("SELECT * FROM sys_role WHERE code = #{code}")
    Role selectByCode(@Param("code") String code);

    @Select("SELECT r.* FROM sys_role r JOIN sys_user_role ur ON ur.role_id = r.id WHERE ur.user_id = #{userId}")
    List<Role> selectByUserId(@Param("userId") Long userId);
}
