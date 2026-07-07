package com.aionn.catalog.application.port.in.attribute;

import com.aionn.catalog.application.dto.attribute.query.GetAttributeTemplateQuery;
import com.aionn.catalog.application.dto.attribute.result.AttributeTemplateResult;

public interface GetAttributeTemplateInputPort {

    AttributeTemplateResult execute(GetAttributeTemplateQuery query);
}
