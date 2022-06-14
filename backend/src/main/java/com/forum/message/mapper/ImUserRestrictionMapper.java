package com.forum.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.message.entity.ImUserRestriction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Administrator imposed messaging bans.
 */
@Mapper
public interface ImUserRestrictionMapper extends BaseMapper<ImUserRestriction> {

    @Select("SELECT * FROM im_user_restriction WHERE user_id = #{userId}")
    ImUserRestriction selectByUserId(@Param("userId") Long userId);

    /** Every ban that is currently in force, expired ones excluded. */
    @Select("SELECT * FROM im_user_restriction WHERE forbid_private_message = 1 "
            + "AND (expires_at IS NULL OR expires_at > NOW())")
    java.util.List<ImUserRestriction> selectActiveRestrictions();
}
