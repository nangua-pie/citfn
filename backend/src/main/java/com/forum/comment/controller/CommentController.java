package com.forum.comment.controller;

import com.forum.comment.dto.CommentVO;
import com.forum.comment.dto.CreateCommentRequest;
import com.forum.comment.service.CommentService;
import com.forum.common.api.ApiResponse;
import com.forum.common.security.CurrentUser;
import com.forum.post.dto.LikeResult;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * Comment and reply API. Reading is public; writing needs a signed in user.
 */
@Validated
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/post/{postId}")
    public ApiResponse<List<CommentVO>> listByPost(@PathVariable Long postId) {
        return ApiResponse.success(
                commentService.listByPost(postId, CurrentUser.findUserId().orElse(null)));
    }

    @PostMapping
    public ApiResponse<CommentVO> create(@Valid @RequestBody CreateCommentRequest request) {
        return ApiResponse.success(commentService.createComment(CurrentUser.requireUserId(), request));
    }

    @PutMapping("/{commentId}")
    public ApiResponse<CommentVO> update(@PathVariable Long commentId,
                                          @Valid @RequestBody UpdateCommentRequest request) {
        return ApiResponse.success(commentService.updateComment(
                commentId, CurrentUser.requireUserId(), request.getContent()));
    }

    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> delete(@PathVariable Long commentId) {
        commentService.deleteComment(commentId, CurrentUser.requireUserId());
        return ApiResponse.success();
    }

    @PostMapping("/{commentId}/like")
    public ApiResponse<LikeResult> toggleLike(@PathVariable Long commentId) {
        return ApiResponse.success(commentService.toggleLike(commentId, CurrentUser.requireUserId()));
    }

    /** Edit payload for a comment. */
    @Data
    public static class UpdateCommentRequest {

        @NotBlank(message = "is required")
        @Size(max = 2000, message = "must not exceed 2000 characters")
        private String content;
    }
}
