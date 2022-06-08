package com.forum.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.forum.post.entity.Post;
import com.forum.post.dto.PostQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Post table access.
 */
@Mapper
public interface PostMapper extends BaseMapper<Post> {

    /**
     * Paged post list.
     *
     * <p>Pinned posts sort above everything else, then by newest first.
     * {@code PostQuery} supplies the optional category, author, keyword,
     * essential and essential-flag filters, and decides whether posts awaiting
     * audit are visible.
     */
    @Select("<script>"
            + "SELECT * FROM forum_post WHERE is_deleted = 0 "
            + "<if test='query.categoryId != null'> AND category_id = #{query.categoryId} </if>"
            + "<if test='query.userId != null'> AND user_id = #{query.userId} </if>"
            + "<if test='query.keyword != null and query.keyword != \"\"'>"
            + " AND (title LIKE CONCAT('%', #{query.keyword}, '%') "
            + "      OR summary LIKE CONCAT('%', #{query.keyword}, '%')) </if>"
            + "<if test='query.onlyEssential'> AND is_essential = 1 </if>"
            + "<choose>"
            + "  <when test='query.includeAllStatuses'> </when>"
            + "  <otherwise> AND status = #{query.visibleStatus} </otherwise>"
            + "</choose>"
            + "<if test='query.tagName != null and query.tagName != \"\"'>"
            + " AND id IN (SELECT post_id FROM forum_post_tag WHERE tag_name = #{query.tagName}) </if>"
            + "ORDER BY is_top DESC, create_time DESC, id DESC"
            + "</script>")
    IPage<Post> selectPostPage(IPage<Post> page, @Param("query") PostQuery query);

    /** Post plus the body, used by the detail endpoint. */
    @Select("SELECT p.*, c.content_md FROM forum_post p "
            + "LEFT JOIN forum_post_content c ON c.post_id = p.id "
            + "WHERE p.id = #{postId} AND p.is_deleted = 0")
    Post selectPostWithContent(@Param("postId") Long postId);

    @Update("UPDATE forum_post SET view_count = view_count + 1 WHERE id = #{postId} AND is_deleted = 0")
    int incrementViewCount(@Param("postId") Long postId);

    /**
     * Adjusts a denormalised counter without letting it go negative.
     *
     * <p>Each counter has its own statement rather than a dynamic column name,
     * so no user supplied value can ever reach the SQL text.
     */
    @Update("UPDATE forum_post SET comment_count = GREATEST(comment_count + #{delta}, 0) "
            + "WHERE id = #{postId} AND is_deleted = 0")
    int adjustCommentCount(@Param("postId") Long postId, @Param("delta") Integer delta);

    @Update("UPDATE forum_post SET like_count = GREATEST(like_count + #{delta}, 0) "
            + "WHERE id = #{postId} AND is_deleted = 0")
    int adjustLikeCount(@Param("postId") Long postId, @Param("delta") Integer delta);

    @Update("UPDATE forum_post SET collect_count = GREATEST(collect_count + #{delta}, 0) "
            + "WHERE id = #{postId} AND is_deleted = 0")
    int adjustCollectCount(@Param("postId") Long postId, @Param("delta") Integer delta);

    @Update("UPDATE forum_post SET report_count = GREATEST(report_count + #{delta}, 0) "
            + "WHERE id = #{postId} AND is_deleted = 0")
    int adjustReportCount(@Param("postId") Long postId, @Param("delta") Integer delta);

    @Select("SELECT id FROM forum_post_like WHERE post_id = #{postId} AND user_id = #{userId} LIMIT 1")
    Long selectLikeId(@Param("postId") Long postId, @Param("userId") Long userId);

    @Select("SELECT id FROM forum_post_collect WHERE post_id = #{postId} AND user_id = #{userId} LIMIT 1")
    Long selectCollectId(@Param("postId") Long postId, @Param("userId") Long userId);

    /** Tags of a post, ordered by insertion so the order shown is stable. */
    @Select("SELECT tag_name FROM forum_post_tag WHERE post_id = #{postId} ORDER BY id")
    List<String> selectTagNames(@Param("postId") Long postId);

    @Select("SELECT COUNT(1) FROM forum_post WHERE user_id = #{userId} "
            + "AND is_deleted = 0 AND status = 1")
    int countPublishedByUser(@Param("userId") Long userId);
}
