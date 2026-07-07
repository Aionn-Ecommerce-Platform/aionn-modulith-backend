package com.aionn.catalog.application.mapper;

import com.aionn.catalog.application.dto.brand.result.BrandResult;
import com.aionn.catalog.domain.model.Brand;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
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
                brand.getUpdatedAt()
        );
    }
}
