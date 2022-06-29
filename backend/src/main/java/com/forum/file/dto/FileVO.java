package com.forum.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Upload result returned to clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileVO {

    private Long id;
    private String originalName;
    private String url;
    private String contentType;
    private Long size;
    private LocalDateTime createTime;
}
