package com.aionn.ucp.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * JPA entity persisting a UCP identity link — the durable binding between a
 * platform's user (platformId + platformSubject) and an Aionn customer. Durable
 * counterpart to the in-memory adapter so links survive restart/scale-out.
 */
@Entity
@Table(name = "ucp_identity_links", indexes = {
        @Index(name = "idx_ucp_identity_links_customer", columnList = "customer_id")
}, uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(
                name = "uq_ucp_identity_links_platform_subject",
                columnNames = { "platform_id", "platform_subject" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UcpIdentityLinkEntity {

    @Id
    @Column(name = "link_id", nullable = false, length = 80)
    private String linkId;

    @Column(name = "platform_id", nullable = false, length = 120)
    private String platformId;

    @Column(name = "platform_subject", nullable = false, length = 200)
    private String platformSubject;

    @Column(name = "customer_id", nullable = false, length = 120)
    private String customerId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scopes", columnDefinition = "jsonb")
    private List<String> scopes;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
