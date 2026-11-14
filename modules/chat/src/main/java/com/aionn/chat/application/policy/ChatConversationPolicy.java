package com.aionn.chat.application.policy;

public interface ChatConversationPolicy {

    int listDefaultLimit();

    int listMaxLimit();

    int unreadScanLimit();
}
