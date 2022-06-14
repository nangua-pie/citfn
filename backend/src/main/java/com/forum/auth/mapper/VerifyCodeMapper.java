package com.forum.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.auth.entity.VerifyCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * Verification code access.
 */
@Mapper
public interface VerifyCodeMapper extends BaseMapper<VerifyCode> {

    /** Newest unused, unexpired code for a target and scene. */
    @Select("SELECT * FROM sys_verify_code WHERE target = #{target} AND scene = #{scene} "
            + "AND used = 0 AND expires_at > NOW() ORDER BY id DESC LIMIT 1")
    VerifyCode selectActive(@Param("target") String target, @Param("scene") String scene);

    /** Marks every outstanding code for the pair as spent. */
    @Update("UPDATE sys_verify_code SET used = 1 WHERE target = #{target} AND scene = #{scene} "
            + "AND used = 0")
    int consumeAll(@Param("target") String target, @Param("scene") String scene);

    @Select("SELECT COUNT(1) FROM sys_verify_code WHERE target = #{target} AND scene = #{scene} "
            + "AND create_time > #{since}")
    int countRecent(@Param("target") String target,
                    @Param("scene") String scene,
                    @Param("since") LocalDateTime since);
}
