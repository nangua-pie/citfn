package com.forum.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.message.entity.ImBlock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Block list access.
 */
@Mapper
public interface ImBlockMapper extends BaseMapper<ImBlock> {

    /** True when {@code userId} refuses messages from {@code blockedUserId}. */
    @Select("SELECT COUNT(1) > 0 FROM im_block "
            + "WHERE user_id = #{userId} AND blocked_user_id = #{blockedUserId}")
    boolean existsBlock(@Param("userId") Long userId, @Param("blockedUserId") Long blockedUserId);

    @Select("SELECT blocked_user_id FROM im_block WHERE user_id = #{userId}")
    java.util.List<Long> selectBlockedUserIds(@Param("userId") Long userId);
}
