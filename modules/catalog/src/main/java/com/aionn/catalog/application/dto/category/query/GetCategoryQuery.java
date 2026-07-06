package com.aionn.catalog.application.dto.category.query;

import com.aionn.sharedkernel.application.query.Query;

public record GetCategoryQuery(String categoryId) implements Query {
}
