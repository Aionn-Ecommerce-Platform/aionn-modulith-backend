package com.aionn.recommendation.infrastructure.scheduling;

import com.aionn.recommendation.application.service.ProfileRefreshService;
import com.aionn.recommendation.infrastructure.config.properties.RecommendationJobProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Refreshes one user's profile per call.
 *
 * <p>Separate from the scheduler so each refresh commits independently: the call passes through the
 * Spring proxy, which a self-invocation from the scheduler would bypass, leaving one bad user to abort
 * the whole sweep.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileRefreshWorker {

    private final ProfileRefreshService profileRefreshService;
    private final RecommendationJobProperties properties;

    public boolean refreshOne(String userId) {
        try {
            profileRefreshService.refresh(
                    userId,
                    Duration.ofDays(properties.profile().lookbackDays()),
                    properties.profile().maxAffinities());
            return true;
        } catch (RuntimeException exception) {
            log.warn("Profile refresh failed for one user: {}", exception.getMessage());
            return false;
        }
    }
}
