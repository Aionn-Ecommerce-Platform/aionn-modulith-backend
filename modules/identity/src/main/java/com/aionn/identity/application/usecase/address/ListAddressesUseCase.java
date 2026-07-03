package com.aionn.identity.application.usecase.address;

import com.aionn.identity.application.dto.address.result.AddressResult;
import com.aionn.identity.application.mapper.AddressResultMapper;
import com.aionn.identity.application.port.in.address.ListAddressesQueryPort;
import com.aionn.identity.application.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListAddressesUseCase implements ListAddressesQueryPort {

    private final AddressService addressService;
    private final AddressResultMapper addressResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResult> execute(String userId) {
        return addressService.listAddressesByUserId(userId).stream()
                .map(addressResultMapper::toResult)
                .toList();
    }
}

