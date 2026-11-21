package com.aionn.shipping.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "shipment_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentItemEntity {

    @EmbeddedId
    private ShipmentItemId id;

    @Column(name = "order_id", length = 50, nullable = false)
    private String orderId;

    @Column(name = "qty", nullable = false)
    private int qty;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @EqualsAndHashCode
    public static class ShipmentItemId implements Serializable {
        @Column(name = "shipment_id", length = 50)
        private String shipmentId;

        @Column(name = "sku_id", length = 50)
        private String skuId;
    }
}
