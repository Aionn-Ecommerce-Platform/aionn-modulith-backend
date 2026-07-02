package com.aionn.identity.application.port.in.geography;

import com.aionn.identity.application.dto.geography.result.GeographyResult;

import java.util.List;

public interface ListWardsQueryPort {

    List<GeographyResult> execute(String districtCode);
}
