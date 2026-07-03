package com.aionn.identity.application.usecase.geography;

import com.aionn.identity.application.dto.geography.result.GeographyResult;
import com.aionn.identity.application.port.in.geography.GetWardQueryPort;
import com.aionn.identity.application.service.GeographyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetWardUseCase implements GetWardQueryPort {

    private final GeographyService geographyService;

    @Override
    @Transactional(readOnly = true)
    public GeographyResult execute(String code) {
        return geographyService.getWard(code);
    }
}
