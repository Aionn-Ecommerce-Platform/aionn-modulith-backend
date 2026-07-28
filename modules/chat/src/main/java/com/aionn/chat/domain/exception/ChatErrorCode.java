package com.aionn.chat.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode {
    CONVERSATION_NOT_FOUND("CHT_001", "Conversation not found"),
    CONVERSATION_FORBIDDEN("CHT_002", "User is not a participant of this conversation"),
    CONVERSATION_ARCHIVED("CHT_003", "Conversation is archived"),

    MESSAGE_NOT_FOUND("CHT_101", "Message not found"),
    MESSAGE_FORBIDDEN("CHT_102", "User is not the sender of this message"),
    MESSAGE_TOO_LONG("CHT_103", "Message exceeds the maximum length"),
    MESSAGE_EMPTY("CHT_104", "Message content must not be empty"),
    MESSAGE_RECALL_WINDOW_EXPIRED("CHT_105", "Recall window has expired"),
    MESSAGE_ALREADY_RECALLED("CHT_106", "Message is already recalled"),

    USER_BLOCKED("CHT_201", "Recipient has blocked you"),
    BLOCK_NOT_FOUND("CHT_202", "Block record not found"),
    BLOCK_SELF("CHT_203", "Cannot block yourself"),

    AUTO_REPLY_NOT_FOUND("CHT_301", "Auto-reply config not found"),
    AUTO_REPLY_FORBIDDEN("CHT_302", "Caller does not own the requested merchant auto-reply config"),

    INVALID_ARGUMENT("CHT_900", "Invalid argument");

    private final String code;
    private final String defaultMessage;
}
