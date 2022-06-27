package com.forum.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A comment with its replies nested underneath.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO {

    private Long id;
    private Long postId;
    private Long userId;
    private String authorNickname;
    private String authorAvatarUrl;
    private Long parentId;
    private Long rootId;
    private Long replyUserId;
    private String replyToNickname;
    private String content;
    private Integer likeCount;
    private Integer replyCount;
    private Boolean liked;
    private LocalDateTime createTime;

    /** Populated for root comments only. */
    private List<CommentVO> replies;
}
