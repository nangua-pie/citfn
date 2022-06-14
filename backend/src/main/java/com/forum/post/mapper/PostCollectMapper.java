package com.forum.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.post.entity.PostCollect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Post bookmark access.
 */
@Mapper
public interface PostCollectMapper extends BaseMapper<PostCollect> {

    /** Newest bookmarks of one user, for the "my collections" page. */
    @Select("SELECT post_id FROM forum_post_collect WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Long> selectPostIdsByUser(@Param("userId") Long userId);
}
