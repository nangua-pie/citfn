package com.forum.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forum.category.entity.Category;
import com.forum.category.mapper.CategoryMapper;
import com.forum.common.api.PageResult;
import com.forum.common.exception.BusinessException;
import com.forum.common.security.CurrentUser;
import com.forum.common.util.Pagination;
import com.forum.post.dto.CreatePostRequest;
import com.forum.post.dto.LikeResult;
import com.forum.post.dto.PostQuery;
import com.forum.post.dto.PostVO;
import com.forum.post.dto.UpdatePostRequest;
import com.forum.post.entity.Post;
import com.forum.post.entity.PostCollect;
import com.forum.post.entity.PostContent;
import com.forum.post.entity.PostLike;
import com.forum.post.entity.PostStatus;
import com.forum.post.entity.PostTag;
import com.forum.post.mapper.PostCollectMapper;
import com.forum.post.mapper.PostContentMapper;
import com.forum.post.mapper.PostLikeMapper;
import com.forum.post.mapper.PostMapper;
import com.forum.post.mapper.PostTagMapper;
import com.forum.post.service.PostService;
import com.forum.rbac.entity.PermissionCodes;
import com.forum.sensitive.service.SensitiveWordService;
import com.forum.user.entity.User;
import com.forum.user.entity.UserStatus;
import com.forum.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Post lifecycle and interactions.
 *
 * <p>Ownership rules live here rather than in the controller so they cannot be
 * bypassed by adding a new endpoint later.
 */
@Slf4j
@Service
public class PostServiceImpl implements PostService {

    /** Summary length kept on the list projection. */
    private static final int SUMMARY_LENGTH = 200;

    private final PostMapper postMapper;
    private final PostContentMapper contentMapper;
    private final PostLikeMapper likeMapper;
    private final PostCollectMapper collectMapper;
    private final PostTagMapper tagMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final SensitiveWordService sensitiveWordService;

    public PostServiceImpl(PostMapper postMapper,
                           PostContentMapper contentMapper,
                           PostLikeMapper likeMapper,
                           PostCollectMapper collectMapper,
                           PostTagMapper tagMapper,
                           CategoryMapper categoryMapper,
                           UserMapper userMapper,
                           SensitiveWordService sensitiveWordService) {
        this.postMapper = postMapper;
        this.contentMapper = contentMapper;
        this.likeMapper = likeMapper;
        this.collectMapper = collectMapper;
        this.tagMapper = tagMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
        this.sensitiveWordService = sensitiveWordService;
    }

    @Override
    public PageResult<PostVO> listPosts(PostQuery query, Long viewerId, boolean includeUnpublished,
                                        Long page, Long size) {
        // Visibility is decided here; a client can never widen it. Requesting
        // unpublished posts only works for staff holding the audit permission.
        boolean auditor = CurrentUser.find()
                .map(user -> user.hasAuthority(PermissionCodes.POST_AUDIT))
                .orElse(false);

        IPage<Post> result = postMapper.selectPostPage(
                new Page<>(Pagination.normalizePage(page), Pagination.normalizeSize(size)),
                query, auditor && includeUnpublished, PostStatus.PUBLISHED.getCode());
        List<PostVO> records = enrich(result.getRecords(), viewerId, false);
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PostVO getPost(Long postId, Long viewerId) {
        Post post = postMapper.selectById(postId);
        if (post == null || Integer.valueOf(1).equals(post.getIsDeleted())) {
            throw BusinessException.notFound("post not found");
        }
        assertVisible(post, viewerId);
        PostContent content = contentMapper.selectByPostId(postId);
        postMapper.incrementViewCount(postId);

        PostVO vo = toVO(post);
        vo.setContentMd(content == null ? "" : content.getContentMd());
        vo.setTags(tagMapper.selectTagNames(postId));
        applyViewerState(vo, postId, viewerId);
        return vo;
    }

    @Override
    @Transactional
    public PostVO createPost(Long authorId, CreatePostRequest request) {
        User author = userMapper.selectById(authorId);
        if (author == null) {
            throw BusinessException.unauthorized("not logged in");
        }
        if (!UserStatus.of(author.getStatus()).canPost()) {
            throw BusinessException.forbidden("your account cannot publish posts");
        }
        Category category = categoryMapper.selectById(request.getCategoryId());
        if (category == null) {
            throw BusinessException.notFound("category not found");
        }

        String title = sensitiveWordService.mask(request.getTitle().trim());
        String content = sensitiveWordService.mask(request.getContent());

        Post post = new Post();
        post.setCategoryId(request.getCategoryId());
        post.setUserId(authorId);
        post.setTitle(title);
        post.setSummary(buildSummary(content));
        post.setCoverUrl(request.getCoverUrl());
        post.setViewCount(0);
        post.setCommentCount(0);
        post.setLikeCount(0);
        post.setCollectCount(0);
        post.setReportCount(0);
        post.setIsTop(0);
        post.setIsEssential(0);
        post.setStatus(PostStatus.PUBLISHED.getCode());
        post.setIsDeleted(0);
        postMapper.insert(post);

        PostContent body = new PostContent(post.getId(), content, LocalDateTime.now());
        contentMapper.insert(body);
        replaceTags(post.getId(), request.getTags());
        categoryMapper.refreshPostCount(post.getCategoryId());
        log.info("post {} published by user {}", post.getId(), authorId);

        PostVO vo = toVO(post);
        vo.setContentMd(content);
        vo.setTags(tagMapper.selectTagNames(post.getId()));
        return vo;
    }

    @Override
    @Transactional
    public PostVO updatePost(Long postId, Long operatorId, UpdatePostRequest request) {
        Post post = requirePost(postId);
        requireOwnerOrPermission(post, operatorId);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            post.setTitle(sensitiveWordService.mask(request.getTitle().trim()));
        }
        String content = sensitiveWordService.mask(request.getContent());
        post.setSummary(buildSummary(content));
        if (request.getCoverUrl() != null) {
            post.setCoverUrl(request.getCoverUrl());
        }
        postMapper.updateById(post);

        PostContent body = contentMapper.selectByPostId(postId);
        if (body == null) {
            contentMapper.insert(new PostContent(postId, content, LocalDateTime.now()));
        } else {
            body.setContentMd(content);
            body.setUpdateTime(LocalDateTime.now());
            contentMapper.updateById(body);
        }
        if (request.getTags() != null) {
            replaceTags(postId, request.getTags());
        }

        PostVO vo = toVO(post);
        vo.setContentMd(content);
        vo.setTags(tagMapper.selectTagNames(postId));
        return vo;
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long operatorId) {
        Post post = requirePost(postId);
        requireOwnerOrPermission(post, operatorId);
        post.setIsDeleted(1);
        post.setStatus(PostStatus.DELETED.getCode());
        postMapper.updateById(post);
        categoryMapper.refreshPostCount(post.getCategoryId());
        log.info("post {} deleted by user {}", postId, operatorId);
    }

