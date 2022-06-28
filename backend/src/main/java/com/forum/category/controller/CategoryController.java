package com.forum.category.controller;

import com.forum.category.dto.CategoryVO;
import com.forum.category.dto.CategorySaveRequest;
import com.forum.category.service.CategoryService;
import com.forum.common.api.ApiResponse;
import com.forum.rbac.entity.PermissionCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.List;

/**
 * Forum board API. Reads are public; management needs {@code category:manage}.
 */
@Validated
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<CategoryVO>> list() {
        return ApiResponse.success(categoryService.listEnabled());
    }

    @GetMapping("/{categoryId}")
    public ApiResponse<CategoryVO> get(@PathVariable Long categoryId) {
        return ApiResponse.success(categoryService.get(categoryId));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CATEGORY_MANAGE + "')")
    public ApiResponse<List<CategoryVO>> listAll() {
        return ApiResponse.success(categoryService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.CATEGORY_MANAGE + "')")
    public ApiResponse<CategoryVO> create(@Valid @RequestBody CategorySaveRequest request) {
        return ApiResponse.success(categoryService.create(request));
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CATEGORY_MANAGE + "')")
    public ApiResponse<CategoryVO> update(@PathVariable Long categoryId,
                                          @Valid @RequestBody CategorySaveRequest request) {
        return ApiResponse.success(categoryService.update(categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CATEGORY_MANAGE + "')")
    public ApiResponse<Void> delete(@PathVariable Long categoryId) {
        categoryService.delete(categoryId);
        return ApiResponse.success();
    }
}
