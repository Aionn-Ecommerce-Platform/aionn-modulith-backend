package com.aionn.identity.adapter.rest.mapper.feedback;

import com.aionn.identity.adapter.rest.dto.feedback.request.AdminChangeFeedbackStatusRequest;
import com.aionn.identity.adapter.rest.dto.feedback.request.AdminReplyFeedbackRequest;
import com.aionn.identity.adapter.rest.dto.feedback.request.SubmitFeedbackRequest;
import com.aionn.identity.adapter.rest.dto.feedback.response.FeedbackResponse;
import com.aionn.identity.application.dto.common.PageResult;
import com.aionn.identity.application.dto.feedback.command.AdminFeedbackCommands;
import com.aionn.identity.application.dto.feedback.command.SubmitFeedbackCommand;
import com.aionn.identity.application.dto.feedback.result.FeedbackResult;
import com.aionn.sharedkernel.adapter.web.response.PageMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FeedbackDtoMapper {

    @Mapping(target = "userId", source = "userId")
    SubmitFeedbackCommand toSubmitCommand(String userId, SubmitFeedbackRequest request);

    @Mapping(target = "feedbackId", source = "feedbackId")
    @Mapping(target = "adminId", source = "adminId")
    @Mapping(target = "reply", source = "request.reply")
    @Mapping(target = "newStatus", source = "request.newStatus")
    AdminFeedbackCommands.ReplyFeedback toReplyCommand(
            String feedbackId,
            String adminId,
            AdminReplyFeedbackRequest request);

    @Mapping(target = "feedbackId", source = "feedbackId")
    @Mapping(target = "adminId", source = "adminId")
    @Mapping(target = "status", source = "request.status")
    AdminFeedbackCommands.ChangeFeedbackStatus toChangeStatusCommand(
            String feedbackId,
            String adminId,
            AdminChangeFeedbackStatusRequest request);

    FeedbackResponse toResponse(FeedbackResult result);

    List<FeedbackResponse> toResponses(List<FeedbackResult> results);

    default PageMetadata toPageMetadata(PageResult<?> result) {
        return new PageMetadata(
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }
}
