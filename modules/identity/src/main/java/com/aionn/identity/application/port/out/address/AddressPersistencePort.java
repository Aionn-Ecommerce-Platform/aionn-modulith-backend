package com.aionn.identity.application.port.out.address;

import com.aionn.identity.domain.model.Address;

import java.util.List;
import java.util.Optional;

public interface AddressPersistencePort {

    List<Address> findByUserId(String userId);

    Address createAtomically(Address address, long maxAddressNumbers, boolean makeDefaultWhenFirst);

    Address save(Address address);

    /**
     * Makes the supplied address the only default for its owner. The owner row is locked for
     * the duration so concurrent operations cannot both clear-then-set and leave zero/two
     * defaults (which would also trip the partial unique index).
     */
    Address setDefault(Address address);

    Optional<Address> findByAddressIdAndUserId(String addressId, String userId);

    void delete(Address address);
}
