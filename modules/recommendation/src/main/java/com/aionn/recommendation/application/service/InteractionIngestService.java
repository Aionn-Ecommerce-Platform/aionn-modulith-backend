package com.aionn.recommendation.application.service;

import com.aionn.recommendation.application.dto.command.RecordInteractionCommand;
import com.aionn.recommendation.application.policy.InteractionWeightPolicy;
import com.aionn.recommendation.application.port.out.InteractionPersistencePort;
import com.aionn.recommendation.application.port.out.ProductAttributeQueryPort;
import com.aionn.recommendation.domain.model.UserInteraction;
import com.aionn.recommendation.domain.valueobject.InteractionType;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/**
 * Writes behavioural signals into the interaction log.
 * Base weight is stored and decay applied at read time. Never log the interacted product above
 * debug: browsing history can reveal sensitive interests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionIngestService {

    private final InteractionPersistencePort interactionRepository;
    private final ProductAttributeQueryPort productAttributeQuery;
    private final InteractionWeightPolicy weightPolicy;
    private final Clock clock;

    @Transactional
    public void ingestInteraction(RecordInteractionCommand command) {
        if (!isRealUser(command.userId())) {
            return;
        }

        InteractionType type = command.type();
        if (type == InteractionType.SEARCH) {
            ingestSearch(command);
            return;
        }

        Optional<String> productId = resolveProductId(command);
        if (productId.isEmpty()) {
            log.debug("Skipping {} interaction: no product resolved for event {}",
                    command.type(), command.sourceEventId());
            return;
        }

        // Lock, then check erasure under it: outbox delivery is at-least-once, so a deletion event can
        // be replayed after the erasure it caused.
        interactionRepository.lockUser(command.userId());
        if (interactionRepository.isUserErased(command.userId())) {
            log.debug("Skipping {} interaction for an erased account", type);
            return;
        }

        UserInteraction interaction = UserInteraction.create(
                IdGenerator.ulid(),
                command.userId(),
                productId.get(),
                type,
                weightPolicy.weightFor(type).baseWeight(),
                command.occurredAt() != null ? command.occurredAt() : clock.instant(),
                command.sourceEventId());

        interactionRepository.append(interaction);
    }

    /** A search has no product, so it carries the facets the profile builder scores directly. */
    private void ingestSearch(RecordInteractionCommand command) {
        interactionRepository.lockUser(command.userId());
        if (interactionRepository.isUserErased(command.userId())) {
            log.debug("Skipping SEARCH interaction for an erased account");
            return;
        }
        UserInteraction interaction = UserInteraction.createSearch(
                IdGenerator.ulid(),
                command.userId(),
                InteractionType.SEARCH,
                weightPolicy.weightFor(InteractionType.SEARCH).baseWeight(),
                command.occurredAt() != null ? command.occurredAt() : clock.instant(),
                command.sourceEventId(),
                command.categoryIds(),
                command.brandId(),
                command.keyword());
        interactionRepository.append(interaction);
    }

    /** SKU-scoped events need an in-process catalog lookup (not external I/O). */
    private Optional<String> resolveProductId(RecordInteractionCommand command) {
        if (command.productId() != null && !command.productId().isBlank()) {
            return Optional.of(command.productId());
        }
        if (command.skuId() == null || command.skuId().isBlank()) {
            return Optional.empty();
        }
        return productAttributeQuery.findProductIdBySkuId(command.skuId());
    }

    /** Spring Security's anonymous principal must not merge all guests into one profile. */
    private static boolean isRealUser(String userId) {
        return userId != null && !userId.isBlank() && !"anonymousUser".equals(userId);
    }
}
