package com.aionn.inventory.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InventoryErrorCode {
    // Warehouse
    WAREHOUSE_NOT_FOUND("INV_001", "Warehouse not found"),
    WAREHOUSE_FORBIDDEN("INV_002", "Warehouse does not belong to this merchant"),
    WAREHOUSE_INVALID_TRANSITION("INV_003", "Invalid warehouse status transition"),

    // Inventory items
    INVENTORY_ITEM_NOT_FOUND("INV_101", "Inventory item not found"),
    INVENTORY_ALREADY_INITIALIZED("INV_102", "Inventory already initialized for this SKU/warehouse"),
    INVENTORY_INSUFFICIENT_STOCK("INV_103", "Available quantity is below the requested amount"),
    INVENTORY_NEGATIVE_PHYSICAL_STOCK("INV_104", "Physical quantity cannot go negative"),
    INVENTORY_LOCKED("INV_105", "Inventory item is emergency-locked"),
    INVENTORY_EXPIRY_INVALID("INV_106", "Expiry date must be after creation"),
    INVENTORY_AUDIT_NEGATIVE("INV_107", "Actual quantity in audit cannot be negative"),

    // Stock transfers
    STOCK_TRANSFER_NOT_FOUND("INV_201", "Stock transfer not found"),
    STOCK_TRANSFER_INVALID("INV_202", "Stock transfer is not in a state that allows this action"),
    STOCK_TRANSFER_SAME_WAREHOUSE("INV_203", "Source and target warehouse cannot be the same"),
    STOCK_TRANSFER_DIFFERENT_MERCHANT("INV_204", "Both warehouses must belong to the same merchant"),

    // Reservations
    STOCK_RESERVATION_NOT_FOUND("INV_301", "Stock reservation not found"),
    STOCK_RESERVATION_INVALID_STATE("INV_302", "Reservation is not in a state that allows this action"),

    // Adjustments
    STOCK_ADJUSTMENT_INVALID("INV_401", "Stock adjustment is invalid"),

    // Generic
    INVALID_ARGUMENT("INV_900", "Invalid argument");

    private final String code;
    private final String defaultMessage;
}

