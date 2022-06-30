package com.forum.log.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forum.common.security.AuthenticatedUser;
import com.forum.common.security.CurrentUser;
import com.forum.log.entity.OperationLog;
import com.forum.log.mapper.OperationLogMapper;
import com.forum.log.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Operation log writer and reader.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    /** Longest stored parameter blob; longer payloads are truncated. */
    private static final int MAX_PARAMS = 1000;

    /**
     * Field names whose values are blanked before a payload is stored.
     *
     * <p>Matched case insensitively against {@code "name": value} pairs in the
     * serialised request.
     */
    private static final List<String> SENSITIVE_FIELDS = Arrays.asList(
            "password", "newpassword", "currentpassword", "oldpassword",
            "token", "accesstoken", "refreshtoken", "secret", "code", "content");

    private final OperationLogMapper operationLogMapper;

    @Override
    @Async
    public void record(String module, String operation, String method, String params,
                       String result, String ip, String userAgent, Long costMs) {
        try {
            OperationLog entry = new OperationLog();
            AuthenticatedUser user = CurrentUser.find().orElse(null);
            if (user != null) {
                entry.setUserId(user.getUserId());
                entry.setUsername(user.getUsername());
            }
            entry.setModule(module);
            entry.setOperation(operation);
            entry.setMethod(truncate(method, 255));
            entry.setParams(sanitize(params));
            entry.setResult(result);
            entry.setIp(truncate(ip, 64));
            entry.setUserAgent(truncate(userAgent, 255));
            entry.setCostMs(costMs);
            entry.setCreateTime(LocalDateTime.now());
            operationLogMapper.insert(entry);
        } catch (RuntimeException ex) {
            // Auditing must never break the operation it is recording.
            log.warn("failed to record operation log for {}: {}", operation, ex.getMessage());
        }
    }

    @Override
    public IPage<OperationLog> page(Long userId, String module, int page, int size) {
        return operationLogMapper.selectPage(new Page<>(page, size), new QueryWrapper<OperationLog>()
                .eq(userId != null, "user_id", userId)
                .eq(module != null && !module.isBlank(), "module", module)
                .orderByDesc("create_time"));
    }

    /**
     * Blanks the values of sensitive fields and drops anything that looks like
     * a private message body.
     */
    static String sanitize(String params) {
        if (params == null || params.isBlank()) {
            return null;
        }
        String sanitized = params;
        for (String field : SENSITIVE_FIELDS) {
            sanitized = Pattern.compile(
                            "\"" + Pattern.quote(field.toLowerCase(Locale.ROOT)) + "\"\\s*:\\s*\"[^\"]*\"",
                            Pattern.CASE_INSENSITIVE)
                    .matcher(sanitized)
                    .replaceAll("\"" + field + "\":\"***\"");
        }
        return truncate(sanitized, MAX_PARAMS);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
