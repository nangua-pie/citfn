package com.forum.file.controller;

import com.forum.common.api.ApiResponse;
import com.forum.common.security.CurrentUser;
import com.forum.file.dto.FileVO;
import com.forum.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Upload API.
 *
 * <p>Type and size limits live in the service so every entry point, including
 * any future one, is checked the same way.
 */
@Validated
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/image")
    public ApiResponse<FileVO> uploadImage(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(fileService.uploadImage(CurrentUser.requireUserId(), file));
    }

    @PostMapping
    public ApiResponse<FileVO> uploadFile(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(fileService.uploadFile(CurrentUser.requireUserId(), file));
    }

    @GetMapping("/mine")
    public ApiResponse<List<FileVO>> mine(@RequestParam(defaultValue = "20") Integer limit) {
        return ApiResponse.success(fileService.listMine(CurrentUser.requireUserId(), limit));
    }
}
