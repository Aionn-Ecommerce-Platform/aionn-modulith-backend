package com.aionn.chat.adapter.rest.controller;

import com.aionn.chat.adapter.rest.dto.conversation.request.JoinSupportRequest;
import com.aionn.chat.adapter.rest.dto.conversation.request.StartConversationRequest;
import com.aionn.chat.adapter.rest.dto.conversation.response.ConversationResponse;
import com.aionn.chat.adapter.rest.mapper.conversation.ConversationDtoMapper;
import com.aionn.chat.application.port.in.conversation.ArchiveConversationInputPort;
import com.aionn.chat.application.port.in.conversation.GetConversationQueryPort;
import com.aionn.chat.application.port.in.conversation.GetUnreadCountsQueryPort;
import com.aionn.chat.application.port.in.conversation.JoinSupportInputPort;
import com.aionn.chat.application.port.in.conversation.ListMyConversationsQueryPort;
import com.aionn.chat.application.port.in.conversation.MarkConversationReadInputPort;
import com.aionn.chat.application.port.in.conversation.StartConversationInputPort;
import com.aionn.chat.application.port.in.conversation.UnarchiveConversationInputPort;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat/conversations")
@RequiredArgsConstructor
public class ChatConversationController {

        private final StartConversationInputPort startConversationInputPort;
        private final ListMyConversationsQueryPort listMyConversationsQueryPort;
        private final GetConversationQueryPort getConversationQueryPort;
        private final MarkConversationReadInputPort markConversationReadInputPort;
        private final ArchiveConversationInputPort archiveConversationInputPort;
        private final UnarchiveConversationInputPort unarchiveConversationInputPort;
        private final JoinSupportInputPort joinSupportInputPort;
        private final GetUnreadCountsQueryPort getUnreadCountsQueryPort;
        private final ConversationDtoMapper dtoMapper;

        @PostMapping
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<ConversationResponse>> start(
                        @CurrentUserId String userId,
                        @Valid @RequestBody StartConversationRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(startConversationInputPort.execute(
                                                dtoMapper.toStartCommand(userId, request))),
                                "Conversation ready"));
        }

        @GetMapping
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<List<ConversationResponse>>> listMine(
                        @CurrentUserId String userId,
                        @RequestParam(defaultValue = "false") boolean includeArchived,
                        @RequestParam(defaultValue = "50") int limit) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponses(listMyConversationsQueryPort.execute(
                                                userId, includeArchived, limit)),
                                "Conversations fetched"));
        }

        @GetMapping("/unread-counts")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCounts(@CurrentUserId String userId) {
                return ResponseEntity.ok(ApiResponse.success(
                                getUnreadCountsQueryPort.execute(userId), "Unread counts fetched"));
        }

        @GetMapping("/{conversationId}")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<ConversationResponse>> get(
                        @CurrentUserId String userId,
                        @PathVariable String conversationId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(getConversationQueryPort.execute(userId, conversationId)),
                                "Conversation fetched"));
        }

        @PostMapping("/{conversationId}/read")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<ConversationResponse>> markRead(
                        @CurrentUserId String userId,
                        @PathVariable String conversationId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(markConversationReadInputPort.execute(
                                                dtoMapper.toMarkReadCommand(userId, conversationId))),
                                "Conversation marked read"));
        }

        @PostMapping("/{conversationId}/archive")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<ConversationResponse>> archive(
                        @CurrentUserId String userId,
                        @PathVariable String conversationId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(archiveConversationInputPort.execute(
                                                dtoMapper.toArchiveCommand(userId, conversationId))),
                                "Conversation archived"));
        }

        @PostMapping("/{conversationId}/unarchive")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<ConversationResponse>> unarchive(
                        @CurrentUserId String userId,
                        @PathVariable String conversationId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(unarchiveConversationInputPort.execute(
                                                dtoMapper.toUnarchiveCommand(userId, conversationId))),
                                "Conversation unarchived"));
        }

        @PostMapping("/{conversationId}/support")
        @PreAuthorize("hasAuthority('ROLE_CS_ADMIN')")
        public ResponseEntity<ApiResponse<ConversationResponse>> joinSupport(
                        @CurrentUserId String userId,
                        @PathVariable String conversationId,
                        @RequestBody(required = false) JoinSupportRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(joinSupportInputPort.execute(
                                                dtoMapper.toJoinSupportCommand(userId, conversationId,
                                                                request))),
                                "Support joined"));
        }
}
