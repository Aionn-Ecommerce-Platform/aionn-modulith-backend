package com.aionn.identity.infrastructure.persistence.mapper;

import com.aionn.identity.domain.model.Feedback;
import com.aionn.identity.infrastructure.persistence.entity.UserFeedbackEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FeedbackDomainMapper {

    Feedback toDomain(UserFeedbackEntity entity);

    UserFeedbackEntity toEntity(Feedback feedback);
}
