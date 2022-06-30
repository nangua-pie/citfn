package com.forum.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Report projection for the moderation queue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportVO {

    private Long id;
    private String targetType;
    private Long targetId;
    private Long reporterId;
    private String reporterNickname;
    private String reasonCode;
    private String reasonDetail;
    private Integer status;
    private Long handleUserId;
    private LocalDateTime handleTime;
    private String handleNote;
    private LocalDateTime createTime;
}
