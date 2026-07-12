package com.aionn.identity.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "kyc_documents", indexes = {
        @Index(name = "idx_kyc_documents_kyc_id", columnList = "kyc_id"),
        @Index(name = "idx_kyc_documents_type", columnList = "document_type")
})
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class KycDocumentEntity {

    @Id
    @Column(name = "document_id", nullable = false, length = 26)
    private String documentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kyc_id", nullable = false)
    private KycProfileEntity kyc;

    @Column(name = "document_type", nullable = false, length = 30)
    private String type;

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "public_id", length = 255)
    private String publicId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;
}
