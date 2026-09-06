package com.aionn.recommendation.infrastructure.listener;

import com.aionn.recommendation.application.service.InteractionRetentionService;
import com.aionn.sharedkernel.integration.event.identity.AccountDeletedIntegrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Erases a deleted account's behavioural data.
 *
 * <p>Identity retains the opaque user ID for historical business records, but browsing and purchase
 * signals are personal data with no such obligation behind them, so they are removed rather than
 * tombstoned.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountLifecycleListener {

    private final InteractionRetentionService retentionService;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAccountDeleted(AccountDeletedIntegrationEvent event) {
        retentionService.eraseUser(event.userId());
    }
}
