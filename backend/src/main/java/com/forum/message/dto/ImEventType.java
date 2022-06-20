package com.forum.message.dto;

/**
 * Server pushed private message events (AGENTS.md section 6.4).
 */
public enum ImEventType {

    NEW_MESSAGE,
    MESSAGE_READ,
    MESSAGE_RECALLED,
    UNREAD_COUNT,
    USER_ONLINE,
    USER_OFFLINE
}
