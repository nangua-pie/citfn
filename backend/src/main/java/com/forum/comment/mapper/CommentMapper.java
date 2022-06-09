package com.forum.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.comment.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Comment table access.
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * Root comments of a post, newest last so the thread renders in order.
     * Pinned moderation aside, ordering is strictly by id.
     */
    @Select("SELECT * FROM forum_comment WHERE post_id = #{postId} AND parent_id IS NULL "
            + "AND status = 1 AND is_deleted = 0 ORDER BY id")
    List<Comment> selectRootComments(@Param("postId") Long postId);

    /**
     * Every reply belonging to the given root comments, in one indexed query,
     * instead of a lookup per comment.
     */
    @Select("<script>"
            + "SELECT * FROM forum_comment WHERE status = 1 AND is_deleted = 0 AND root_id IN "
            + "<foreach collection='rootIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "ORDER BY root_id, id"
            + "</script>")
    List<Comment> selectRepliesByRootIds(@Param("rootIds") List<Long> rootIds);

    @Update("UPDATE forum_comment SET reply_count = GREATEST(reply_count + #{delta}, 0) "
            + "WHERE id = #{commentId} AND is_deleted = 0")
    int adjustReplyCount(@Param("commentId") Long commentId, @Param("delta") Integer delta);

    @Update("UPDATE forum_comment SET like_count = GREATEST(like_count + #{delta}, 0) "
            + "WHERE id = #{commentId} AND is_deleted = 0")
    int adjustLikeCount(@Param("commentId") Long commentId, @Param("delta") Integer delta);

    @Select("SELECT id FROM forum_comment_like WHERE comment_id = #{commentId} AND user_id = #{userId} LIMIT 1")
    Long selectLikeId(@Param("commentId") Long commentId, @Param("userId") Long userId);

    @Select("SELECT COUNT(1) FROM forum_comment WHERE post_id = #{postId} "
            + "AND status = 1 AND is_deleted = 0")
    int countVisibleByPost(@Param("postId") Long postId);
}
