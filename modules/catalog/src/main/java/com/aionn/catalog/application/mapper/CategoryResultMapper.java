package com.aionn.catalog.application.mapper;

import com.aionn.catalog.application.dto.category.result.CategoryResult;
import com.aionn.catalog.domain.model.Category;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import java.util.Locale;

@Component
public class CategoryResultMapper {

    public CategoryResult toResult(Category category) {
        if (category == null) {
            return null;
        }

        Locale locale = LocaleContextHolder.getLocale();
        String fullTag = locale.toLanguageTag();
        String language = locale.getLanguage();
        String name = category.getName();

        Category.Translation trans = category.translations().stream()
                .filter(t -> t.locale().equalsIgnoreCase(fullTag))
                .findFirst()
                .or(() -> category.translations().stream()
                        .filter(t -> t.locale().equalsIgnoreCase(language))
                        .findFirst())
                .orElse(null);
        if (trans != null) {
            name = trans.name();
        }

        return new CategoryResult(
                category.getCategoryId(),
                category.getParentId(),
                name,
                category.getSlug(),
                category.getIconUrl(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}
