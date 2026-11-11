package com.aionn.contracts.port.notification;

/**
 * Read-only view of the simple notification toggles a customer controls in account settings.
 * The notification module combines this with its per-category/per-channel subscription so the
 * settings screen has a real effect on delivery.
 */
public interface NotificationPreferenceQueryPort {

    NotificationPreferenceSnapshot find(String userId);

    record NotificationPreferenceSnapshot(
            boolean orderUpdatesEnabled,
            boolean promotionsEnabled,
            boolean chatEnabled,
            boolean systemEnabled) {

        public static NotificationPreferenceSnapshot defaults() {
            return new NotificationPreferenceSnapshot(true, true, true, true);
        }
    }
}
