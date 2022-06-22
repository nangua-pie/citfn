package com.forum.post.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Post projection returned by the API.
 *
 * <p>{@code contentMd} is raw markdown. It is sanitised in the browser with
 * DOMPurify before rendering and is never treated as trusted HTML.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostVO {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private Long userId;
    private String authorNickname;
    private String authorAvatarUrl;

    private String title;
    private String summary;
    private String coverUrl;

    /** Only populated by the detail endpoint. */
    private String contentMd;

    private Integer viewCount;
    private Integer commentCount;
    private Integer likeCount;
    private Integer collectCount;
    private Integer reportCount;
    private Integer isTop;
    private Integer isEssential;
    private Integer status;

    /** Viewer specific state, filled when a caller is authenticated. */
    private Boolean liked;
    private Boolean collected;

    private List<String> tags;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
