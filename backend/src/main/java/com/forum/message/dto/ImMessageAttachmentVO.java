package com.forum.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Attachment projection attached to an image or file message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImMessageAttachmentVO {

    private String objectKey;
    private String url;
    private String fileName;
    private String contentType;
    private Long size;
}
