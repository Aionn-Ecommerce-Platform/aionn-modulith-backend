package com.aionn.inventory.domain.valueobject;

/**
 * Reason category for stock movements. Drives how PhysicalQty / AvailableQty
 * shift in {@link com.aionn.inventory.domain.model.InventoryItem}:
 *
 * <ul>
 * <li>{@link #MANUAL_INCREASE}/{@link #MANUAL_DECREASE} - merchant
 * correction.</li>
 * <li>{@link #DAMAGED} - physical drops, available drops.</li>
 * <li>{@link #OUTBOUND} - generated automatically when a reservation commits;
 * physical drops, available unchanged (was reserved already).</li>
 * <li>{@link #TRANSFER_OUT}/{@link #TRANSFER_IN} - emitted by stock transfer
 * commands so audit ledger is symmetric with what the InventoryItem
 * applies.</li>
 * </ul>
 */
public enum AdjustmentType {
    MANUAL_INCREASE,
    MANUAL_DECREASE,
    DAMAGED,
    OUTBOUND,
    TRANSFER_OUT,
    TRANSFER_IN
}

