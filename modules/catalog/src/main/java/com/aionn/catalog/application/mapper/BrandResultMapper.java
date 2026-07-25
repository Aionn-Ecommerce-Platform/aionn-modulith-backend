package com.aionn.catalog.application.mapper;

import com.aionn.catalog.application.dto.brand.result.BrandResult;
import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.domain.model.Brand;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Locale;

@Component
public class BrandResultMapper {

    public BrandResult toResult(Brand brand) {
        if (brand == null) {
            return null;
        }

        Locale locale = LocaleContextHolder.getLocale();
        String name = brand.getName();
        String description = brand.getDescription();

        Brand.Translation trans = brand.translations().stream()
                .filter(t -> t.locale().equalsIgnoreCase(locale.getLanguage()))
                .findFirst()
                .orElse(null);
        if (trans != null) {
            name = trans.name();
            description = trans.description();
        }

        return new BrandResult(
                brand.getBrandId(),
                name,
                brand.getLogoUrl(),
                description,
                brand.getStatus().name(),
                brand.getCreatedAt(),
                brand.getUpdatedAt());
    }

    public List<BrandResult> toResults(List<Brand> brands) {
        if (brands == null) {
            return List.of();
        }
        return brands.stream()
                .map(this::toResult)
                .toList();
    }

    public PageResult<BrandResult> toPageResult(PageResult<Brand> page) {
        return new PageResult<>(
                toResults(page.content()),
                page.page(),
                page.size(),
                page.totalElements());
    }
}