    @Override
    @Transactional
    public LikeResult toggleLike(Long postId, Long userId) {
        requirePost(postId);
        Long existing = postMapper.selectLikeId(postId, userId);
        boolean active;
        if (existing == null) {
            likeMapper.insert(PostLike.of(postId, userId));
            postMapper.adjustLikeCount(postId, 1);
            active = true;
        } else {
            likeMapper.deleteById(existing);
            postMapper.adjustLikeCount(postId, -1);
            active = false;
        }
        Post updated = postMapper.selectById(postId);
        return new LikeResult(active, updated == null ? 0 : updated.getLikeCount());
    }

    @Override
    @Transactional
    public LikeResult toggleCollect(Long postId, Long userId) {
        requirePost(postId);
        Long existing = postMapper.selectCollectId(postId, userId);
        boolean active;
        if (existing == null) {
            collectMapper.insert(PostCollect.of(postId, userId));
            postMapper.adjustCollectCount(postId, 1);
            active = true;
        } else {
            collectMapper.deleteById(existing);
            postMapper.adjustCollectCount(postId, -1);
            active = false;
        }
        Post updated = postMapper.selectById(postId);
        return new LikeResult(active, updated == null ? 0 : updated.getCollectCount());
    }

    @Override
    public PageResult<PostVO> listMyCollections(Long userId, Long page, Long size) {
        List<Long> postIds = collectMapper.selectPostIdsByUser(userId);
        if (postIds.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, Pagination.normalizePage(page),
                    Pagination.normalizeSize(size));
        }
        Page<Post> result = postMapper.selectPage(
                new Page<>(Pagination.normalizePage(page), Pagination.normalizeSize(size)),
                new QueryWrapper<Post>().in("id", postIds).eq("is_deleted", 0));
        List<PostVO> records = enrich(result.getRecords(), userId, false);
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public void setTop(Long postId, Long operatorId, boolean top) {
        Post post = requirePost(postId);
        post.setIsTop(top ? 1 : 0);
        postMapper.updateById(post);
        log.info("post {} top flag set to {} by user {}", postId, top, operatorId);
    }

    @Override
    @Transactional
    public void setEssential(Long postId, Long operatorId, boolean essential) {
        Post post = requirePost(postId);
        post.setIsEssential(essential ? 1 : 0);
        postMapper.updateById(post);
        log.info("post {} essential flag set to {} by user {}", postId, essential, operatorId);
    }

    @Override
    @Transactional
    public void audit(Long postId, Long operatorId, boolean approved, String reason) {
        Post post = requirePost(postId);
        post.setStatus(approved ? PostStatus.PUBLISHED.getCode() : PostStatus.REJECTED.getCode());
        post.setAuditUserId(operatorId);
        post.setAuditTime(LocalDateTime.now());
        post.setAuditReason(reason);
        postMapper.updateById(post);
        categoryMapper.refreshPostCount(post.getCategoryId());
        log.info("post {} audited by user {} approved={}", postId, operatorId, approved);
    }

    @Override
    public List<Map<String, Object>> popularTags(int limit) {
        return tagMapper.selectPopularTags(limit <= 0 ? 20 : Math.min(limit, 50));
    }

