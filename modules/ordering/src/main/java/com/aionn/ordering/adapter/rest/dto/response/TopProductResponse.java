package com.aionn.ordering.adapter.rest.dto.response;

import java.math.BigDecimal;

public record TopProductResponse(String skuId, long unitsSold, BigDecimal revenue) {}
