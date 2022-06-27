package com.forum.comment.service.impl;

import com.forum.comment.dto.CommentVO;
import com.forum.comment.dto.CreateCommentRequest;
import com.forum.comment.entity.Comment;
import com.forum.comment.entity.CommentLike;
import com.forum.comment.entity.CommentStatus;
import com.forum.comment.mapper.CommentLikeMapper;
import com.forum.comment.mapper.CommentMapper;
import com.forum.comment.service.CommentService;
import com.forum.common.exception.BusinessException;
import com.forum.common.security.CurrentUser;
import com.forum.notification.entity.NotificationType;
import com.forum.notification.service.NotificationService;
import com.forum.post.dto.LikeResult;
import com.forum.post.entity.Post;
import com.forum.post.mapper.PostMapper;
import com.forum.rbac.entity.PermissionCodes;
import com.forum.sensitive.service.SensitiveWordService;
import com.forum.user.entity.User;
import com.forum.user.entity.UserStatus;
import com.forum.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Comment and reply handling.
 *
 * <p>A reply keeps two links: {@code parentId} to the comment being answered and
 * {@code rootId} to the thread root, so a whole sub-thread loads with one
 * indexed query instead of a lookup per comment.
 */
@Slf4j
@Service
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final CommentLikeMapper likeMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final SensitiveWordService sensitiveWordService;
    private final NotificationService notificationService;

    public CommentServiceImpl(CommentMapper commentMapper,
                              CommentLikeMapper likeMapper,
                              PostMapper postMapper,
                              UserMapper userMapper,
                              SensitiveWordService sensitiveWordService,
                              NotificationService notificationService) {
        this.commentMapper = commentMapper;
        this.likeMapper = likeMapper;
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.sensitiveWordService = sensitiveWordService;
        this.notificationService = notificationService;
    }

    @Override
    public List<CommentVO> listByPost(Long postId, Long viewerId) {
        Post post = postMapper.selectById(postId);
        if (post == null || Integer.valueOf(1).equals(post.getIsDeleted())) {
            throw BusinessException.notFound("post not found");
        }
        List<Comment> roots = commentMapper.selectRootComments(postId);
        if (roots.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> rootIds = roots.stream().map(Comment::getId).collect(Collectors.toList());
        List<Comment> replies = commentMapper.selectRepliesByRootIds(rootIds);

        Set<Long> userIds = new LinkedHashSet<>();
        List<Comment> all = new ArrayList<>(roots);
        all.addAll(replies);
        for (Comment comment : all) {
            userIds.add(comment.getUserId());
            if (comment.getReplyUserId() != null) {
                userIds.add(comment.getReplyUserId());
            }
        }
        Map<Long, User> users = loadUsers(userIds);

        Map<Long, CommentVO> byId = new HashMap<>();
        List<CommentVO> result = new ArrayList<>(roots.size());
        for (Comment root : roots) {
            CommentVO vo = toVO(root, users, viewerId);
            vo.setReplies(new ArrayList<>());
            byId.put(root.getId(), vo);
            result.add(vo);
        }
        for (Comment reply : replies) {
            CommentVO parent = byId.get(reply.getRootId());
            if (parent == null) {
                continue;
            }
            CommentVO vo = toVO(reply, users, viewerId);
            User repliedTo = reply.getReplyUserId() == null ? null : users.get(reply.getReplyUserId());
            vo.setReplyToNickname(repliedTo == null ? null : repliedTo.getNickname());
            parent.getReplies().add(vo);
        }
        return result;
    }

    @Override
    @Transactional
    public CommentVO createComment(Long authorId, CreateCommentRequest request) {
        Post post = postMapper.selectById(request.getPostId());
        if (post == null || Integer.valueOf(1).equals(post.getIsDeleted())) {
            throw BusinessException.notFound("post not found");
        }
        User author = userMapper.selectById(authorId);
        if (author == null) {
            throw BusinessException.unauthorized("not logged in");
        }
        if (!UserStatus.of(author.getStatus()).canPost()) {
            throw BusinessException.forbidden("your account cannot comment");
        }

        Comment parent = null;
        Long rootId = null;
        if (request.getParentId() != null) {
            parent = commentMapper.selectById(request.getParentId());
            // The parent must belong to the same post, otherwise a reply could
            // be grafted onto an unrelated thread.
            if (parent == null || Integer.valueOf(1).equals(parent.getIsDeleted())
                    || !parent.getPostId().equals(request.getPostId())) {
                throw BusinessException.badRequest("the comment being replied to is unavailable");
            }
            rootId = parent.getRootId() == null ? parent.getId() : parent.getRootId();
        }

        Comment comment = new Comment();
        comment.setPostId(request.getPostId());
        comment.setUserId(authorId);
        comment.setContent(sensitiveWordService.mask(request.getContent().trim()));
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setStatus(CommentStatus.VISIBLE.getCode());
        comment.setIsDeleted(0);
        if (parent == null) {
            comment.setParentId(null);
            comment.setRootId(null);
            comment.setReplyUserId(null);
        } else {
            comment.setParentId(parent.getId());
            comment.setReplyUserId(parent.getUserId());
            comment.setRootId(rootId);
        }
        commentMapper.insert(comment);
        postMapper.adjustCommentCount(comment.getPostId(), 1);
        if (rootId != null) {
            // Only the thread root keeps the visible reply count.
            commentMapper.adjustReplyCount(rootId, 1);
        }

        notifyTarget(post, parent, author, comment);
        log.info("comment {} created on post {} by user {}", comment.getId(), post.getId(), authorId);

        CommentVO vo = toVO(comment, loadUsers(Set.of(authorId)), authorId);
        vo.setReplyToNickname(parent == null ? null : userNickname(parent.getUserId()));
        return vo;
    }

    @Override
    @Transactional
    public CommentVO updateComment(Long commentId, Long operatorId, String content) {
        Comment comment = requireComment(commentId);
        if (!comment.getUserId().equals(operatorId)
                && !hasAuthority(PermissionCodes.COMMENT_EDIT_ANY)) {
            throw BusinessException.forbidden("only the author can edit this comment");
        }
        comment.setContent(sensitiveWordService.mask(content.trim()));
        commentMapper.updateById(comment);
        return toVO(comment, loadUsers(Set.of(comment.getUserId())), operatorId);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long operatorId) {
        Comment comment = requireComment(commentId);
        if (!comment.getUserId().equals(operatorId)
                && !hasAuthority(PermissionCodes.COMMENT_DELETE_ANY)) {
            throw BusinessException.forbidden("only the author can delete this comment");
        }
        comment.setIsDeleted(1);
        comment.setStatus(CommentStatus.DELETED.getCode());
        commentMapper.updateById(comment);
        postMapper.adjustCommentCount(comment.getPostId(), -1);
        if (comment.getRootId() != null) {
            commentMapper.adjustReplyCount(comment.getRootId(), -1);
        }
        log.info("comment {} deleted by user {}", commentId, operatorId);
    }

    @Override
    @Transactional
    public LikeResult toggleLike(Long commentId, Long userId) {
        requireComment(commentId);
        Long existing = commentMapper.selectLikeId(commentId, userId);
        boolean active;
        if (existing == null) {
            likeMapper.insert(CommentLike.of(commentId, userId));
            commentMapper.adjustLikeCount(commentId, 1);
            active = true;
        } else {
            likeMapper.deleteById(existing);
            commentMapper.adjustLikeCount(commentId, -1);
            active = false;
        }
        Comment updated = commentMapper.selectById(commentId);
        return new LikeResult(active, updated == null ? 0 : updated.getLikeCount());
    }

    // --- helpers -------------------------------------------------------

    private Comment requireComment(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || Integer.valueOf(1).equals(comment.getIsDeleted())) {
            throw BusinessException.notFound("comment not found");
        }
        return comment;
    }

    private boolean hasAuthority(String code) {
        return CurrentUser.find().map(user -> user.hasAuthority(code)).orElse(false);
    }

    private Map<Long, User> loadUsers(Set<Long> userIds) {
        Map<Long, User> users = new HashMap<>();
        if (userIds.isEmpty()) {
            return users;
        }
        for (User user : userMapper.selectBatchIds(userIds)) {
            users.put(user.getId(), user);
        }
        return users;
    }

    private String userNickname(Long userId) {
        User user = userId == null ? null : userMapper.selectById(userId);
        return user == null ? null : user.getNickname();
    }

    /** Notifies the post author, or the replied-to author for a reply. */
    private void notifyTarget(Post post, Comment parent, User author, Comment comment) {
        Long recipientId = parent == null ? post.getUserId() : parent.getUserId();
        if (recipientId == null || recipientId.equals(author.getId())) {
            return;
        }
        NotificationType type = parent == null ? NotificationType.COMMENT : NotificationType.REPLY;
        String title = parent == null ? "New comment on your post" : "New reply to your comment";
        notificationService.notify(recipientId, type, title,
                author.getNickname() + " commented on \"" + truncate(post.getTitle()) + "\"",
                parent == null ? "POST" : "COMMENT", comment.getId());
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 40 ? text : text.substring(0, 40);
    }

    private CommentVO toVO(Comment comment, Map<Long, User> users, Long viewerId) {
        User author = users.get(comment.getUserId());
        return CommentVO.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .userId(comment.getUserId())
                .authorNickname(author == null ? null : author.getNickname())
                .authorAvatarUrl(author == null ? null : author.getAvatarUrl())
                .parentId(comment.getParentId())
                .rootId(comment.getRootId())
                .replyUserId(comment.getReplyUserId())
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .replyCount(comment.getReplyCount())
                .liked(viewerId != null && commentMapper.selectLikeId(comment.getId(), viewerId) != null)
                .createTime(comment.getCreateTime())
                .build();
    }
}