    // --- helpers -------------------------------------------------------

    private Post requirePost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || Integer.valueOf(1).equals(post.getIsDeleted())) {
            throw BusinessException.notFound("post not found");
        }
        return post;
    }

    /** Drafts and rejected posts stay visible only to their author and staff. */
    private void assertVisible(Post post, Long viewerId) {
        if (PostStatus.of(post.getStatus()).isVisibleToPublic()) {
            return;
        }
        boolean owner = viewerId != null && viewerId.equals(post.getUserId());
        boolean staff = CurrentUser.find()
                .map(user -> user.hasAuthority(PermissionCodes.POST_AUDIT)
                        || user.hasAuthority(PermissionCodes.POST_EDIT_ANY))
                .orElse(false);
        if (!owner && !staff) {
            throw BusinessException.notFound("post not found");
        }
    }

    private void requireOwnerOrPermission(Post post, Long operatorId) {
        if (post.getUserId().equals(operatorId)) {
            return;
        }
        boolean allowed = CurrentUser.find()
                .map(user -> user.hasAuthority(PermissionCodes.POST_EDIT_ANY)
                        || user.hasAuthority(PermissionCodes.POST_DELETE_ANY))
                .orElse(false);
        if (!allowed) {
            throw BusinessException.forbidden("only the author can modify this post");
        }
    }

    private List<PostVO> enrich(List<Post> posts, Long viewerId, boolean includeContent) {
        if (posts.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, User> authors = loadAuthors(posts);
        Map<Long, String> categories = loadCategories(posts);
        List<PostVO> result = new ArrayList<>(posts.size());
        for (Post post : posts) {
            PostVO vo = toVO(post);
            User author = authors.get(post.getUserId());
            if (author != null) {
                vo.setAuthorNickname(author.getNickname());
                vo.setAuthorAvatarUrl(author.getAvatarUrl());
            }
            vo.setCategoryName(categories.get(post.getCategoryId()));
            vo.setTags(tagMapper.selectTagNames(post.getId()));
            applyViewerState(vo, post.getId(), viewerId);
            result.add(vo);
        }
        return result;
    }

    private Map<Long, User> loadAuthors(List<Post> posts) {
        Set<Long> ids = posts.stream().map(Post::getUserId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, User> map = new java.util.HashMap<>();
        for (User user : userMapper.selectBatchIds(ids)) {
            map.put(user.getId(), user);
        }
        return map;
    }

    private Map<Long, String> loadCategories(List<Post> posts) {
        Set<Long> ids = posts.stream().map(Post::getCategoryId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> map = new java.util.HashMap<>();
        for (Category category : categoryMapper.selectBatchIds(ids)) {
            map.put(category.getId(), category.getName());
        }
        return map;
    }

    private void applyViewerState(PostVO vo, Long postId, Long viewerId) {
        if (viewerId == null) {
            vo.setLiked(Boolean.FALSE);
            vo.setCollected(Boolean.FALSE);
            return;
        }
        vo.setLiked(postMapper.selectLikeId(postId, viewerId) != null);
        vo.setCollected(postMapper.selectCollectId(postId, viewerId) != null);
    }

    private PostVO toVO(Post post) {
        return PostVO.builder()
                .id(post.getId())
                .categoryId(post.getCategoryId())
                .userId(post.getUserId())
                .title(post.getTitle())
                .summary(post.getSummary())
                .coverUrl(post.getCoverUrl())
                .viewCount(post.getViewCount())
                .commentCount(post.getCommentCount())
                .likeCount(post.getLikeCount())
                .collectCount(post.getCollectCount())
                .reportCount(post.getReportCount())
                .isTop(post.getIsTop())
                .isEssential(post.getIsEssential())
                .status(post.getStatus())
                .createTime(post.getCreateTime())
                .updateTime(post.getUpdateTime())
                .build();
    }

    /** Replaces the tag set of a post. */
    private void replaceTags(Long postId, List<String> tags) {
        tagMapper.deleteByPostId(postId);
        if (tags == null || tags.isEmpty()) {
            return;
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String tag : tags) {
            if (tag == null) {
                continue;
            }
            String cleaned = tag.trim().toLowerCase(Locale.ROOT);
            if (!cleaned.isEmpty() && unique.size() < 5) {
                unique.add(sensitiveWordService.mask(cleaned));
            }
        }
        for (String tag : unique) {
            tagMapper.insert(PostTag.of(postId, tag));
        }
    }

    /** Plain-text excerpt of the markdown body for list views. */
    private String buildSummary(String markdown) {
        if (markdown == null) {
            return "";
        }
        String text = markdown
                .replaceAll("!\\[[^\\]]*]\\([^)]*\\)", " ")
                .replaceAll("\\[([^\\]]*)]\\([^)]*\\)", "$1")
                .replaceAll("(?m)^\\s{0,3}#{1,6}\\s*", "")
                .replaceAll("[>*_`~#-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return text.length() <= SUMMARY_LENGTH ? text : text.substring(0, SUMMARY_LENGTH);
    }
}
