package com.aionn.catalog.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CatalogErrorCode {
    // Merchant
    MERCHANT_NOT_FOUND("CATALOG_001", "Merchant not found"),
    MERCHANT_ALREADY_EXISTS("CATALOG_002", "Merchant already registered for this owner"),
    MERCHANT_INVALID_TRANSITION("CATALOG_003", "Cannot transition merchant to that state"),
    MERCHANT_HAS_OPEN_ORDERS("CATALOG_004", "Cannot close merchant while orders are open"),
    MERCHANT_FORBIDDEN("CATALOG_005", "Merchant action not allowed for current user"),

    // Category
    CATEGORY_NOT_FOUND("CATALOG_101", "Category not found"),
    CATEGORY_NAME_CONFLICT("CATALOG_102", "Category name must be unique within parent"),
    CATEGORY_SLUG_CONFLICT("CATALOG_103", "Category slug must be unique"),
    CATEGORY_HAS_PRODUCTS("CATALOG_104", "Category still has products assigned"),
    CATEGORY_CYCLE("CATALOG_105", "Category move would create a cycle"),

    // Brand
    BRAND_NOT_FOUND("CATALOG_201", "Brand not found"),
    BRAND_NAME_CONFLICT("CATALOG_202", "Brand name must be unique"),
    BRAND_HAS_ACTIVE_PRODUCTS("CATALOG_203", "Brand still has active products; cannot delete"),

    // Product
    PRODUCT_NOT_FOUND("CATALOG_301", "Product not found"),
    PRODUCT_INVALID_TRANSITION("CATALOG_302", "Invalid product status transition"),
    PRODUCT_FORBIDDEN("CATALOG_303", "Product does not belong to this merchant"),
    PRODUCT_PUBLISH_REQUIREMENTS("CATALOG_304", "Product missing required fields for publish"),
    PRODUCT_VARIANT_NOT_FOUND("CATALOG_305", "Product variant (SKU) not found"),
    PRODUCT_VARIANT_DUPLICATE("CATALOG_306", "Variant attribute combination already exists"),
    PRODUCT_BRAND_NOT_APPROVED("CATALOG_307", "Brand has not been approved for use"),
    PRODUCT_CATEGORY_REQUIRED("CATALOG_308", "Product must belong to at least one category"),
    PRODUCT_BULK_TOO_LARGE("CATALOG_309", "Bulk price update exceeds the allowed batch size"),

    // Attribute templates
    ATTRIBUTE_TEMPLATE_NOT_FOUND("CATALOG_401", "Attribute template not found"),
    ATTRIBUTE_KEY_NOT_FOUND("CATALOG_402", "Attribute key not declared on template"),

    // Product Reviews
    REVIEW_NOT_FOUND("CATALOG_501", "Product review not found"),
    REVIEW_ALREADY_EXISTS("CATALOG_502", "User has already reviewed this product"),
    REVIEW_NOT_PURCHASED("CATALOG_503", "Only verified purchasers can review this product"),
    REVIEW_FORBIDDEN("CATALOG_504", "Action on review is not allowed for current user"),
    REVIEW_INVALID_RATING("CATALOG_505", "Rating must be between 1 and 5 stars"),
    REVIEW_ALREADY_REPORTED("CATALOG_506", "This review has already been reported"),
    REVIEW_NOT_REPORTED("CATALOG_507", "Review is not in REPORTED status"),

    // Generic
    INVALID_ARGUMENT("CATALOG_900", "Invalid argument");

    private final String code;
    private final String defaultMessage;
}
