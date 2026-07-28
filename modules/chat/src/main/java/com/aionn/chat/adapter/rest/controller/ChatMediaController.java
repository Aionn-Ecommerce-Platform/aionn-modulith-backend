package com.aionn.chat.adapter.rest.controller;

import com.aionn.chat.adapter.rest.dto.media.response.UploadSignatureResponse;
import com.aionn.chat.adapter.rest.mapper.media.ChatMediaDtoMapper;
import com.aionn.chat.adapter.rest.support.session.CurrentUserId;
import com.aionn.chat.application.port.in.media.GenerateChatMediaUploadSignatureInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat/media/upload-signatures")
@RequiredArgsConstructor
@Tag(name = "Chat - Media Upload", description = "Chat module: signed upload parameters for image messages")
public class ChatMediaController {

        private final GenerateChatMediaUploadSignatureInputPort generateChatMediaUploadSignatureInputPort;
        private final ChatMediaDtoMapper mediaDtoMapper;

        @PostMapping("/image")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Generate chat-image upload signature", description = "Authenticated users get signed upload params scoped to their userId")
        public ResponseEntity<ApiResponse<UploadSignatureResponse>> generateChatImageSignature(
                        @CurrentUserId String userId) {
                var result = generateChatMediaUploadSignatureInputPort.execute(userId);
                var response = mediaDtoMapper.toUploadSignatureResponse(result);
                return ResponseEntity.ok(ApiResponse.success(response,
                                "Chat image upload signature generated"));
        }
}
