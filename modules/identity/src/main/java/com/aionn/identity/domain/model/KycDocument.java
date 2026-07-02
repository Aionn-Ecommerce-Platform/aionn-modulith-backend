package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.KycDocumentStatus;
import com.aionn.identity.domain.valueobject.KycDocumentType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class KycDocument {
    private final String documentId;
    private final String kycId;
    private final KycDocumentType type;
    private final String url;
    private final String publicId;
    private final KycDocumentStatus status;
    private final LocalDateTime uploadedAt;
}
