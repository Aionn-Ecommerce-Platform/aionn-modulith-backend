package com.aionn.identity.application.mapper;

import com.aionn.identity.application.dto.address.result.AddressResult;
import com.aionn.identity.domain.model.Address;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AddressResultMapper {
    AddressResult toResult(Address address);
}



