package com.forum.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Serves locally stored uploads under {@code /files/**}.
 *
 * <p>Only used when object storage is not configured; with MinIO the URLs point
 * at the bucket instead and this handler simply finds nothing.
 */
@Configuration
public class StorageResourceConfig implements WebMvcConfigurer {

    private final String localDirectory;

    public StorageResourceConfig(
            @Value("${forum.storage.local-directory:./upload}") String localDirectory) {
        this.localDirectory = Paths.get(localDirectory).toAbsolutePath().normalize().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + localDirectory + "/");
    }
}
