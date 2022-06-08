package com.forum.category.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.category.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * Category table access.
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /** Recomputes the denormalised post counter from the authoritative rows. */
    @Update("UPDATE forum_category SET post_count = "
            + "(SELECT COUNT(1) FROM forum_post p WHERE p.category_id = #{categoryId} "
            + "  AND p.is_deleted = 0 AND p.status = 1) "
            + "WHERE id = #{categoryId} AND is_deleted = 0")
    int refreshPostCount(@Param("categoryId") Long categoryId);
}
