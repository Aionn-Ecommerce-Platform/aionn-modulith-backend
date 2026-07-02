package com.aionn.identity.application.service;

import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.Feedback;
import com.aionn.identity.domain.valueobject.FeedbackCategory;
import com.aionn.identity.domain.valueobject.FeedbackStatus;
import com.aionn.identity.application.dto.common.PageResult;
import com.aionn.identity.application.port.out.feedback.FeedbackPersistencePort;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FeedbackService {

    private final FeedbackPersistencePort feedbackPersistencePort;

    public Feedback submit(String userId, String category, String subject, String content,
            Integer rating, String contactEmail, String contactPhone) {
        Feedback feedback = Feedback.createNew(
                IdGenerator.ulid(),
                userId,
                FeedbackCategory.from(category),
                trimToNull(subject),
                content.trim(),
                rating,
                trimToNull(contactEmail),
                trimToNull(contactPhone));
        Feedback saved = feedbackPersistencePort.save(feedback);
        log.info("Feedback submitted: id={} userId={} category={}", saved.getFeedbackId(), userId, saved.getCategory());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Feedback> listMine(String userId) {
        return feedbackPersistencePort.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public PageResult<Feedback> adminList(FeedbackStatus status, int page, int size) {
        return feedbackPersistencePort.findAdminPage(status, page, size);
    }

    @Transactional(readOnly = true)
    public Feedback adminGet(String feedbackId) {
        return requireFeedback(feedbackId);
    }

    public Feedback adminReply(String feedbackId, String adminId, String reply,
            FeedbackStatus newStatus) {
        Feedback feedback = requireFeedback(feedbackId);
        feedback.reply(adminId, reply, newStatus, LocalDateTime.now());
        return feedbackPersistencePort.save(feedback);
    }

    public Feedback adminChangeStatus(String feedbackId, String adminId, FeedbackStatus newStatus) {
        Feedback feedback = requireFeedback(feedbackId);
        feedback.changeStatus(adminId, newStatus, LocalDateTime.now());
        return feedbackPersistencePort.save(feedback);
    }

    private Feedback requireFeedback(String feedbackId) {
        return feedbackPersistencePort.findById(feedbackId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.FEEDBACK_NOT_FOUND));
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}
