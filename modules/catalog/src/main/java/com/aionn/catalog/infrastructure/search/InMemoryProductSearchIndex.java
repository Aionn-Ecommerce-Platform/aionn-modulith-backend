package com.aionn.catalog.infrastructure.search;

import com.aionn.catalog.application.port.out.search.ProductSearchIndexPort;
import com.aionn.catalog.domain.model.Product;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryProductSearchIndex implements ProductSearchIndexPort {

    private final Map<String, IndexedProduct> byId = new ConcurrentHashMap<>();

    @Override
    public void index(Product product) {
        byId.put(product.getProductId(), IndexedProduct.from(product));
    }

    @Override
    public void delete(String productId) {
        byId.remove(productId);
    }

    @Override
    public List<String> searchIds(String keyword, OffsetPagination pagination) {
        String needle = normalize(keyword);
        return byId.values().stream()
                .filter(p -> needle.isEmpty() || p.searchable().contains(needle))
                .sorted(Comparator.comparing(IndexedProduct::productId))
                .skip((long) pagination.page() * pagination.size())
                .limit(pagination.size())
                .map(IndexedProduct::productId)
                .toList();
    }

    @Override
    public long countMatches(String keyword) {
        String needle = normalize(keyword);
        if (needle.isEmpty()) {
            return byId.size();
        }
        return byId.values().stream()
                .filter(p -> p.searchable().contains(needle))
                .count();
    }

    private String normalize(String keyword) {
        return keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
    }

    private record IndexedProduct(String productId, String searchable) {
        static IndexedProduct from(Product product) {
            StringBuilder sb = new StringBuilder();
            if (product.getName() != null)
                sb.append(product.getName().toLowerCase(Locale.ROOT)).append(' ');
            if (product.getAiDescription() != null)
                sb.append(product.getAiDescription().toLowerCase(Locale.ROOT)).append(' ');
            product.tags().forEach(t -> sb.append(t.toLowerCase(Locale.ROOT)).append(' '));
            return new IndexedProduct(product.getProductId(), sb.toString().trim());
        }
    }
}
