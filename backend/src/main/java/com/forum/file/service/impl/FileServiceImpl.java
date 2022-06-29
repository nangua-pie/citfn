package com.forum.file.service.impl;

import com.forum.common.api.ResultCode;
import com.forum.common.exception.BusinessException;
import com.forum.file.config.MinioProperties;
import com.forum.file.dto.FileVO;
import com.forum.file.entity.FileRecord;
import com.forum.file.mapper.FileRecordMapper;
import com.forum.file.service.FileService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Upload handling.
 *
 * <p>Every upload is validated on declared type and size before anything is
 * written, and the stored name is generated on the server rather than derived
 * from the client file name. Object storage is used when configured; otherwise
 * the bytes land under a local directory so development works without MinIO.
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {

    /** Only real raster formats are accepted as images. */
    private static final Set<String> IMAGE_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"));

    private static final Set<String> FILE_TYPES = new HashSet<>(Arrays.asList(
            "application/pdf", "application/zip", "text/plain", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

    private static final DateTimeFormatter PATH_DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final FileRecordMapper fileRecordMapper;
    private final MinioProperties minioProperties;
    private final long maxImageSize;
    private final long maxFileSize;
    private final Path localDirectory;
    private final String publicBaseUrl;

    public FileServiceImpl(FileRecordMapper fileRecordMapper,
                           MinioProperties minioProperties,
                           @Value("${forum.file.max-image-size:5242880}") long maxImageSize,
                           @Value("${forum.file.max-file-size:10485760}") long maxFileSize,
                           @Value("${forum.storage.local-directory:./upload}") String localDirectory,
                           @Value("${forum.storage.public-base-url:http://localhost:8080/files}")
                           String publicBaseUrl) {
        this.fileRecordMapper = fileRecordMapper;
        this.minioProperties = minioProperties;
        this.maxImageSize = maxImageSize;
        this.maxFileSize = maxFileSize;
        this.localDirectory = Paths.get(localDirectory).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    @Override
    @Transactional
    public FileVO uploadImage(Long uploaderId, MultipartFile file) {
        return store(uploaderId, file, IMAGE_TYPES, maxImageSize);
    }

    @Override
    @Transactional
    public FileVO uploadFile(Long uploaderId, MultipartFile file) {
        Set<String> allowed = new HashSet<>(FILE_TYPES);
        allowed.addAll(IMAGE_TYPES);
        return store(uploaderId, file, allowed, maxFileSize);
    }

    @Override
    public List<FileVO> listMine(Long uploaderId, int limit) {
        return fileRecordMapper.selectRecentByUploader(uploaderId, Math.min(Math.max(limit, 1), 100))
                .stream()
                .map(record -> FileVO.builder()
                        .id(record.getId())
                        .originalName(record.getOriginalName())
                        .url(record.getUrl())
                        .contentType(record.getContentType())
                        .size(record.getSize())
                        .createTime(record.getCreateTime())
                        .build())
                .collect(Collectors.toList());
    }

    private FileVO store(Long uploaderId, MultipartFile file, Set<String> allowedTypes, long maxSize) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("the uploaded file is empty");
        }
        String contentType = file.getContentType() == null
                ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!allowedTypes.contains(contentType)) {
            // The declared content type is checked; the extension is not trusted.
            throw BusinessException.badRequest("file type is not allowed: " + contentType);
        }
        if (file.getSize() > maxSize) {
            throw BusinessException.badRequest("file must not exceed " + maxSize + " bytes");
        }

        String extension = resolveExtension(file.getOriginalFilename(), contentType);
        // Random name, so a crafted original name cannot influence the path.
        String storedName = UUID.randomUUID().toString().replace("-", "")
                + (extension.isEmpty() ? "" : "." + extension);
        String objectKey = LocalDateTime.now().format(PATH_DATE) + "/" + storedName;

        String url;
        try (InputStream in = file.getInputStream()) {
            url = minioProperties.isConfigured()
                    ? putToMinio(objectKey, in, file.getSize(), contentType)
                    : putToLocal(objectKey, in);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("failed to store upload for user {}", uploaderId, ex);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "the file could not be stored");
        }

        FileRecord record = new FileRecord();
        record.setOriginalName(safeOriginalName(file.getOriginalFilename()));
        record.setStoredName(storedName);
        record.setBucketName(minioProperties.isConfigured() ? minioProperties.getBucket() : "local");
        record.setObjectKey(objectKey);
        record.setUrl(url);
        record.setContentType(contentType);
        record.setSize(file.getSize());
        record.setUploaderId(uploaderId);
        record.setCreateTime(LocalDateTime.now());
        fileRecordMapper.insert(record);
        return FileVO.builder()
                .id(record.getId())
                .originalName(record.getOriginalName())
                .url(record.getUrl())
                .contentType(record.getContentType())
                .size(record.getSize())
                .createTime(record.getCreateTime())
                .build();
    }

    private String putToMinio(String objectKey, InputStream in, long size, String contentType)
            throws Exception {
        MinioClient client = MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
        client.putObject(PutObjectArgs.builder()
                .bucket(minioProperties.getBucket())
                .object(objectKey)
                .stream(in, size, -1)
                .contentType(contentType)
                .build());
        return minioProperties.getEndpoint() + "/" + minioProperties.getBucket() + "/" + objectKey;
    }

    private String putToLocal(String objectKey, InputStream in) throws Exception {
        Path target = localDirectory.resolve(objectKey).normalize();
        // Refuse anything that would escape the configured directory.
        if (!target.startsWith(localDirectory)) {
            throw BusinessException.badRequest("invalid upload path");
        }
        Files.createDirectories(target.getParent());
        Files.copy(in, target);
        return publicBaseUrl + "/" + objectKey;
    }

    /** Extension follows from the validated content type, not the file name. */
    private String resolveExtension(String originalName, String contentType) {
        switch (contentType) {
            case "image/jpeg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            case "image/webp":
                return "webp";
            case "application/pdf":
                return "pdf";
            case "application/zip":
                return "zip";
            case "text/plain":
                return "txt";
            case "application/msword":
                return "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return "docx";
            case "application/vnd.ms-excel":
                return "xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
                return "xlsx";
            default:
                return extensionFromName(originalName);
        }
    }

    private String extensionFromName(String originalName) {
        if (originalName == null) {
            return "";
        }
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            return "";
        }
        String extension = originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return extension.matches("[a-z0-9]{1,8}") ? extension : "";
    }

    /** Keeps a displayable name without letting it carry path separators. */
    private String safeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "unnamed";
        }
        String name = Paths.get(originalName).getFileName().toString();
        return name.length() <= 255 ? name : name.substring(name.length() - 255);
    }
}
