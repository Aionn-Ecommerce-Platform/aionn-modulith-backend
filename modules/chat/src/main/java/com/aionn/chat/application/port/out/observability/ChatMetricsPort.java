package com.aionn.chat.application.port.out.observability;

public interface ChatMetricsPort {

    void messageSent(String messageType);

    void conversationStarted();

    void messageRecalled();

    void userBlocked();

    void pushNotificationDispatched(String outcome);

    void autoReplyTriggered();
}
