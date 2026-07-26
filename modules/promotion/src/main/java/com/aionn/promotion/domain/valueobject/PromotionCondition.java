package com.aionn.promotion.domain.valueobject;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record PromotionCondition(
        BigDecimal minOrderValue,
        List<String> applicableCategoryIds,
        Integer maxClaimsPerUser,
        Integer maxUsesPerVoucher) {

    public PromotionCondition {
        applicableCategoryIds = applicableCategoryIds == null ? List.of() : List.copyOf(applicableCategoryIds);
    }

    public static PromotionCondition empty() {
        return new PromotionCondition(null, List.of(), null, null);
    }

    public boolean matches(BigDecimal orderValue, List<String> orderCategoryIds) {
        if (minOrderValue != null && orderValue.compareTo(minOrderValue) < 0) {
            return false;
        }
        if (!applicableCategoryIds.isEmpty()) {
            List<String> orderCats = orderCategoryIds == null ? Collections.emptyList() : orderCategoryIds;
            boolean intersects = applicableCategoryIds.stream().anyMatch(orderCats::contains);
            if (!intersects)
                return false;
        }
        return true;
    }

    public PromotionCondition withMin(BigDecimal min) {
        return new PromotionCondition(min, applicableCategoryIds, maxClaimsPerUser, maxUsesPerVoucher);
    }

    public PromotionCondition withCategories(List<String> categories) {
        return new PromotionCondition(minOrderValue, categories, maxClaimsPerUser, maxUsesPerVoucher);
    }

    public PromotionCondition withMaxClaimsPerUser(Integer max) {
        return new PromotionCondition(minOrderValue, applicableCategoryIds, max, maxUsesPerVoucher);
    }

    public PromotionCondition withMaxUsesPerVoucher(Integer max) {
        return new PromotionCondition(minOrderValue, applicableCategoryIds, maxClaimsPerUser, max);
    }

    public boolean hasMinOrderValue() {
        return minOrderValue != null && minOrderValue.signum() > 0;
    }

    public boolean hasCategories() {
        return !applicableCategoryIds.isEmpty();
    }

    public Integer maxClaimsPerUserOrNull() {
        return maxClaimsPerUser;
    }

    public Integer maxUsesPerVoucherOrNull() {
        return maxUsesPerVoucher;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PromotionCondition that))
            return false;
        return Objects.equals(minOrderValue, that.minOrderValue)
                && Objects.equals(applicableCategoryIds, that.applicableCategoryIds)
                && Objects.equals(maxClaimsPerUser, that.maxClaimsPerUser)
                && Objects.equals(maxUsesPerVoucher, that.maxUsesPerVoucher);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minOrderValue, applicableCategoryIds, maxClaimsPerUser, maxUsesPerVoucher);
    }
}

