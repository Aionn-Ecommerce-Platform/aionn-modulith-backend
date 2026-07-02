package com.aionn.identity.application.port.in.geography;

import com.aionn.identity.application.dto.geography.result.GeographyResult;

public interface GetProvinceQueryPort {

    GeographyResult execute(String code);
}
