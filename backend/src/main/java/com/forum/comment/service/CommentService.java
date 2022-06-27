package com.forum.comment.service;

import com.forum.comment.dto.CommentVO;
import com.forum.comment.dto.CreateCommentRequest;
import com.forum.post.dto.LikeResult;

import java.util.List;

/**
 * Comments and threaded replies.
 */
public interface CommentService {

    /**
     * Comments of a post with their replies nested.
     *
     * @param viewerId caller, used to fill the per-viewer liked flag
     */
    List<CommentVO> listByPost(Long postId, Long viewerId);

    /** Posts a root comment or a reply, filling in the thread linkage. */
    CommentVO createComment(Long authorId, CreateCommentRequest request);

    /** Edits a comment; only its author may do so. */
    CommentVO updateComment(Long commentId, Long operatorId, String content);

    /** Soft deletes a comment; the author or staff may do so. */
    void deleteComment(Long commentId, Long operatorId);

    /** Toggles a like on a comment. */
    LikeResult toggleLike(Long commentId, Long userId);
}
