package com.aionn.catalog.application.service;

import com.aionn.catalog.application.dto.brand.command.CreateBrandCommand;
import com.aionn.catalog.application.dto.brand.command.DeleteBrandCommand;
import com.aionn.catalog.application.dto.brand.command.UpdateBrandCommand;
import com.aionn.catalog.application.dto.brand.result.BrandResult;
import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.mapper.BrandResultMapper;
import com.aionn.catalog.application.port.out.brand.BrandPersistencePort;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.model.Brand;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BrandService {

    private final BrandPersistencePort brandRepository;
    private final BrandResultMapper brandResultMapper;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public BrandResult create(CreateBrandCommand command) {
        if (brandRepository.existsByName(command.name())) {
            throw new CatalogException(CatalogErrorCode.BRAND_NAME_CONFLICT);
        }
        Brand brand = Brand.create(IdGenerator.ulid(), command.name(), command.logoUrl(), command.description(), clock);
        Brand saved = brandRepository.save(brand);
        eventPublisher.publish(brand.pullEvents());
        return brandResultMapper.toResult(saved);
    }

    public BrandResult update(UpdateBrandCommand command) {
        Brand brand = required(command.brandId());
        if (command.name() != null
                && !command.name().equalsIgnoreCase(brand.getName())
                && brandRepository.existsByName(command.name())) {
            throw new CatalogException(CatalogErrorCode.BRAND_NAME_CONFLICT);
        }
        brand.update(command.name(), command.logoUrl(), command.description(), clock);
        Brand saved = brandRepository.save(brand);
        eventPublisher.publish(brand.pullEvents());
        return brandResultMapper.toResult(saved);
    }

    public void delete(DeleteBrandCommand command) {
        Brand brand = required(command.brandId());
        if (brandRepository.hasActiveProducts(command.brandId())) {
            throw new CatalogException(CatalogErrorCode.BRAND_HAS_ACTIVE_PRODUCTS);
        }
        brand.softDelete(command.reason(), clock);
        brandRepository.save(brand);
        eventPublisher.publish(brand.pullEvents());
    }

    @Transactional(readOnly = true)
    public PageResult<BrandResult> list(OffsetPagination pagination) {
        List<BrandResult> results = brandRepository.list(pagination).stream()
                .map(brandResultMapper::toResult)
                .toList();
        long total = brandRepository.count();
        return new PageResult<>(results, pagination.page(), pagination.size(), total);
    }

    @Transactional(readOnly = true)
    public BrandResult get(String brandId) {
        return brandResultMapper.toResult(required(brandId));
    }

    private Brand required(String brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.BRAND_NOT_FOUND));
    }
}
