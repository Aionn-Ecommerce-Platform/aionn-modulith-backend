package com.aionn.identity.domain.valueobject;

public enum SecurityAuditEventType {

    PASSWORD_CHANGED("PASSWORD_CHANGED", "User changed password"),
    PASSWORD_RESET_REQUESTED("PASSWORD_RESET_REQUESTED", "User requested password reset"),
    PASSWORD_RESET_COMPLETED("PASSWORD_RESET_COMPLETED", "Password reset completed with token"),
    MFA_SETUP_INITIATED("MFA_SETUP_INITIATED", "MFA setup initiated"),
    MFA_ENABLED("MFA_ENABLED", "MFA enabled"),
    MFA_DISABLED("MFA_DISABLED", "MFA disabled"),
    MFA_BACKUP_CODES_REGENERATED("MFA_BACKUP_CODES_REGENERATED", "Backup codes regenerated");

    private final String eventType;
    private final String description;

    SecurityAuditEventType(String eventType, String description) {
        this.eventType = eventType;
        this.description = description;
    }

    public String eventType() {
        return eventType;
    }

    public String description() {
        return description;
    }
}
