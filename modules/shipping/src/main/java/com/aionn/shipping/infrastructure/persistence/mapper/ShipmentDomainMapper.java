package com.aionn.shipping.infrastructure.persistence.mapper;

import com.aionn.shipping.domain.model.Shipment;
import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.shipping.domain.valueobject.ShipmentDimensions;
import com.aionn.shipping.domain.valueobject.ShipmentStatus;
import com.aionn.shipping.infrastructure.persistence.entity.ShipmentEntity;
import org.springframework.stereotype.Component;

@Component
public class ShipmentDomainMapper {

        public Shipment toDomain(ShipmentEntity e) {
                ShipmentAddress address = new ShipmentAddress(
                                e.getToFullName(), e.getToPhone(), e.getToAddressLine(),
                                e.getToWardCode(), e.getToDistrictId(), e.getToProvinceCode(), e.getToCountryCode());
                ShipmentDimensions dimensions = new ShipmentDimensions(
                                e.getWeightGram(), e.getLengthCm(), e.getWidthCm(), e.getHeightCm());
                return new Shipment(
                                e.getShipmentId(), e.getOrderId(), e.getMerchantId(), e.getUserId(),
                                address, dimensions,
                                e.getCodAmount(), e.getShippingFee(), e.getCurrency(),
                                e.getTrackingCode(), e.getCarrierOrderId(), e.getLabelUrl(),
                                e.getCurrentLocation(), e.getShipperName(), e.getShipperPhone(), e.getSignatureUrl(),
                                e.getAttemptCount(), e.getLastFailureReason(), e.getIssueType(), e.getIssueResolution(),
                                e.getExpectedDeliveryDate(),
                                ShipmentStatus.valueOf(e.getStatus()),
                                e.getCreatedAt(), e.getUpdatedAt(), e.getPickedAt(), e.getDeliveredAt(),
                                e.getCancelledAt(), e.getReturnedAt());
        }

        public ShipmentEntity toEntity(Shipment s, ShipmentEntity existing) {
                ShipmentEntity entity = existing != null ? existing
                                : ShipmentEntity.builder()
                                                .shipmentId(s.getShipmentId())
                                                .orderId(s.getOrderId())
                                                .merchantId(s.getMerchantId())
                                                .userId(s.getUserId())
                                                .weightGram(s.getDimensions().weightGram())
                                                .lengthCm(s.getDimensions().lengthCm())
                                                .widthCm(s.getDimensions().widthCm())
                                                .heightCm(s.getDimensions().heightCm())
                                                .toFullName(s.getAddress().fullName())
                                                .toPhone(s.getAddress().phone())
                                                .toAddressLine(s.getAddress().addressLine())
                                                .toWardCode(s.getAddress().wardCode())
                                                .toDistrictId(s.getAddress().districtId())
                                                .toProvinceCode(s.getAddress().provinceCode())
                                                .toCountryCode(s.getAddress().countryCode())
                                                .build();
                entity.setTrackingCode(s.getTrackingCode());
                entity.setCarrierOrderId(s.getCarrierOrderId());
                entity.setLabelUrl(s.getLabelUrl());
                entity.setCodAmount(s.getCodAmount());
                entity.setShippingFee(s.getShippingFee());
                entity.setCurrency(s.getCurrency());
                entity.setCurrentLocation(s.getCurrentLocation());
                entity.setShipperName(s.getShipperName());
                entity.setShipperPhone(s.getShipperPhone());
                entity.setSignatureUrl(s.getSignatureUrl());
                entity.setAttemptCount(s.getAttemptCount());
                entity.setLastFailureReason(s.getLastFailureReason());
                entity.setIssueType(s.getIssueType());
                entity.setIssueResolution(s.getIssueResolution());
                entity.setExpectedDeliveryDate(s.getExpectedDeliveryDate());
                entity.setStatus(s.getStatus().name());
                entity.setPickedAt(s.getPickedAt());
                entity.setDeliveredAt(s.getDeliveredAt());
                entity.setCancelledAt(s.getCancelledAt());
                entity.setReturnedAt(s.getReturnedAt());
                return entity;
        }
}
