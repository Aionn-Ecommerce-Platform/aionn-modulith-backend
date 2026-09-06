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
 *
 * <p>
 * Interactions are stored with their base weight; decay is applied at read
 * time. Storing a decayed
 * value would freeze it at write time and every row would be wrong within days.
 *
 * <p>
 * Never logs the product a user interacted with above debug level: a person's
 * browsing history can
 * reveal sensitive interests, and it is not needed for operational diagnosis.
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
        Optional<String> productId = resolveProductId(command);
        if (productId.isEmpty()) {
            log.debug("Skipping {} interaction: no product resolved for event {}",
                    command.type(), command.sourceEventId());
            return;
        }

        InteractionType type = command.type();
        UserInteraction interaction = UserInteraction.create(
                IdGenerator.ulid(),
                command.userId(),
                productId.get(),
                type,
                weightPolicy.weightFor(type).baseWeight(),
                command.occurredAt() != null ? command.occurredAt() : clock.instant());

        interactionRepository.append(interaction);
    }

    /**
     * Product-scoped events carry the product directly. SKU-scoped ones (cart,
     * order lines) need a
     * catalog lookup, which is an in-process adapter call rather than external I/O,
     * so it is safe
     * inside this short transaction.
     */
    private Optional<String> resolveProductId(RecordInteractionCommand command) {
        if (command.productId() != null && !command.productId().isBlank()) {
            return Optional.of(command.productId());
        }
        if (command.skuId() == null || command.skuId().isBlank()) {
            return Optional.empty();
        }
        return productAttributeQuery.findProductIdBySkuId(command.skuId());
    }

    /**
     * Spring Security names an unauthenticated principal {@code anonymousUser};
     * treating it as a real
     * user would merge every guest's behaviour into one shared profile.
     */
    private static boolean isRealUser(String userId) {
        return userId != null && !userId.isBlank() && !"anonymousUser".equals(userId);
    }
}
