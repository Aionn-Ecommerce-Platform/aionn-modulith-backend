package com.aionn.catalog.application.port.out.brand;

import com.aionn.catalog.domain.model.Brand;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;

import java.util.List;
import java.util.Optional;

public interface BrandPersistencePort {

    Brand save(Brand brand);

    Optional<Brand> findById(String brandId);

    boolean existsByName(String name);

    List<Brand> list(OffsetPagination pagination);

    long count();
}
