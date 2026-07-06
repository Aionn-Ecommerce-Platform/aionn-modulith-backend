package com.aionn.catalog.domain.exception;

import com.aionn.sharedkernel.common.exception.DomainException;

public class CatalogException extends DomainException {

    public CatalogException(CatalogErrorCode errorCode) {
        super("Catalog", errorCode.getCode(), errorCode.getDefaultMessage());
    }

    public CatalogException(CatalogErrorCode errorCode, String customMessage) {
        super("Catalog", errorCode.getCode(), customMessage);
    }
}

