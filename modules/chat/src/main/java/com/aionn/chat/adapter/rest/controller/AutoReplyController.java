package com.aionn.chat.adapter.rest.controller;

import com.aionn.chat.adapter.rest.dto.autoreply.request.UpdateAutoReplyRequest;
import com.aionn.chat.adapter.rest.dto.autoreply.response.AutoReplyResponse;
import com.aionn.chat.adapter.rest.mapper.autoreply.AutoReplyDtoMapper;
import com.aionn.chat.application.port.in.autoreply.GetAutoReplyQueryPort;
import com.aionn.chat.application.port.in.autoreply.UpdateAutoReplyInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.aionn.chat.adapter.rest.support.session.CurrentUserId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat/merchants/{merchantId}/auto-reply")
@RequiredArgsConstructor
public class AutoReplyController {

        private final GetAutoReplyQueryPort getAutoReplyQueryPort;
        private final UpdateAutoReplyInputPort updateAutoReplyInputPort;
        private final AutoReplyDtoMapper dtoMapper;

        @GetMapping
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<AutoReplyResponse>> get(
                        @CurrentUserId String userId,
                        @PathVariable String merchantId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(getAutoReplyQueryPort.execute(userId, merchantId)),
                                "Auto-reply config fetched"));
        }

        @PutMapping
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<AutoReplyResponse>> update(
                        @CurrentUserId String userId,
                        @PathVariable String merchantId,
                        @RequestBody UpdateAutoReplyRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(updateAutoReplyInputPort.execute(
                                                dtoMapper.toUpdateCommand(userId, merchantId, request))),
                                "Auto-reply config saved"));
        }
}
