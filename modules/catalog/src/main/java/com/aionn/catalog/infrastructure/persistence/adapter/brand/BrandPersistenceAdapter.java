package com.aionn.catalog.infrastructure.persistence.adapter.brand;

import com.aionn.catalog.application.port.out.brand.BrandPersistencePort;
import com.aionn.catalog.domain.model.Brand;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.catalog.infrastructure.persistence.mapper.BrandDomainMapper;
import com.aionn.catalog.infrastructure.persistence.repository.brand.BrandRepository;
import com.aionn.catalog.infrastructure.persistence.repository.product.ProductRepository;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BrandPersistenceAdapter implements BrandPersistencePort {

    private final BrandRepository jpa;
    private final ProductRepository productJpa;
    private final BrandDomainMapper mapper;

    @Override
    public Brand save(Brand brand) {
        var saved = jpa.save(mapper.toEntity(brand));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Brand> findById(String brandId) {
        return jpa.findById(brandId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return jpa.existsByNameIgnoreCase(name);
    }

    @Override
    public boolean hasActiveProducts(String brandId) {
        return productJpa.existsByBrandIdAndStatus(brandId, ProductStatus.PUBLISHED.name());
    }

    @Override
    public List<Brand> list(OffsetPagination pagination) {
        return jpa.findAll(PageRequest.of(pagination.page(), pagination.size(),
                Sort.by("createdAt").descending().and(Sort.by("brandId").ascending())))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return jpa.count();
    }
}
