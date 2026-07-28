package com.aionn.chat.adapter.rest.controller;

import com.aionn.chat.adapter.rest.dto.block.request.BlockUserRequest;
import com.aionn.chat.adapter.rest.dto.block.response.BlockResponse;
import com.aionn.chat.adapter.rest.mapper.block.BlockDtoMapper;
import com.aionn.chat.application.port.in.block.BlockUserInputPort;
import com.aionn.chat.application.port.in.block.ListMyBlocksQueryPort;
import com.aionn.chat.application.port.in.block.UnblockUserInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.aionn.chat.adapter.rest.support.session.CurrentUserId;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat/blocks")
@RequiredArgsConstructor
public class UserBlockController {

        private final BlockUserInputPort blockUserInputPort;
        private final UnblockUserInputPort unblockUserInputPort;
        private final ListMyBlocksQueryPort listMyBlocksQueryPort;
        private final BlockDtoMapper dtoMapper;

        @PostMapping
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<BlockResponse>> block(
                        @CurrentUserId String userId,
                        @Valid @RequestBody BlockUserRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(blockUserInputPort.execute(
                                                dtoMapper.toBlockCommand(userId, request))),
                                "User blocked"));
        }

        @DeleteMapping("/{blockedId}")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<BlockResponse>> unblock(
                        @CurrentUserId String userId,
                        @PathVariable String blockedId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(unblockUserInputPort.execute(
                                                dtoMapper.toUnblockCommand(userId, blockedId))),
                                "User unblocked"));
        }

        @GetMapping
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<List<BlockResponse>>> listMyBlocks(@CurrentUserId String userId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponses(listMyBlocksQueryPort.execute(userId)),
                                "Blocks fetched"));
        }
}
