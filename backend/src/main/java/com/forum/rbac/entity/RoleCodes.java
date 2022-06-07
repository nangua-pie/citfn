package com.forum.rbac.entity;

/**
 * Built in role codes. Rows are inserted by the bootstrap migration so the
 * constants here and the database never drift.
 */
public final class RoleCodes {

    /** Everyone who has registered. */
    public static final String USER = "ROLE_USER";

    /** Forum staff: can moderate content without touching system settings. */
    public static final String MODERATOR = "ROLE_MODERATOR";

    /** Full administrative access. */
    public static final String ADMIN = "ROLE_ADMIN";

    private RoleCodes() {
    }
}
