package com.forum.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.forum.category.entity.Category;
import com.forum.category.mapper.CategoryMapper;
import com.forum.common.exception.BusinessException;
import com.forum.post.dto.CreatePostRequest;
import com.forum.post.dto.PostQuery;
import com.forum.post.entity.Post;
import com.forum.post.entity.PostCollect;
import com.forum.post.entity.PostContent;
import com.forum.post.entity.PostLike;
import com.forum.post.entity.PostStatus;
import com.forum.post.mapper.PostCollectMapper;
import com.forum.post.mapper.PostContentMapper;
import com.forum.post.mapper.PostLikeMapper;
import com.forum.post.mapper.PostMapper;
import com.forum.post.mapper.PostTagMapper;
import com.forum.sensitive.service.SensitiveWordService;
import com.forum.user.entity.User;
import com.forum.user.entity.UserStatus;
import com.forum.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Post create, read, update and delete tests, including the ownership rules.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostServiceImplTest {

    private static final Long AUTHOR_ID = 5L;
    private static final Long CATEGORY_ID = 3L;

    @Mock
    private PostMapper postMapper;
    @Mock
    private PostContentMapper contentMapper;
    @Mock
    private PostLikeMapper likeMapper;
    @Mock
    private PostCollectMapper collectMapper;
    @Mock
    private PostTagMapper tagMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private SensitiveWordService sensitiveWordService;

    private PostServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PostServiceImpl(postMapper, contentMapper, likeMapper, collectMapper,
                tagMapper, categoryMapper, userMapper, sensitiveWordService);
        when(sensitiveWordService.mask(anyString())).thenAnswer(call -> call.getArgument(0));
        when(tagMapper.selectTagNames(anyLong())).thenReturn(Collections.emptyList());
        when(userMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());
        when(categoryMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("create stores the body separately from the list projection")
    void createSplitsBodyFromListRow() {
        when(userMapper.selectById(AUTHOR_ID)).thenReturn(activeUser());
        when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(category());
        when(postMapper.insert(any(Post.class))).thenAnswer(call -> {
            ((Post) call.getArgument(0)).setId(42L);
            return 1;
        });

        var result = service.createPost(AUTHOR_ID, createRequest());

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postMapper).insert(postCaptor.capture());
        // Counters start at zero and the row is published immediately.
        assertThat(postCaptor.getValue().getViewCount()).isZero();
        assertThat(postCaptor.getValue().getLikeCount()).isZero();
        assertThat(postCaptor.getValue().getStatus()).isEqualTo(PostStatus.PUBLISHED.getCode());

        ArgumentCaptor<PostContent> contentCaptor = ArgumentCaptor.forClass(PostContent.class);
        verify(contentMapper).insert(contentCaptor.capture());
        assertThat(contentCaptor.getValue().getPostId()).isEqualTo(42L);
        assertThat(contentCaptor.getValue().getContentMd()).isEqualTo("the body");
        assertThat(result.getId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("create derives a summary from the markdown body")
    void createBuildsSummary() {
        when(userMapper.selectById(AUTHOR_ID)).thenReturn(activeUser());
        when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(category());
        when(postMapper.insert(any(Post.class))).thenAnswer(call -> {
            ((Post) call.getArgument(0)).setId(1L);
            return 1;
        });

        CreatePostRequest request = createRequest();
        request.setContent("# Heading\n\nSome **bold** text with a [link](https://example.com).");

        service.createPost(AUTHOR_ID, request);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postMapper).insert(captor.capture());
        String summary = captor.getValue().getSummary();
        assertThat(summary).contains("Heading").contains("bold").doesNotContain("**");
    }

    @Test
    @DisplayName("create rejects an unknown category")
    void createRejectsUnknownCategory() {
        when(userMapper.selectById(AUTHOR_ID)).thenReturn(activeUser());
        when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.createPost(AUTHOR_ID, createRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40400);

        verify(postMapper, never()).insert(any(Post.class));
    }

    @Test
    @DisplayName("create rejects a muted account")
    void createRejectsMutedAuthor() {
        User author = activeUser();
        author.setStatus(UserStatus.MUTED.getCode());
        when(userMapper.selectById(AUTHOR_ID)).thenReturn(author);

        assertThatThrownBy(() -> service.createPost(AUTHOR_ID, createRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40300);

        verify(postMapper, never()).insert(any(Post.class));
    }

    @Test
    @DisplayName("read returns the body and bumps the view counter")
    void readReturnsBodyAndCountsView() {
        Post post = post();
        when(postMapper.selectById(42L)).thenReturn(post);
        PostContent content = new PostContent(42L, "the body", null);
        when(contentMapper.selectByPostId(42L)).thenReturn(content);

        var result = service.getPost(42L, null);

        assertThat(result.getContentMd()).isEqualTo("the body");
        verify(postMapper).incrementViewCount(42L);
    }

    @Test
    @DisplayName("read hides an unpublished post from everyone but its author")
    void readHidesUnpublishedFromStrangers() {
        Post post = post();
        post.setStatus(PostStatus.PENDING_AUDIT.getCode());
        when(postMapper.selectById(42L)).thenReturn(post);

        assertThatThrownBy(() -> service.getPost(42L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40400);

        verify(postMapper, never()).incrementViewCount(anyLong());
    }

    @Test
    @DisplayName("the author may still read their own pending post")
    void readAllowsAuthorToSeeOwnPendingPost() {
        Post post = post();
        post.setStatus(PostStatus.PENDING_AUDIT.getCode());
        when(postMapper.selectById(42L)).thenReturn(post);
        when(contentMapper.selectByPostId(42L)).thenReturn(new PostContent(42L, "x", null));

        assertThat(service.getPost(42L, AUTHOR_ID).getId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("update is refused when the caller is not the author")
    void updateRefusedForNonAuthor() {
        when(postMapper.selectById(42L)).thenReturn(post());

        var request = new com.forum.post.dto.UpdatePostRequest();
        request.setContent("a new body");

        // No authentication context is present, so no staff override applies.
        assertThatThrownBy(() -> service.updatePost(42L, 999L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40300);

        verify(postMapper, never()).updateById(any(Post.class));
    }

    @Test
    @DisplayName("delete soft deletes and refreshes the category counter")
    void deleteSoftDeletes() {
        when(postMapper.selectById(42L)).thenReturn(post());

        service.deletePost(42L, AUTHOR_ID);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postMapper).updateById(captor.capture());
        assertThat(captor.getValue().getIsDeleted()).isEqualTo(1);
        assertThat(captor.getValue().getStatus()).isEqualTo(PostStatus.DELETED.getCode());
        verify(categoryMapper).refreshPostCount(CATEGORY_ID);
    }

    @Test
    @DisplayName("liking twice removes the like and decrements the counter")
    void likeToggles() {
        when(postMapper.selectById(42L)).thenReturn(post());
        when(postMapper.selectLikeId(42L, AUTHOR_ID)).thenReturn(null, 77L);
        when(postMapper.selectById(42L)).thenReturn(post());

        var first = service.toggleLike(42L, AUTHOR_ID);
        assertThat(first.getActive()).isTrue();
        verify(likeMapper).insert(any(PostLike.class));
        verify(postMapper).adjustLikeCount(42L, 1);

        var second = service.toggleLike(42L, AUTHOR_ID);
        assertThat(second.getActive()).isFalse();
        verify(likeMapper).deleteById(77L);
        verify(postMapper).adjustLikeCount(42L, -1);
    }

    @Test
    @DisplayName("collecting twice removes the bookmark")
    void collectToggles() {
        when(postMapper.selectById(42L)).thenReturn(post());
        when(postMapper.selectCollectId(42L, AUTHOR_ID)).thenReturn(null, 88L);
        when(postMapper.selectById(42L)).thenReturn(post());

        assertThat(service.toggleCollect(42L, AUTHOR_ID).getActive()).isTrue();
        assertThat(service.toggleCollect(42L, AUTHOR_ID).getActive()).isFalse();
        verify(collectMapper).insert(any(PostCollect.class));
        verify(collectMapper).deleteById(88L);
    }

    @Test
    @DisplayName("list always asks the mapper for published posts only")
    void listNeverWidensVisibility() {
        when(postMapper.selectPostPage(any(), any(), any(Boolean.class), any(Integer.class)))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<Post>(1, 20));

        PostQuery query = new PostQuery();
        service.listPosts(query, null, true, 1L, 20L);

        verify(postMapper).selectPostPage(any(), any(), eq(false), eq(PostStatus.PUBLISHED.getCode()));
    }

    @Test
    @DisplayName("tags are lower cased, de-duplicated and capped at five")
    void tagsAreNormalised() {
        when(userMapper.selectById(AUTHOR_ID)).thenReturn(activeUser());
        when(categoryMapper.selectById(CATEGORY_ID)).thenReturn(category());
        when(postMapper.insert(any(Post.class))).thenAnswer(call -> {
            ((Post) call.getArgument(0)).setId(1L);
            return 1;
        });

        CreatePostRequest request = createRequest();
        request.setTags(Arrays.asList("Java", "java", "Spring", "Vue", "SQL", "Extra", "More"));

        service.createPost(AUTHOR_ID, request);

        // At most five rows are inserted regardless of how many were supplied.
        verify(tagMapper, org.mockito.Mockito.times(5)).insert(any(com.forum.post.entity.PostTag.class));
        verify(tagMapper).deleteByPostId(anyLong());
    }

    private CreatePostRequest createRequest() {
        CreatePostRequest request = new CreatePostRequest();
        request.setCategoryId(CATEGORY_ID);
        request.setTitle("A reasonable title");
        request.setContent("the body");
        return request;
    }

    private Post post() {
        Post post = new Post();
        post.setId(42L);
        post.setCategoryId(CATEGORY_ID);
        post.setUserId(AUTHOR_ID);
        post.setTitle("A reasonable title");
        post.setStatus(PostStatus.PUBLISHED.getCode());
        post.setIsDeleted(0);
        post.setViewCount(1);
        post.setCommentCount(0);
        post.setLikeCount(0);
        post.setCollectCount(0);
        return post;
    }

    private User activeUser() {
        User user = new User();
        user.setId(AUTHOR_ID);
        user.setNickname("author");
        user.setStatus(UserStatus.ACTIVE.getCode());
        user.setIsDeleted(0);
        return user;
    }

    private Category category() {
        Category category = new Category();
        category.setId(CATEGORY_ID);
        category.setName("General");
        category.setStatus(1);
        category.setIsDeleted(0);
        return category;
    }
}
