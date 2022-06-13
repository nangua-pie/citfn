package com.forum.report.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Moderation outcome of a report.
 */
@Getter
@AllArgsConstructor
public enum ReportStatus {

    PENDING(0),
    ACCEPTED(1),
    REJECTED(2);

    private final int code;

    public static ReportStatus of(Integer code) {
        if (code != null) {
            for (ReportStatus status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
        }
        return PENDING;
    }
}
