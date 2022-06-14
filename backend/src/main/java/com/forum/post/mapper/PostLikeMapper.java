package com.forum.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.post.entity.PostLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Post like access.
 */
@Mapper
public interface PostLikeMapper extends BaseMapper<PostLike> {

    /** Newest likes, used for the "liked by" strip and for awarding points. */
    @Select("SELECT user_id FROM forum_post_like WHERE post_id = #{postId} ORDER BY create_time DESC LIMIT #{limit}")
    List<Long> selectRecentUserIds(@Param("postId") Long postId, @Param("limit") Integer limit);
}
