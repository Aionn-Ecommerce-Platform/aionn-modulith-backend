package com.aionn.catalog.application.port.out.merchant;

import com.aionn.catalog.domain.model.Merchant;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MerchantPersistencePort {

    Merchant save(Merchant merchant);

    Optional<Merchant> findById(String merchantId);

    /** Bulk lookup keyed by merchant id; unknown ids are absent. */
    Map<String, Merchant> findByIds(Collection<String> merchantIds);

    Optional<Merchant> findByOwnerId(String ownerId);

    boolean existsByOwnerId(String ownerId);

    List<Merchant> list(OffsetPagination pagination);

    long count();
}
