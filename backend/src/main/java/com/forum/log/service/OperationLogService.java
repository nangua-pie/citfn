package com.forum.log.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.forum.log.entity.OperationLog;

/**
 * Audit trail for state changing operations.
 */
public interface OperationLogService {

    /**
     * Records an operation.
     *
     * <p>Parameters are sanitised before they are stored: passwords, tokens and
     * private message bodies never reach the log table.
     */
    void record(String module, String operation, String method, String params,
                String result, String ip, String userAgent, Long costMs);

    /** Paged audit trail for the admin console. */
    IPage<OperationLog> page(Long userId, String module, int page, int size);
}
