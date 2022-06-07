package com.forum.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * User table access.
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * Public profile columns only; deliberately omits password hash, email and
     * login metadata so a caller cannot leak them by accident.
     */
    @Select("SELECT id, username, nickname, avatar_url, bio, level, points, create_time "
            + "FROM sys_user WHERE id = #{userId} AND is_deleted = 0")
    User selectPublicProfile(@Param("userId") Long userId);

    /** Authority strings granted to the user through role and permission rows. */
    @Select("SELECT DISTINCT p.code FROM sys_user_role ur "
            + "JOIN sys_role_permission rp ON rp.role_id = ur.role_id "
            + "JOIN sys_permission p ON p.id = rp.permission_id "
            + "WHERE ur.user_id = #{userId}")
    List<String> selectAuthorities(@Param("userId") Long userId);

    /** Role codes held by the user, used to build the JWT authority list. */
    @Select("SELECT r.code FROM sys_user_role ur JOIN sys_role r ON r.id = ur.role_id "
            + "WHERE ur.user_id = #{userId}")
    List<String> selectRoleCodes(@Param("userId") Long userId);

    @Select("SELECT COUNT(1) FROM sys_user WHERE username = #{username} AND is_deleted = 0")
    int countByUsername(@Param("username") String username);

    @Select("SELECT COUNT(1) FROM sys_user WHERE email = #{email} AND is_deleted = 0")
    int countByEmail(@Param("email") String email);

    /** Existence probe used before sending a private message to the receiver. */
    @Select("SELECT COUNT(1) FROM sys_user WHERE id = #{userId} AND is_deleted = 0")
    int existsById(@Param("userId") Long userId);
}
