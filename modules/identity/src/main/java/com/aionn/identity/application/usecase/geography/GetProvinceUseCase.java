package com.aionn.identity.application.usecase.geography;

import com.aionn.identity.application.dto.geography.result.GeographyResult;
import com.aionn.identity.application.port.in.geography.GetProvinceQueryPort;
import com.aionn.identity.application.service.GeographyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetProvinceUseCase implements GetProvinceQueryPort {

    private final GeographyService geographyService;

    @Override
    @Transactional(readOnly = true)
    public GeographyResult execute(String code) {
        return geographyService.getProvince(code);
    }
}
