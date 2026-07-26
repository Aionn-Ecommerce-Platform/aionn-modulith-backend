package com.aionn.promotion.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PromotionErrorCode {
    CAMPAIGN_NOT_FOUND("PRM_001", "Promotion campaign not found"),
    CAMPAIGN_INVALID_STATE("PRM_002", "Campaign is not in a state that allows this action"),
    CAMPAIGN_OUT_OF_BUDGET("PRM_003", "Campaign budget exhausted"),
    CAMPAIGN_NOT_RUNNING("PRM_004", "Campaign is not currently running"),

    VOUCHER_NOT_FOUND("PRM_101", "Voucher not found"),
    VOUCHER_EXPIRED("PRM_102", "Voucher has expired"),
    VOUCHER_NO_USAGE_LEFT("PRM_103", "Voucher usage limit reached"),
    VOUCHER_DUPLICATE_CODE("PRM_104", "Voucher code already exists"),
    VOUCHER_WRONG_SHOP("PRM_105", "Shop voucher is not valid for this merchant"),
    MERCHANT_NOT_FOUND("PRM_106", "Merchant profile not found for the current user"),

    USER_VOUCHER_ALREADY_CLAIMED("PRM_201", "User has already claimed this voucher"),
    USER_VOUCHER_LIMIT_REACHED("PRM_202", "User claim limit reached"),
    USER_VOUCHER_NOT_FOUND("PRM_203", "User voucher claim not found"),
    USER_VOUCHER_INVALID_STATE("PRM_204", "User voucher is not in a state that allows this action"),
    USER_VOUCHER_RESERVED_BY_OTHER("PRM_205", "Voucher is currently reserved for a different order"),

    CONDITION_NOT_MET("PRM_301", "Promotion conditions are not met"),
    CONDITION_INVALID("PRM_302", "Invalid promotion condition"),

    FLASH_SALE_NOT_FOUND("PRM_401", "Flash-sale registration not found"),
    FLASH_SALE_DUPLICATE("PRM_402", "SKU already registered in this campaign"),
    FLASH_SALE_INVALID_CAMPAIGN("PRM_403", "Campaign is not a FLASH_SALE type"),
    FLASH_SALE_FORBIDDEN("PRM_404", "Caller is not allowed to act on this registration"),

    BANNER_NOT_FOUND("PRM_501", "Promotion banner not found"),

    INVALID_ARGUMENT("PRM_900", "Invalid argument");

    private final String code;
    private final String defaultMessage;
}
