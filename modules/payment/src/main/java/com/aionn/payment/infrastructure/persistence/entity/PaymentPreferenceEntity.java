package com.aionn.payment.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "payment_preferences")
@Getter
@Setter
@NoArgsConstructor
public class PaymentPreferenceEntity {

    @Id
    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "payment_type", length = 20, nullable = false)
    private String paymentType;

    @Column(name = "payment_method_id", length = 50)
    private String paymentMethodId;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
