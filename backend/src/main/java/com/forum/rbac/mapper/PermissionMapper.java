package com.forum.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.rbac.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Permission table access.
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    @Select("SELECT * FROM sys_permission WHERE code = #{code}")
    Permission selectByCode(@Param("code") String code);

    @Select("SELECT * FROM sys_permission ORDER BY module, code")
    List<Permission> selectAll();
}
