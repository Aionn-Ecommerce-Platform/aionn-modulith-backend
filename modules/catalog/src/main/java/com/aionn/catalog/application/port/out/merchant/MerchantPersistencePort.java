package com.aionn.catalog.application.port.out.merchant;

import com.aionn.catalog.domain.model.Merchant;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;

import java.util.List;
import java.util.Optional;

public interface MerchantPersistencePort {

    Merchant save(Merchant merchant);

    Optional<Merchant> findById(String merchantId);

    Optional<Merchant> findByOwnerId(String ownerId);

    boolean existsByOwnerId(String ownerId);

    List<Merchant> list(OffsetPagination pagination);
}

