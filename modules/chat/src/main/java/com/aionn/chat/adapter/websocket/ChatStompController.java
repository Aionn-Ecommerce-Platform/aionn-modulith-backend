package com.aionn.chat.adapter.websocket;

import com.aionn.chat.application.dto.message.command.MessageCommands;
import com.aionn.chat.application.port.in.message.MarkDeliveredInputPort;
import com.aionn.chat.application.port.in.message.MarkReadInputPort;
import com.aionn.chat.application.port.in.message.SetTypingInputPort;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final SetTypingInputPort setTypingInputPort;
    private final MarkDeliveredInputPort markDeliveredInputPort;
    private final MarkReadInputPort markReadInputPort;

    @MessageMapping("/chat/conversations/{conversationId}/typing")
    public void typing(@DestinationVariable String conversationId,
            @Payload TypingPayload payload, Principal principal) {
        if (principal == null) {
            return;
        }
        setTypingInputPort.execute(new MessageCommands.SetTyping(
                principal.getName(), conversationId, payload.typing()));
    }

    @MessageMapping("/chat/messages/{messageId}/delivered")
    public void delivered(@DestinationVariable String messageId, Principal principal) {
        if (principal == null) {
            return;
        }
        markDeliveredInputPort.execute(new MessageCommands.DeliverMessage(principal.getName(), messageId));
    }

    @MessageMapping("/chat/messages/{messageId}/read")
    public void read(@DestinationVariable String messageId, Principal principal) {
        if (principal == null) {
            return;
        }
        markReadInputPort.execute(new MessageCommands.ReadMessage(principal.getName(), messageId));
    }

    public record TypingPayload(boolean typing) {
    }
}
