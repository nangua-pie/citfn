package com.forum.sensitive.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.forum.common.api.ApiResponse;
import com.forum.common.exception.BusinessException;
import com.forum.rbac.entity.PermissionCodes;
import com.forum.sensitive.entity.SensitiveWord;
import com.forum.sensitive.mapper.SensitiveWordMapper;
import com.forum.sensitive.service.SensitiveWordService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Sensitive word administration.
 *
 * <p>Every mutation refreshes the in-memory cache so a change takes effect
 * without waiting for the scheduled reload.
 */
@Validated
@RestController
@RequestMapping("/api/admin/sensitive-words")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('" + PermissionCodes.SENSITIVE_MANAGE + "')")
public class SensitiveWordController {

    private final SensitiveWordMapper sensitiveWordMapper;
    private final SensitiveWordService sensitiveWordService;

    @GetMapping
    public ApiResponse<List<SensitiveWord>> list() {
        return ApiResponse.success(
                sensitiveWordMapper.selectList(new QueryWrapper<SensitiveWord>().orderByAsc("word")));
    }

    @PostMapping
    @Transactional
    public ApiResponse<SensitiveWord> create(@Valid @RequestBody SensitiveWordRequest request) {
        String word = request.getWord().trim();
        if (sensitiveWordMapper.countByWord(word) > 0) {
            throw BusinessException.conflict("this word is already configured");
        }
        SensitiveWord entity = new SensitiveWord();
        entity.setWord(word);
        entity.setCategory(request.getCategory() == null ? "DEFAULT" : request.getCategory());
        entity.setReplaceWith(request.getReplaceWith() == null ? "***" : request.getReplaceWith());
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        sensitiveWordMapper.insert(entity);
        sensitiveWordService.refresh();
        return ApiResponse.success(entity);
    }

    @PutMapping("/{wordId}")
    @Transactional
    public ApiResponse<SensitiveWord> update(@PathVariable Long wordId,
                                              @Valid @RequestBody SensitiveWordRequest request) {
        SensitiveWord entity = sensitiveWordMapper.selectById(wordId);
        if (entity == null) {
            throw BusinessException.notFound("word not found");
        }
        if (request.getWord() != null && !request.getWord().isBlank()) {
            entity.setWord(request.getWord().trim());
        }
        if (request.getCategory() != null) {
            entity.setCategory(request.getCategory());
        }
        if (request.getReplaceWith() != null) {
            entity.setReplaceWith(request.getReplaceWith());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        entity.setUpdateTime(LocalDateTime.now());
        sensitiveWordMapper.updateById(entity);
        sensitiveWordService.refresh();
        return ApiResponse.success(entity);
    }

    @DeleteMapping("/{wordId}")
    @Transactional
    public ApiResponse<Void> delete(@PathVariable Long wordId) {
        if (sensitiveWordMapper.selectById(wordId) == null) {
            throw BusinessException.notFound("word not found");
        }
        sensitiveWordMapper.deleteById(wordId);
        sensitiveWordService.refresh();
        return ApiResponse.success();
    }

    /** Create and update payload. */
    @Data
    public static class SensitiveWordRequest {

        @NotBlank(message = "is required")
        @Size(max = 128, message = "must not exceed 128 characters")
        private String word;

        @Size(max = 32, message = "must not exceed 32 characters")
        private String category;

        @Size(max = 16, message = "must not exceed 16 characters")
        private String replaceWith;

        private Integer status;
    }
}
