package com.forum.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.post.entity.PostContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Post body access, kept apart from the list projection.
 */
@Mapper
public interface PostContentMapper extends BaseMapper<PostContent> {

    @Select("SELECT * FROM forum_post_content WHERE post_id = #{postId}")
    PostContent selectByPostId(@Param("postId") Long postId);
}
