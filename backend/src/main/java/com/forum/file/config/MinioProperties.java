package com.forum.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Object storage settings.
 *
 * <p>Credentials come from configuration only; nothing is defaulted to a real
 * secret. When they are absent the service falls back to local disk so a
 * developer machine works without MinIO running.
 */
@Data
@Component
@ConfigurationProperties(prefix = "forum.minio")
public class MinioProperties {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket = "forum";

    /** True when both an endpoint and credentials were supplied. */
    public boolean isConfigured() {
        return endpoint != null && !endpoint.isBlank()
                && accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }
}
