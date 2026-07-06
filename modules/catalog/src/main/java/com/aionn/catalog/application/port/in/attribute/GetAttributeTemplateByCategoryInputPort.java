package com.aionn.catalog.application.port.in.attribute;

import com.aionn.catalog.application.dto.attribute.query.GetAttributeTemplateByCategoryQuery;
import com.aionn.catalog.application.dto.attribute.result.AttributeTemplateResult;

public interface GetAttributeTemplateByCategoryInputPort {

    AttributeTemplateResult execute(GetAttributeTemplateByCategoryQuery query);
}
