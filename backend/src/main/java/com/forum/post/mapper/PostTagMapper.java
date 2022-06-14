package com.forum.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.post.entity.PostTag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Post tag access.
 */
@Mapper
public interface PostTagMapper extends BaseMapper<PostTag> {

    /** Replaces the tag set of a post; the caller deletes then inserts. */
    @Delete("DELETE FROM forum_post_tag WHERE post_id = #{postId}")
    int deleteByPostId(@Param("postId") Long postId);

    @Select("SELECT tag_name, COUNT(1) AS total FROM forum_post_tag "
            + "JOIN forum_post p ON p.id = forum_post_tag.post_id "
            + "AND p.is_deleted = 0 AND p.status = 1 "
            + "GROUP BY tag_name ORDER BY total DESC, tag_name LIMIT #{limit}")
    List<java.util.Map<String, Object>> selectPopularTags(@Param("limit") Integer limit);
}
