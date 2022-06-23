package com.forum.post.controller;

import com.forum.common.api.ApiResponse;
import com.forum.common.api.PageResult;
import com.forum.common.security.CurrentUser;
import com.forum.post.dto.CreatePostRequest;
import com.forum.post.dto.LikeResult;
import com.forum.post.dto.PostQuery;
import com.forum.post.dto.PostVO;
import com.forum.post.dto.UpdatePostRequest;
import com.forum.post.service.PostService;
import com.forum.rbac.entity.PermissionCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * Post REST API.
 *
 * <p>Public reads are permitted by SecurityConfig; writes need an authenticated
 * user, and moderation actions additionally require the matching permission
 * through {@code @PreAuthorize}.
 */
@Validated
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * Paged list.
     *
     * @param includeUnpublished honoured only for staff holding post:audit
     */
    @GetMapping
    public ApiResponse<PageResult<PostVO>> list(
            PostQuery query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "false") Boolean onlyEssential,
            @RequestParam(defaultValue = "false") boolean includeUnpublished,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size) {
        query.setCategoryId(categoryId);
        query.setUserId(userId);
        query.setKeyword(keyword);
        query.setTagName(tag);
        query.setOnlyEssential(Boolean.TRUE.equals(onlyEssential));
        return ApiResponse.success(postService.listPosts(
                query, CurrentUser.findUserId().orElse(null), includeUnpublished, page, size));
    }

    @GetMapping("/{postId}")
    public ApiResponse<PostVO> detail(@PathVariable Long postId) {
        return ApiResponse.success(postService.getPost(postId, CurrentUser.findUserId().orElse(null)));
    }

    @PostMapping
    public ApiResponse<PostVO> create(@Valid @RequestBody CreatePostRequest request) {
        return ApiResponse.success(postService.createPost(CurrentUser.requireUserId(), request));
    }

    @PutMapping("/{postId}")
    public ApiResponse<PostVO> update(@PathVariable Long postId,
                                      @Valid @RequestBody UpdatePostRequest request) {
        return ApiResponse.success(
                postService.updatePost(postId, CurrentUser.requireUserId(), request));
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<Void> delete(@PathVariable Long postId) {
        postService.deletePost(postId, CurrentUser.requireUserId());
        return ApiResponse.success();
    }

    @PostMapping("/{postId}/like")
    public ApiResponse<LikeResult> toggleLike(@PathVariable Long postId) {
        return ApiResponse.success(postService.toggleLike(postId, CurrentUser.requireUserId()));
    }

    @PostMapping("/{postId}/collect")
    public ApiResponse<LikeResult> toggleCollect(@PathVariable Long postId) {
        return ApiResponse.success(postService.toggleCollect(postId, CurrentUser.requireUserId()));
    }

    @GetMapping("/mine/collected")
    public ApiResponse<PageResult<PostVO>> myCollections(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size) {
        return ApiResponse.success(
                postService.listMyCollections(CurrentUser.requireUserId(), page, size));
    }

    @PatchMapping("/{postId}/top")
    @PreAuthorize("hasAuthority('" + PermissionCodes.POST_TOP + "')")
    public ApiResponse<Void> setTop(@PathVariable Long postId, @RequestParam boolean top) {
        postService.setTop(postId, CurrentUser.requireUserId(), top);
        return ApiResponse.success();
    }

    @PatchMapping("/{postId}/essential")
    @PreAuthorize("hasAuthority('" + PermissionCodes.POST_ESSENTIAL + "')")
    public ApiResponse<Void> setEssential(@PathVariable Long postId, @RequestParam boolean essential) {
        postService.setEssential(postId, CurrentUser.requireUserId(), essential);
        return ApiResponse.success();
    }

    @PatchMapping("/{postId}/audit")
    @PreAuthorize("hasAuthority('" + PermissionCodes.POST_AUDIT + "')")
    public ApiResponse<Void> audit(@PathVariable Long postId,
                                   @RequestParam boolean approved,
                                   @RequestParam(required = false) String reason) {
        postService.audit(postId, CurrentUser.requireUserId(), approved, reason);
        return ApiResponse.success();
    }

    @GetMapping("/tags/popular")
    public ApiResponse<List<Map<String, Object>>> popularTags(
            @RequestParam(defaultValue = "20") Integer limit) {
        return ApiResponse.success(postService.popularTags(limit));
    }
}
