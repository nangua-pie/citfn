package com.forum.post.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.forum.category.entity.Category;
import com.forum.category.mapper.CategoryMapper;
import com.forum.post.entity.Post;
import com.forum.post.entity.PostStatus;
import com.forum.post.entity.PostTag;
import com.forum.post.mapper.PostContentMapper;
import com.forum.post.mapper.PostMapper;
import com.forum.post.mapper.PostTagMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mapper integration tests against a real MariaDB.
 *
 * <p>These verify the parts of the persistence layer that a mock cannot: that
 * Flyway's schema matches the entity mappings, that the logical delete filter is
 * applied to reads, and that the post body really lives in its own table.
 *
 * <p>Skipped unless {@code FORUM_DB_PASSWORD} is set, so {@code mvn test} stays
 * green on a machine with no database. Point it at a scratch schema and the
 * suite runs for real.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "FORUM_DB_PASSWORD", matches = ".+")
class PostMapperIntegrationTest {

    @Autowired
    private PostMapper postMapper;
    @Autowired
    private PostContentMapper contentMapper;
    @Autowired
    private PostTagMapper tagMapper;
    @Autowired
    private CategoryMapper categoryMapper;

    private Long categoryId;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setName("integration-" + System.nanoTime());
        category.setSortOrder(0);
        category.setPostCount(0);
        category.setStatus(1);
        category.setIsDeleted(0);
        categoryMapper.insert(category);
        categoryId = category.getId();
    }

    @AfterEach
    void tearDown() {
        postMapper.delete(new QueryWrapper<Post>().eq("category_id", categoryId));
        categoryMapper.deleteById(categoryId);
    }

    private Post newPost(String title, PostStatus status) {
        Post post = new Post();
        post.setCategoryId(categoryId);
        post.setUserId(1L);
        post.setTitle(title);
        post.setSummary("summary");
        post.setViewCount(0);
        post.setCommentCount(0);
        post.setLikeCount(0);
        post.setCollectCount(0);
        post.setReportCount(0);
        post.setIsTop(0);
        post.setIsEssential(0);
        post.setStatus(status.getCode());
        post.setIsDeleted(0);
        postMapper.insert(post);
        return post;
    }

    @Test
    @DisplayName("an inserted post gets a snowflake id and server timestamps")
    void insertPopulatesIdAndTimestamps() {
        Post post = newPost("first", PostStatus.PUBLISHED);

        assertThat(post.getId()).isNotNull();
        assertThat(post.getCreateTime()).isNotNull();

        Post reloaded = postMapper.selectById(post.getId());
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getTitle()).isEqualTo("first");
    }

    @Test
    @DisplayName("the body is stored separately and is not part of the list row")
    void bodyLivesInItsOwnTable() {
        Post post = newPost("with body", PostStatus.PUBLISHED);

        contentMapper.insert(new com.forum.post.entity.PostContent(
                post.getId(), "# markdown body", LocalDateTime.now()));

        // The list projection carries no body column.
        Post listRow = postMapper.selectById(post.getId());
        assertThat(listRow.getTitle()).isEqualTo("with body");

        assertThat(contentMapper.selectByPostId(post.getId()).getContentMd())
                .isEqualTo("# markdown body");
    }

    @Test
    @DisplayName("logical delete hides the row from a normal read")
    void logicalDeleteFiltersReads() {
        Post post = newPost("to remove", PostStatus.PUBLISHED);

        postMapper.deleteById(post.getId());

        assertThat(postMapper.selectById(post.getId())).isNull();
        assertThat(postMapper.selectCount(new QueryWrapper<Post>().eq("id", post.getId()))).isZero();
    }

    @Test
    @DisplayName("the denormalised like counter never goes negative")
    void countersAreClampedAtZero() {
        Post post = newPost("counters", PostStatus.PUBLISHED);

        postMapper.adjustLikeCount(post.getId(), -5);

        assertThat(postMapper.selectById(post.getId()).getLikeCount()).isZero();
    }

    @Test
    @DisplayName("refreshing a category counter counts only published posts")
    void categoryCounterCountsPublishedPosts() {
        newPost("published one", PostStatus.PUBLISHED);
        newPost("pending one", PostStatus.PENDING_AUDIT);

        categoryMapper.refreshPostCount(categoryId);

        assertThat(categoryMapper.selectById(categoryId).getPostCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("tags round trip in insertion order")
    void tagsRoundTripInOrder() {
        Post post = newPost("tagged", PostStatus.PUBLISHED);
        tagMapper.insert(PostTag.of(post.getId(), "java"));
        tagMapper.insert(PostTag.of(post.getId(), "spring"));

        assertThat(tagMapper.selectTagNames(post.getId())).containsExactly("java", "spring");

        tagMapper.deleteByPostId(post.getId());
        assertThat(tagMapper.selectTagNames(post.getId())).isEmpty();
    }
}
