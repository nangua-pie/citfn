package com.forum.report.service;

import com.forum.common.api.PageResult;
import com.forum.report.dto.ReportRequest;
import com.forum.report.dto.ReportVO;

/**
 * Reports against posts and comments.
 */
public interface ReportService {

    /**
     * Files a report.
 *
     * <p>One user may report a given target only once; a repeat raises a
     * conflict instead of flooding the moderation queue.
     */
    void report(Long reporterId, ReportRequest request);

    /** Pending and handled reports for the moderation queue. */
    PageResult<ReportVO> list(Long operatorId, Integer status, Long page, Long size);

    /**
     * Records a moderation decision and notifies the reporter.
     *
     * @param accept true to accept the report and delete the target
     */
    void handle(Long operatorId, Long reportId, boolean accept, String note);
}
