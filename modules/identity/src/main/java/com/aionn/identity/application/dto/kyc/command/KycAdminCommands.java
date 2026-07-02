package com.aionn.identity.application.dto.kyc.command;

public final class KycAdminCommands {

    private KycAdminCommands() {
    }

    public record ApproveKyc(String kycId, String adminId, String note) {
    }

    public record RejectKyc(String kycId, String adminId, String reason) {
    }

    public record MarkInReviewKyc(String kycId, String adminId, String note) {
    }
}
