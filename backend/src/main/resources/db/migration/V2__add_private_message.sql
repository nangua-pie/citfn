-- Private messaging schema (AGENTS.md section 6.2).
-- im_conversation holds the per-viewer projection of a one to one chat:
-- each participant owns a row, so (user_id, target_user_id) is unique.
-- im_message is the durable record; Redis only caches online state, unread
-- counters and recent conversations.

CREATE TABLE IF NOT EXISTS im_conversation (
    id                   BIGINT       NOT NULL,
    user_id              BIGINT       NOT NULL,
    target_user_id       BIGINT       NOT NULL,
    last_message_id      BIGINT       DEFAULT NULL,
    last_message_preview VARCHAR(128) DEFAULT NULL,
    last_message_time    DATETIME     DEFAULT NULL,
    unread_count         INT          NOT NULL DEFAULT 0,
    is_deleted           TINYINT      NOT NULL DEFAULT 0,
    is_pinned            TINYINT      NOT NULL DEFAULT 0,
    create_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_conversation_user_target (user_id, target_user_id),
    KEY idx_conversation_user_recent (user_id, is_deleted, last_message_time),
    KEY idx_conversation_target (target_user_id),
    KEY idx_conversation_last_message (last_message_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS im_message (
    id              BIGINT      NOT NULL,
    conversation_id BIGINT      NOT NULL,
    sender_id       BIGINT      NOT NULL,
    receiver_id     BIGINT      NOT NULL,
    message_type    VARCHAR(16) NOT NULL COMMENT 'TEXT, IMAGE, FILE or SYSTEM',
    content         TEXT        NOT NULL,
    extra           VARCHAR(1000) DEFAULT NULL COMMENT 'JSON payload, e.g. attachment metadata',
    status          VARCHAR(16) NOT NULL COMMENT 'SENT, DELIVERED, READ, RECALLED',
    send_time       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_time       DATETIME    DEFAULT NULL,
    recall_time     DATETIME    DEFAULT NULL,
    is_deleted      TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    -- History is read with an id cursor, so this index serves the ORDER BY.
    KEY idx_message_conversation_id (conversation_id, id),
    KEY idx_message_receiver_status (receiver_id, status, id),
    KEY idx_message_sender (sender_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS im_message_attachment (
    id          BIGINT       NOT NULL,
    message_id  BIGINT       NOT NULL,
    object_key  VARCHAR(255) NOT NULL,
    url         VARCHAR(500) NOT NULL,
    file_name   VARCHAR(255) DEFAULT NULL,
    content_type VARCHAR(128) DEFAULT NULL,
    size        BIGINT       NOT NULL DEFAULT 0,
    sort_order  INT          NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_attachment_message (message_id, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- A blocked pair blocks both directions, hence two rows keyed by the blocker.
CREATE TABLE IF NOT EXISTS im_block (
    id             BIGINT   NOT NULL,
    user_id        BIGINT   NOT NULL COMMENT 'the user who applied the block',
    blocked_user_id BIGINT  NOT NULL,
    reason         VARCHAR(255) DEFAULT NULL,
    create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_block_user_blocked (user_id, blocked_user_id),
    KEY idx_block_blocked (blocked_user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- One row per user for administrator imposed messaging bans.
CREATE TABLE IF NOT EXISTS im_user_restriction (
    id           BIGINT       NOT NULL,
    user_id      BIGINT       NOT NULL,
    forbid_private_message TINYINT NOT NULL DEFAULT 1,
    reason       VARCHAR(255) DEFAULT NULL,
    operator_id  BIGINT       DEFAULT NULL,
    expires_at   DATETIME     DEFAULT NULL COMMENT 'null means a permanent ban',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_restriction_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
