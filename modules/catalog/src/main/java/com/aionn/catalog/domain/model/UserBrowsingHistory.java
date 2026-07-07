package com.aionn.catalog.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class UserBrowsingHistory {

    private static final int MAX_PREFERENCES = 5;

    private final String userId;
    private final List<String> categoryIds;
    private final List<String> brandIds;

    public static UserBrowsingHistory create(String userId) {
        return new UserBrowsingHistory(userId, new ArrayList<>(), new ArrayList<>());
    }

    public void trackView(List<String> productCategoryIds, String brandId) {
        if (productCategoryIds != null) {
            for (String cat : productCategoryIds) {
                categoryIds.remove(cat);
                categoryIds.add(0, cat);
            }
        }
        if (brandId != null) {
            brandIds.remove(brandId);
            brandIds.add(0, brandId);
        }
        trim(categoryIds);
        trim(brandIds);
    }

    private static void trim(List<String> values) {
        while (values.size() > MAX_PREFERENCES) {
            values.remove(values.size() - 1);
        }
    }
}
