package com.forum.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.forum.category.dto.CategoryVO;
import com.forum.category.dto.CategorySaveRequest;
import com.forum.category.entity.Category;
import com.forum.category.mapper.CategoryMapper;
import com.forum.category.service.CategoryService;
import com.forum.common.exception.BusinessException;
import com.forum.post.mapper.PostMapper;
import com.forum.post.entity.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Forum board management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final PostMapper postMapper;

    @Override
    public List<CategoryVO> listEnabled() {
        return categoryMapper.selectList(new QueryWrapper<Category>()
                        .eq("status", 1)
                        .orderByAsc("sort_order")
                        .orderByAsc("id"))
                .stream().map(CategoryServiceImpl::toVO).collect(Collectors.toList());
    }

    @Override
    public List<CategoryVO> listAll() {
        return categoryMapper.selectList(new QueryWrapper<Category>()
                        .orderByAsc("sort_order")
                        .orderByAsc("id"))
                .stream().map(CategoryServiceImpl::toVO).collect(Collectors.toList());
    }

    @Override
    public CategoryVO get(Long categoryId) {
        return toVO(requireCategory(categoryId));
    }

    @Override
    @Transactional
    public CategoryVO create(CategorySaveRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        category.setPostCount(0);
        category.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        category.setIsDeleted(0);
        categoryMapper.insert(category);
        log.info("category {} created", category.getId());
        return toVO(category);
    }

    @Override
    @Transactional
    public CategoryVO update(Long categoryId, CategorySaveRequest request) {
        Category category = requireCategory(categoryId);
        if (request.getName() != null && !request.getName().isBlank()) {
            category.setName(request.getName());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getIcon() != null) {
            category.setIcon(request.getIcon());
        }
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }
        categoryMapper.updateById(category);
        return toVO(category);
    }

    @Override
    @Transactional
    public void delete(Long categoryId) {
        Category category = requireCategory(categoryId);
        Long published = postMapper.selectCount(new QueryWrapper<Post>()
                .eq("category_id", categoryId)
                .eq("is_deleted", 0)
                .eq("status", 1));
        if (published != null && published > 0) {
            // Deleting a populated board would orphan its posts.
            throw BusinessException.conflict("move or delete the posts in this category first");
        }
        category.setIsDeleted(1);
        category.setStatus(0);
        categoryMapper.updateById(category);
        log.info("category {} deleted", categoryId);
    }

    private Category requireCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || Integer.valueOf(1).equals(category.getIsDeleted())) {
            throw BusinessException.notFound("category not found");
        }
        return category;
    }

    private static CategoryVO toVO(Category category) {
        return CategoryVO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .icon(category.getIcon())
                .sortOrder(category.getSortOrder())
                .postCount(category.getPostCount())
                .status(category.getStatus())
                .createTime(category.getCreateTime())
                .build();
    }
}
