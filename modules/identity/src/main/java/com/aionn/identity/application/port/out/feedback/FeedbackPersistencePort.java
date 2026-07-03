package com.aionn.identity.application.port.out.feedback;

import com.aionn.identity.application.dto.common.PageResult;
import com.aionn.identity.domain.model.Feedback;
import com.aionn.identity.domain.valueobject.FeedbackStatus;

import java.util.List;
import java.util.Optional;

public interface FeedbackPersistencePort {

    Feedback save(Feedback feedback);

    Optional<Feedback> findById(String feedbackId);

    List<Feedback> findByUserId(String userId);

    PageResult<Feedback> findAdminPage(FeedbackStatus status, int page, int size);
}
