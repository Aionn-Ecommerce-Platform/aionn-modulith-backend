package com.aionn.identity.application.usecase.address;

import com.aionn.identity.application.dto.address.result.AddressResult;
import com.aionn.identity.application.dto.address.command.SetDefaultAddressCommand;
import com.aionn.identity.application.mapper.AddressResultMapper;
import com.aionn.identity.application.port.in.address.SetDefaultAddressInputPort;
import com.aionn.identity.application.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SetDefaultAddressUseCase implements SetDefaultAddressInputPort {

    private final AddressService addressService;
    private final AddressResultMapper addressResultMapper;

    @Override
    @Transactional
    public AddressResult execute(SetDefaultAddressCommand command) {
        var address = addressService.setDefaultAddress(command.userId(), command.addressId());
        return addressResultMapper.toResult(address);
    }
}

