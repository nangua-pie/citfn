package com.forum.log.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.forum.common.api.ApiResponse;
import com.forum.common.util.Pagination;
import com.forum.log.entity.OperationLog;
import com.forum.log.service.OperationLogService;
import com.forum.rbac.entity.PermissionCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Audit trail API, restricted to staff holding {@code log:view}.
 */
@Validated
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('" + PermissionCodes.OPERATION_LOG_VIEW + "')")
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping
    public ApiResponse<IPage<OperationLog>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size) {
        return ApiResponse.success(operationLogService.page(
                userId, module, (int) Pagination.normalizePage(page), (int) Pagination.normalizeSize(size)));
    }
}
