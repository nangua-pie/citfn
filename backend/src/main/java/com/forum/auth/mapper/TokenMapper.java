package com.forum.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.auth.entity.TokenRecord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * Issued token records.
 */
@Mapper
public interface TokenMapper extends BaseMapper<TokenRecord> {

    @Select("SELECT * FROM sys_token WHERE token_hash = #{tokenHash} AND revoked = 0")
    TokenRecord selectActiveByHash(@Param("tokenHash") String tokenHash);

    @Update("UPDATE sys_token SET revoked = 1, revoke_time = #{revokeTime} "
            + "WHERE token_hash = #{tokenHash} AND revoked = 0")
    int revokeByHash(@Param("tokenHash") String tokenHash,
                     @Param("revokeTime") LocalDateTime revokeTime);

    /** Revokes every live token of a user, used by the logout-all action. */
    @Update("UPDATE sys_token SET revoked = 1, revoke_time = #{revokeTime} "
            + "WHERE user_id = #{userId} AND revoked = 0")
    int revokeAllForUser(@Param("userId") Long userId,
                         @Param("revokeTime") LocalDateTime revokeTime);

    @Delete("DELETE FROM sys_token WHERE expires_at < #{before}")
    int deleteExpired(@Param("before") LocalDateTime before);
}
