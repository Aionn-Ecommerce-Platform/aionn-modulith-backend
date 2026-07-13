package com.aionn.inventory.domain.exception;

import com.aionn.sharedkernel.common.exception.DomainException;

public class InventoryException extends DomainException {

    public InventoryException(InventoryErrorCode errorCode) {
        super("Inventory", errorCode.getCode(), errorCode.getDefaultMessage());
    }

    public InventoryException(InventoryErrorCode errorCode, String customMessage) {
        super("Inventory", errorCode.getCode(), customMessage);
    }
}

