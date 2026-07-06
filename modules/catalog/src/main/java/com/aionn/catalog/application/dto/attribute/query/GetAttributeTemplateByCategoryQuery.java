package com.aionn.catalog.application.dto.attribute.query;

import com.aionn.sharedkernel.application.query.Query;

public record GetAttributeTemplateByCategoryQuery(String categoryId) implements Query {
}
