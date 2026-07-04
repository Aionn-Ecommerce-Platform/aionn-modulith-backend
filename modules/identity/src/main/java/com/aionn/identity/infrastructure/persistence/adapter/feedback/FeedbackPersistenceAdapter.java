package com.aionn.identity.infrastructure.persistence.adapter.feedback;

import com.aionn.identity.application.dto.common.PageResult;
import com.aionn.identity.application.port.out.feedback.FeedbackPersistencePort;
import com.aionn.identity.domain.model.Feedback;
import com.aionn.identity.domain.valueobject.FeedbackStatus;
import com.aionn.identity.infrastructure.persistence.mapper.FeedbackDomainMapper;
import com.aionn.identity.infrastructure.persistence.repository.feedback.UserFeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FeedbackPersistenceAdapter implements FeedbackPersistencePort {

    private final UserFeedbackRepository repository;
    private final FeedbackDomainMapper feedbackDomainMapper;

    @Override
    public Feedback save(Feedback feedback) {
        return feedbackDomainMapper.toDomain(repository.save(feedbackDomainMapper.toEntity(feedback)));
    }

    @Override
    public Optional<Feedback> findById(String feedbackId) {
        return repository.findById(feedbackId).map(feedbackDomainMapper::toDomain);
    }

    @Override
    public List<Feedback> findByUserId(String userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(feedbackDomainMapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<Feedback> findAdminPage(FeedbackStatus status, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(0, page);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize);
        var result = status != null
                ? repository.findByStatusOrderByCreatedAtDesc(status, pageRequest)
                : repository.findAllByOrderByCreatedAtDesc(pageRequest);
        return new PageResult<>(
                result.getContent().stream().map(feedbackDomainMapper::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());
    }
}
