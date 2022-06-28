package com.forum.category.service;

import com.forum.category.dto.CategoryVO;
import com.forum.category.dto.CategorySaveRequest;

import java.util.List;

/**
 * Forum boards.
 */
public interface CategoryService {

    /** Enabled boards ordered by their configured sort order. */
    List<CategoryVO> listEnabled();

    /** Every board including disabled ones, for the admin console. */
    List<CategoryVO> listAll();

    CategoryVO get(Long categoryId);

    /** Creates a board; requires {@code category:manage}. */
    CategoryVO create(CategorySaveRequest request);

    /** Updates a board's name, description, icon or ordering. */
    CategoryVO update(Long categoryId, CategorySaveRequest request);

    /** Soft deletes a board once it holds no posts. */
    void delete(Long categoryId);
}
