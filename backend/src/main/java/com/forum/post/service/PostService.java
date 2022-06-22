package com.forum.post.service;

import com.forum.common.api.PageResult;
import com.forum.post.dto.CreatePostRequest;
import com.forum.post.dto.PostQuery;
import com.forum.post.dto.PostVO;
import com.forum.post.dto.UpdatePostRequest;

import java.util.List;
import java.util.Map;

/**
 * Post lifecycle, search and interactions.
 */
public interface PostService {

    /**
     * Paged, filtered list.
     *
     * <p>Only published posts are visible unless the caller holds
     * {@code post:audit}, which the service checks rather than trusting a
     * request parameter.
     */
    PageResult<PostVO> listPosts(PostQuery query, Long viewerId, boolean includeUnpublished,
                                 Long page, Long size);

    /** Post detail with its markdown body; increments the view counter. */
    PostVO getPost(Long postId, Long viewerId);

    /** Publishes a post authored by the current user. */
    PostVO createPost(Long authorId, CreatePostRequest request);

    /** Edits a post; only the author may do so. */
    PostVO updatePost(Long postId, Long operatorId, UpdatePostRequest request);

    /** Soft deletes a post; only the author or staff may do so. */
    void deletePost(Long postId, Long operatorId);

    /** Toggles a like and returns the resulting state and counter. */
    LikeResult toggleLike(Long postId, Long userId);

    /** Toggles a bookmark and returns the resulting state and counter. */
    LikeResult toggleCollect(Long postId, Long userId);

    /** Posts the current user has bookmarked, newest first. */
    PageResult<PostVO> listMyCollections(Long userId, Long page, Long size);

    /** Pins or unpins a post. Requires {@code post:top}. */
    void setTop(Long postId, Long operatorId, boolean top);

    /** Marks or unmarks a post as essential. Requires {@code post:essential}. */
    void setEssential(Long postId, Long operatorId, boolean essential);

    /** Administrator audit decision. */
    void audit(Long postId, Long operatorId, boolean approved, String reason);

    /** Popular tags with usage counts, for the tag cloud. */
    List<Map<String, Object>> popularTags(int limit);
}
