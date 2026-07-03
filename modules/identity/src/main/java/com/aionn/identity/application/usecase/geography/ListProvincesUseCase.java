package com.aionn.identity.application.usecase.geography;

import com.aionn.identity.application.dto.geography.result.GeographyResult;
import com.aionn.identity.application.port.in.geography.ListProvincesQueryPort;
import com.aionn.identity.application.service.GeographyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListProvincesUseCase implements ListProvincesQueryPort {

    private final GeographyService geographyService;

    @Override
    @Transactional(readOnly = true)
    public List<GeographyResult> execute(String countryCode) {
        return geographyService.listProvinces(countryCode);
    }
}
