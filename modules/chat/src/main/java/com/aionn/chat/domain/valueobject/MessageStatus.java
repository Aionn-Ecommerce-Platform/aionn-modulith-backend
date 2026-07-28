package com.aionn.chat.domain.valueobject;

public enum MessageStatus {
    SENT,
    DELIVERED,
    READ,
    RECALLED;

    public boolean canTransitionTo(MessageStatus next) {
        return switch (this) {
            case SENT -> next == DELIVERED || next == READ || next == RECALLED;
            case DELIVERED -> next == READ || next == RECALLED;
            case READ -> next == RECALLED;
            case RECALLED -> false;
        };
    }
}

