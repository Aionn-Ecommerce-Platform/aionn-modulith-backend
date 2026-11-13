package com.aionn.catalog.adapter.rest.mapper.question;

import com.aionn.catalog.adapter.rest.dto.question.response.ProductQuestionResponse;
import com.aionn.catalog.application.dto.question.result.ProductQuestionResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductQuestionDtoMapper {

    ProductQuestionResponse toResponse(ProductQuestionResult result);

    List<ProductQuestionResponse> toResponses(List<ProductQuestionResult> results);
}
