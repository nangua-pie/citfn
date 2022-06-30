package com.forum.report.controller;

import com.forum.common.api.ApiResponse;
import com.forum.common.api.PageResult;
import com.forum.common.security.CurrentUser;
import com.forum.rbac.entity.PermissionCodes;
import com.forum.report.dto.ReportRequest;
import com.forum.report.dto.ReportVO;
import com.forum.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * Report API: any signed in user may file one, only staff may handle them.
 */
@Validated
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ApiResponse<Void> report(@Valid @RequestBody ReportRequest request) {
        reportService.report(CurrentUser.requireUserId(), request);
        return ApiResponse.success();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.REPORT_HANDLE + "')")
    public ApiResponse<PageResult<ReportVO>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size) {
        return ApiResponse.success(
                reportService.list(CurrentUser.requireUserId(), status, page, size));
    }

    @PatchMapping("/{reportId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.REPORT_HANDLE + "')")
    public ApiResponse<Void> handle(@PathVariable Long reportId,
                                    @RequestParam boolean accept,
                                    @RequestParam(required = false) String note) {
        reportService.handle(CurrentUser.requireUserId(), reportId, accept, note);
        return ApiResponse.success();
    }
}
