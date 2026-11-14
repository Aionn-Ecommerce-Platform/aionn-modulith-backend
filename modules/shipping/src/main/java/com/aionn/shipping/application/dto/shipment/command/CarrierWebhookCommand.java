package com.aionn.shipping.application.dto.shipment.command;

import com.aionn.sharedkernel.application.command.Command;
import java.util.Arrays;
import java.util.Objects;

public record CarrierWebhookCommand(
        String trackingCode,
        String type,
        String currentLocation,
        String statusDesc,
        String shipperName,
        String shipperPhone,
        String signatureUrl,
        String reason,
        String warehouseId,
        String webhookSecret,
        byte[] rawPayload,
        String signature,
        String timestamp,
        String eventId) implements Command {

    public CarrierWebhookCommand(String trackingCode, String type, String currentLocation,
            String statusDesc, String shipperName, String shipperPhone, String signatureUrl,
            String reason, String warehouseId, String webhookSecret) {
        this(trackingCode, type, currentLocation, statusDesc, shipperName, shipperPhone,
                signatureUrl, reason, warehouseId, webhookSecret, null, null, null, null);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CarrierWebhookCommand that)) {
            return false;
        }
        return Objects.equals(trackingCode, that.trackingCode)
                && Objects.equals(type, that.type)
                && Objects.equals(currentLocation, that.currentLocation)
                && Objects.equals(statusDesc, that.statusDesc)
                && Objects.equals(shipperName, that.shipperName)
                && Objects.equals(shipperPhone, that.shipperPhone)
                && Objects.equals(signatureUrl, that.signatureUrl)
                && Objects.equals(reason, that.reason)
                && Objects.equals(warehouseId, that.warehouseId)
                && Objects.equals(webhookSecret, that.webhookSecret)
                && Arrays.equals(rawPayload, that.rawPayload)
                && Objects.equals(signature, that.signature)
                && Objects.equals(timestamp, that.timestamp)
                && Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(trackingCode, type, currentLocation, statusDesc, shipperName,
                shipperPhone, signatureUrl, reason, warehouseId, webhookSecret, signature,
                timestamp, eventId);
        return 31 * result + Arrays.hashCode(rawPayload);
    }

    @Override
    public String toString() {
        return "CarrierWebhookCommand[trackingCode=" + trackingCode
                + ", type=" + type
                + ", currentLocation=" + currentLocation
                + ", statusDesc=" + statusDesc
                + ", shipperName=" + shipperName
                + ", shipperPhone=" + shipperPhone
                + ", signatureUrl=" + signatureUrl
                + ", reason=" + reason
                + ", warehouseId=" + warehouseId
                + ", webhookSecret=[redacted]"
                + ", rawPayload=" + Arrays.toString(rawPayload)
                + ", signature=" + signature
                + ", timestamp=" + timestamp
                + ", eventId=" + eventId
                + "]";
    }
}
