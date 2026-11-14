package com.aionn.catalog.infrastructure.integration.catalog;

import com.aionn.catalog.application.port.out.merchant.MerchantPersistencePort;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.model.ProductVariant;
import com.aionn.catalog.domain.valueobject.MerchantStatus;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.contracts.port.catalog.CatalogQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CatalogQueryAdapter implements CatalogQueryPort {

    private final ProductPersistencePort productPersistencePort;
    private final MerchantPersistencePort merchantPersistencePort;

    @Override
    @Transactional(readOnly = true)
    public List<ProductView> search(SearchCriteria criteria) {
        int fetchLimit = criteria.limit() + 1;
        List<Product> products = productPersistencePort.searchPublished(criteria.query(), criteria.minPrice(),
                criteria.maxPrice(), fetchLimit, criteria.offset());
        List<ProductView> mapped = new ArrayList<>(products.size());
        for (Product p : products) {
            ProductView dto = toView(p);
            if (dto != null) {
                mapped.add(dto);
            }
        }
        return mapped;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductView> findByProductOrSkuId(String id) {
        Optional<Product> byId = productPersistencePort.findById(id);
        if (byId.isPresent()) {
            return Optional.ofNullable(toView(byId.get()));
        }
        List<Product> bySku = productPersistencePort.findAllBySkuIds(List.of(id));
        if (!bySku.isEmpty()) {
            return Optional.ofNullable(toView(bySku.get(0)));
        }
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public LookupResult lookupByProductOrSkuIds(List<String> ids) {
        List<Product> bySku = productPersistencePort.findAllBySkuIds(ids);
        Map<String, Product> productById = new LinkedHashMap<>();
        Set<String> resolvedInputIds = new HashSet<>();
        for (Product p : bySku) {
            productById.put(p.getProductId(), p);
            for (ProductVariant v : p.variants()) {
                if (ids.contains(v.skuId())) {
                    resolvedInputIds.add(v.skuId());
                }
            }
        }
        for (String id : ids) {
            if (resolvedInputIds.contains(id) || productById.containsKey(id)) {
                continue;
            }
            productPersistencePort.findById(id).ifPresent(p -> {
                productById.put(p.getProductId(), p);
                resolvedInputIds.add(id);
            });
        }

        List<ProductView> views = new ArrayList<>();
        for (Product p : productById.values()) {
            ProductView v = toView(p);
            if (v != null) {
                views.add(v);
            }
        }
        List<String> notFound = ids.stream().filter(id -> !resolvedInputIds.contains(id)).toList();
        return new LookupResult(views, notFound);
    }

    private ProductView toView(Product p) {
        if (p.getStatus() != ProductStatus.PUBLISHED) {
            return null;
        }
        boolean merchantActive = merchantPersistencePort.findById(p.getMerchantId())
                .map(m -> m.getStatus() == MerchantStatus.ACTIVE)
                .orElse(false);
        if (!merchantActive) {
            return null;
        }
        List<ProductVariant> domainVariants = p.variants();
        List<VariantView> variants = new ArrayList<>(domainVariants.size());
        for (ProductVariant v : domainVariants) {
            Money price = v.price();
            BigDecimal amount = price == null ? null : price.amount();
            String currency = price == null ? null : price.currency();
            ProductVariant.ParcelDimensions parcel = v.parcel();
            ParcelView parcelView = parcel == null ? null
                    : new ParcelView(parcel.weightGram(), parcel.lengthCm(), parcel.widthCm(), parcel.heightCm());
            variants.add(new VariantView(
                    v.skuId(),
                    p.getName() + variantSuffix(v),
                    amount,
                    currency,
                    price != null,
                    v.attributeValues() == null ? Map.of() : v.attributeValues(),
                    parcelView));
        }
        return new ProductView(
                p.getProductId(),
                p.getName(),
                p.getAiDescription(),
                p.imageList(),
                variants);
    }

    private static String variantSuffix(ProductVariant v) {
        if (v.attributeValues() == null || v.attributeValues().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" (");
        boolean first = true;
        for (Map.Entry<String, String> e : v.attributeValues().entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(e.getValue());
            first = false;
        }
        sb.append(')');
        return sb.toString();
    }
}
