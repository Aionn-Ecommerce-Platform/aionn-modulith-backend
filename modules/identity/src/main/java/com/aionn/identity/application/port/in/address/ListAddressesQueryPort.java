package com.aionn.identity.application.port.in.address;

import com.aionn.identity.application.dto.address.result.AddressResult;
import java.util.List;

public interface ListAddressesQueryPort {
    List<AddressResult> execute(String userId);
}
