package com.forum.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forum.comment.entity.Comment;
import com.forum.comment.entity.CommentStatus;
import com.forum.comment.mapper.CommentMapper;
import com.forum.common.api.PageResult;
import com.forum.common.exception.BusinessException;
import com.forum.common.util.Pagination;
import com.forum.notification.entity.NotificationType;
import com.forum.notification.service.NotificationService;
import com.forum.post.entity.Post;
import com.forum.post.entity.PostStatus;
import com.forum.post.mapper.PostMapper;
import com.forum.report.dto.ReportRequest;
import com.forum.report.dto.ReportVO;
import com.forum.report.entity.Report;
import com.forum.report.entity.ReportStatus;
import com.forum.report.mapper.ReportMapper;
import com.forum.report.service.ReportService;
import com.forum.user.entity.User;
import com.forum.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Report intake and moderation.
 *
 * <p>Accepting a report removes the reported content, so the decision and the
 * deletion share one transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final String TARGET_POST = "POST";
    private static final String TARGET_COMMENT = "COMMENT";

    private final ReportMapper reportMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void report(Long reporterId, ReportRequest request) {
        requireTargetExists(request.getTargetType(), request.getTargetId());

        int existing = reportMapper.countByReporter(
                request.getTargetType(), request.getTargetId(), reporterId);
        if (existing > 0) {
            throw BusinessException.conflict("you have already reported this item");
        }

        Report report = new Report();
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReporterId(reporterId);
        report.setReasonCode(request.getReasonCode());
        report.setReasonDetail(request.getReasonDetail());
        report.setStatus(ReportStatus.PENDING.getCode());
        reportMapper.insert(report);

        if (TARGET_POST.equals(request.getTargetType())) {
            postMapper.adjustReportCount(request.getTargetId(), 1);
        }
        log.info("report {} filed by user {} on {} {}",
                report.getId(), reporterId, request.getTargetType(), request.getTargetId());
    }

    @Override
    public PageResult<ReportVO> list(Long operatorId, Integer status, Long page, Long size) {
        Page<Report> result = reportMapper.selectPage(
                new Page<>(Pagination.normalizePage(page), Pagination.normalizeSize(size)),
                new QueryWrapper<Report>()
                        .eq(status != null, "status", status)
                        .orderByDesc("create_time"));

        List<ReportVO> records = result.getRecords().stream()
                .map(ReportServiceImpl::toVO)
                .collect(Collectors.toList());
        if (records.isEmpty()) {
            return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
        }
        List<Long> reporterIds = result.getRecords().stream()
                .map(Report::getReporterId)
                .distinct()
                .collect(Collectors.toList());
        java.util.Map<Long, User> reporters = new java.util.HashMap<>();
        for (User user : userMapper.selectBatchIds(reporterIds)) {
            reporters.put(user.getId(), user);
        }
        for (ReportVO vo : records) {
            User reporter = reporters.get(vo.getReporterId());
            vo.setReporterNickname(reporter == null ? null : reporter.getNickname());
        }
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public void handle(Long operatorId, Long reportId, boolean accept, String note) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw BusinessException.notFound("report not found");
        }
        if (ReportStatus.PENDING != ReportStatus.of(report.getStatus())) {
            throw BusinessException.conflict("this report has already been handled");
        }
        if (accept) {
            removeReportedTarget(report);
        }
        report.setStatus(accept ? ReportStatus.ACCEPTED.getCode() : ReportStatus.REJECTED.getCode());
        report.setHandleUserId(operatorId);
        report.setHandleTime(LocalDateTime.now());
        report.setHandleNote(note);
        reportMapper.updateById(report);

        notificationService.notify(report.getReporterId(), NotificationType.REPORT_RESULT,
                accept ? "Your report was accepted" : "Your report was rejected",
                note == null ? "" : note,
                report.getTargetType(), report.getTargetId());
        log.info("report {} handled by user {} accept={}", reportId, operatorId, accept);
    }

    private void requireTargetExists(String targetType, Long targetId) {
        boolean exists;
        if (TARGET_POST.equals(targetType)) {
            exists = postMapper.selectById(targetId) != null;
        } else if (TARGET_COMMENT.equals(targetType)) {
            exists = commentMapper.selectById(targetId) != null;
        } else {
            throw BusinessException.badRequest("unsupported report target");
        }
        if (!exists) {
            throw BusinessException.notFound("the reported item no longer exists");
        }
    }

    private void removeReportedTarget(Report report) {
        if (TARGET_POST.equals(report.getTargetType())) {
            Post post = postMapper.selectById(report.getTargetId());
            if (post != null && !Integer.valueOf(1).equals(post.getIsDeleted())) {
                post.setIsDeleted(1);
                post.setStatus(PostStatus.DELETED.getCode());
                postMapper.updateById(post);
            }
            return;
        }
        Comment comment = commentMapper.selectById(report.getTargetId());
        if (comment != null && !Integer.valueOf(1).equals(comment.getIsDeleted())) {
            comment.setIsDeleted(1);
            comment.setStatus(CommentStatus.DELETED.getCode());
            commentMapper.updateById(comment);
            postMapper.adjustCommentCount(comment.getPostId(), -1);
            if (comment.getRootId() != null) {
                commentMapper.adjustReplyCount(comment.getRootId(), -1);
            }
        }
    }

    private static ReportVO toVO(Report report) {
        return ReportVO.builder()
                .id(report.getId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reporterId(report.getReporterId())
                .reasonCode(report.getReasonCode())
                .reasonDetail(report.getReasonDetail())
                .status(report.getStatus())
                .handleUserId(report.getHandleUserId())
                .handleTime(report.getHandleTime())
                .handleNote(report.getHandleNote())
                .createTime(report.getCreateTime())
                .build();
    }
}
