package com.aionn.identity.application.mapper;

import com.aionn.identity.application.dto.kyc.result.KycResult;
import com.aionn.identity.domain.model.KycProfile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface KycResultMapper {

    KycResult toResult(KycProfile domain);
}
