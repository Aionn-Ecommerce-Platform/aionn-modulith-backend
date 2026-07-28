package com.aionn.chat.adapter.websocket;

import com.aionn.chat.application.dto.message.command.MessageCommands;
import com.aionn.chat.application.port.in.message.MarkDeliveredInputPort;
import com.aionn.chat.application.port.in.message.MarkReadInputPort;
import com.aionn.chat.application.port.in.message.SetTypingInputPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ChatStompControllerTest {

    private static final Principal USER = () -> "user-1";

    @Mock
    private SetTypingInputPort setTypingInputPort;
    @Mock
    private MarkDeliveredInputPort markDeliveredInputPort;
    @Mock
    private MarkReadInputPort markReadInputPort;

    @InjectMocks
    private ChatStompController controller;

    @Test
    void typingForwardsToInputPort() {
        controller.typing("conv-1", new ChatStompController.TypingPayload(true), USER);

        verify(setTypingInputPort).execute(new MessageCommands.SetTyping("user-1", "conv-1", true));
    }

    @Test
    void typingWithoutPrincipalIsIgnored() {
        controller.typing("conv-1", new ChatStompController.TypingPayload(true), null);

        verifyNoInteractions(setTypingInputPort);
    }

    @Test
    void deliveredForwardsToInputPort() {
        controller.delivered("msg-1", USER);

        verify(markDeliveredInputPort).execute(new MessageCommands.DeliverMessage("user-1", "msg-1"));
    }

    @Test
    void deliveredWithoutPrincipalIsIgnored() {
        controller.delivered("msg-1", null);

        verifyNoInteractions(markDeliveredInputPort);
    }

    @Test
    void readForwardsToInputPort() {
        controller.read("msg-1", USER);

        verify(markReadInputPort).execute(new MessageCommands.ReadMessage("user-1", "msg-1"));
    }

    @Test
    void readWithoutPrincipalIsIgnored() {
        controller.read("msg-1", null);

        verifyNoInteractions(markReadInputPort);
    }
}
