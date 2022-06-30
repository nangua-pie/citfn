package com.forum.report.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Payload for reporting a post or a comment.
 */
@Data
public class ReportRequest {

    @NotBlank(message = "is required")
    @Pattern(regexp = "POST|COMMENT", message = "must be POST or COMMENT")
    private String targetType;

    @NotNull(message = "is required")
    private Long targetId;

    @NotBlank(message = "is required")
    @Size(max = 32, message = "must not exceed 32 characters")
    private String reasonCode;

    @Size(max = 500, message = "must not exceed 500 characters")
    private String reasonDetail;
}
