package com.aionn.ordering.domain.valueobject;

/**
 * Classifies why a customer is returning an order. The free-text reason stays
 * on the return itself; this value is what merchant/admin filters and return
 * analytics group by.
 */
public enum ReturnReasonType {
    WRONG_ITEM,
    DAMAGED,
    DEFECTIVE,
    NOT_AS_DESCRIBED,
    MISSING_PARTS,
    WRONG_SIZE,
    LATE_DELIVERY,
    CHANGED_MIND,
    OTHER
}
