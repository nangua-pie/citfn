package com.forum.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forum.log.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * Operation log access.
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
