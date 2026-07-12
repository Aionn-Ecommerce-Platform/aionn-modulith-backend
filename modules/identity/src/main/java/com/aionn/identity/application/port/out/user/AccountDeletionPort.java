package com.aionn.identity.application.port.out.user;

import com.aionn.identity.application.dto.user.view.DeletionRequestView;

import java.time.Instant;
import java.util.Optional;

public interface AccountDeletionPort {

    DeletionRequestView save(String userId, Instant scheduledDeletionAt);

    Optional<DeletionRequestView> findPendingByUserId(String userId);

    void cancel(String userId);
}
