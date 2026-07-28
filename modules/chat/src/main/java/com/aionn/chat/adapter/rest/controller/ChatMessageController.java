package com.aionn.chat.adapter.rest.controller;

import com.aionn.chat.adapter.rest.dto.message.request.SendMessageRequest;
import com.aionn.chat.adapter.rest.dto.message.request.SetTypingRequest;
import com.aionn.chat.adapter.rest.dto.message.response.MessageResponse;
import com.aionn.chat.adapter.rest.mapper.message.MessageDtoMapper;
import com.aionn.chat.application.port.in.message.ListMessagesQueryPort;
import com.aionn.chat.application.port.in.message.MarkDeliveredInputPort;
import com.aionn.chat.application.port.in.message.MarkReadInputPort;
import com.aionn.chat.application.port.in.message.RecallMessageInputPort;
import com.aionn.chat.application.port.in.message.SendMessageInputPort;
import com.aionn.chat.application.port.in.message.SetTypingInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.aionn.chat.adapter.rest.support.session.CurrentUserId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatMessageController {

        private final SendMessageInputPort sendMessageInputPort;
        private final ListMessagesQueryPort listMessagesQueryPort;
        private final MarkDeliveredInputPort markDeliveredInputPort;
        private final MarkReadInputPort markReadInputPort;
        private final RecallMessageInputPort recallMessageInputPort;
        private final SetTypingInputPort setTypingInputPort;
        private final MessageDtoMapper dtoMapper;

        @PostMapping("/conversations/{conversationId}/messages")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<MessageResponse>> send(
                        @CurrentUserId String userId,
                        @PathVariable String conversationId,
                        @Valid @RequestBody SendMessageRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(sendMessageInputPort.execute(
                                                dtoMapper.toSendCommand(userId, conversationId, request))),
                                "Message sent"));
        }

        @GetMapping("/conversations/{conversationId}/messages")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<List<MessageResponse>>> list(
                        @CurrentUserId String userId,
                        @PathVariable String conversationId,
                        @RequestParam(required = false) Instant before,
                        @RequestParam(defaultValue = "30") int limit) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponses(listMessagesQueryPort.execute(
                                                userId, conversationId, before, limit)),
                                "Messages fetched"));
        }

        @PostMapping("/messages/{messageId}/delivered")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<MessageResponse>> markDelivered(
                        @CurrentUserId String userId,
                        @PathVariable String messageId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(markDeliveredInputPort.execute(
                                                dtoMapper.toDeliverCommand(userId, messageId))),
                                "Message delivered"));
        }

        @PostMapping("/messages/{messageId}/read")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<MessageResponse>> markRead(
                        @CurrentUserId String userId,
                        @PathVariable String messageId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(markReadInputPort.execute(
                                                dtoMapper.toReadCommand(userId, messageId))),
                                "Message read"));
        }

        @PostMapping("/messages/{messageId}/recall")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<MessageResponse>> recall(
                        @CurrentUserId String userId,
                        @PathVariable String messageId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(recallMessageInputPort.execute(
                                                dtoMapper.toRecallCommand(userId, messageId))),
                                "Message recalled"));
        }

        @PostMapping("/conversations/{conversationId}/typing")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<Void>> setTyping(
                        @CurrentUserId String userId,
                        @PathVariable String conversationId,
                        @RequestBody SetTypingRequest request) {
                setTypingInputPort.execute(dtoMapper.toSetTypingCommand(
                                userId, conversationId, request));
                return ResponseEntity.ok(ApiResponse.success("Typing state updated"));
        }
}
