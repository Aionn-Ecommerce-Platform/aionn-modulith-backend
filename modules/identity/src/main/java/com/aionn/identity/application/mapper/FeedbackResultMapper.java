package com.aionn.identity.application.mapper;

import com.aionn.identity.application.dto.feedback.result.FeedbackResult;
import com.aionn.identity.domain.model.Feedback;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FeedbackResultMapper {

    @Mapping(target = "category", expression = "java(feedback.getCategory().name())")
    @Mapping(target = "status", expression = "java(feedback.getStatus().name())")
    @Mapping(target = "rating", expression = "java(feedback.getRating() == null ? null : feedback.getRating().intValue())")
    FeedbackResult toResult(Feedback feedback);
}
