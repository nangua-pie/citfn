package com.forum.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.comment.entity.CommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Comment like access.
 */
@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {

    @Select("SELECT user_id FROM forum_comment_like WHERE comment_id = #{commentId} "
            + "ORDER BY create_time DESC LIMIT #{limit}")
    List<Long> selectRecentUserIds(@Param("commentId") Long commentId, @Param("limit") Integer limit);
}
