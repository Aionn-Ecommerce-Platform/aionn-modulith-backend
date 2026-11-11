package com.aionn.identity.application.port.out.user;

import com.aionn.identity.application.dto.user.view.DeletionRequestView;

import java.time.Instant;
import java.util.Optional;

public interface AccountDeletionPort {

    DeletionRequestView save(String userId, Instant scheduledDeletionAt);

    Optional<DeletionRequestView> findPendingByUserId(String userId);

    /**
     * Cancels the pending request under a row lock.
     *
     * @return {@code true} when a pending request was cancelled, {@code false} when the
     *         processor already claimed/completed it
     */
    boolean cancel(String userId);
}
