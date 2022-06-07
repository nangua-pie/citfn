package com.forum.rbac.entity;

/**
 * Authority strings carried in the JWT.
 *
 * <p>Spring Security matches {@code @PreAuthorize} values against these, so a
 * typo here silently locks an endpoint rather than exposing it.
 */
public final class PermissionCodes {

    // --- Post moderation ---
    public static final String POST_TOP = "post:top";
    public static final String POST_ESSENTIAL = "post:essential";
    public static final String POST_EDIT_ANY = "post:edit:any";
    public static final String POST_DELETE_ANY = "post:delete:any";
    public static final String POST_AUDIT = "post:audit";

    // --- Comment moderation ---
    public static final String COMMENT_EDIT_ANY = "comment:edit:any";
    public static final String COMMENT_DELETE_ANY = "comment:delete:any";

    // --- Reporting ---
    public static final String REPORT_HANDLE = "report:handle";

    // --- User administration ---
    public static final String USER_MUTE = "user:mute";
    public static final String USER_BAN = "user:ban";
    public static final String USER_ROLE_ASSIGN = "user:role:assign";
    public static final String USER_PRIVATE_MESSAGE_BAN = "user:im:ban";

    // --- System ---
    public static final String SENSITIVE_MANAGE = "sensitive:manage";
    public static final String CATEGORY_MANAGE = "category:manage";
    public static final String OPERATION_LOG_VIEW = "log:view";

    private PermissionCodes() {
    }
}
