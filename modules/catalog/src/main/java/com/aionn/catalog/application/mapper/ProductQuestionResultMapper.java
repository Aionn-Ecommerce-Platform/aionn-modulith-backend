package com.aionn.catalog.application.mapper;

import com.aionn.catalog.application.dto.question.result.ProductQuestionResult;
import com.aionn.catalog.application.port.out.merchant.ProductQuestionPersistencePort.Question;
import com.aionn.contracts.port.identity.UserProfileLookupPort;
import com.aionn.contracts.port.identity.UserProfileLookupPort.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductQuestionResultMapper {

    private final UserProfileLookupPort userProfileLookupPort;

    public ProductQuestionResult toResult(Question question) {
        return toResult(question, profileOf(question.userId()));
    }

    public List<ProductQuestionResult> toResults(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return List.of();
        }
        Set<String> userIds = questions.stream()
                .map(Question::userId)
                .filter(Objects::nonNull)
                .filter(userId -> !userId.isBlank())
                .collect(Collectors.toSet());
        Map<String, UserProfile> profiles = userIds.isEmpty()
                ? Map.of()
                : userProfileLookupPort.findProfiles(userIds);
        return questions.stream()
                .map(question -> toResult(question, profiles.get(question.userId())))
                .toList();
    }

    private UserProfile profileOf(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        return userProfileLookupPort.findProfiles(Set.of(userId)).get(userId);
    }

    private static ProductQuestionResult toResult(Question question, UserProfile profile) {
        return new ProductQuestionResult(
                question.questionId(),
                question.productId(),
                question.userId(),
                question.question(),
                question.answer(),
                question.answeredBy(),
                question.answeredAt(),
                question.createdAt(),
                profile == null ? null : profile.username(),
                profile == null ? null : profile.displayName(),
                profile == null ? null : profile.avatarUrl());
    }
}
