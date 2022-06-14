package com.forum.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.report.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Report access.
 */
@Mapper
public interface ReportMapper extends BaseMapper<Report> {

    @Select("SELECT COUNT(1) FROM forum_report WHERE target_type = #{targetType} AND target_id = #{targetId}")
    int countForTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    @Select("SELECT COUNT(1) FROM forum_report WHERE target_type = #{targetType} AND target_id = #{targetId} "
            + "AND reporter_id = #{reporterId}")
    int countByReporter(@Param("targetType") String targetType,
                        @Param("targetId") Long targetId,
                        @Param("reporterId") Long reporterId);
}
