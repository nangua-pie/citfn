package com.forum.file.service;

import com.forum.file.dto.FileVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * File and image uploads.
 */
public interface FileService {

    /**
     * Stores an uploaded image.
     *
     * <p>The stored name is always a server generated random string, so a
     * crafted original name can never influence the path on disk or in the
     * bucket.
     */
    FileVO uploadImage(Long uploaderId, MultipartFile file);

    /** Stores any allowed attachment, with a larger size ceiling. */
    FileVO uploadFile(Long uploaderId, MultipartFile file);

    /** Files the caller uploaded, newest first. */
    List<FileVO> listMine(Long uploaderId, int limit);
}
