package com.aionn.ucp.application.catalog;

import com.aionn.sharedkernel.integration.port.catalog.CatalogQueryPort;
import com.aionn.ucp.adapter.rest.dto.UcpMessage;
import com.aionn.ucp.adapter.rest.dto.cart.UcpResponseMetadata;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpCatalogLookupRequest;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpCatalogLookupResponse;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpCatalogModels.*;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpCatalogSearchRequest;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpCatalogSearchResponse;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpProductDetailRequest;
import com.aionn.ucp.adapter.rest.dto.catalog.UcpProductDetailResponse;
import com.aionn.ucp.application.port.out.UcpSchemaValidationPort;
import com.aionn.ucp.domain.exception.UcpProtocolException;
import com.aionn.ucp.domain.util.UcpCurrencyUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class UcpCatalogApplicationService {

    private static final String PROTOCOL_VERSION = "2026-08-25";
    private static final String CATALOG_SEARCH_SCHEMA_URI = "https://ucp.dev/schemas/shopping/catalog_search.json#/$defs/search_response";
    private static final String CATALOG_LOOKUP_SCHEMA_URI = "https://ucp.dev/schemas/shopping/catalog_lookup.json#/$defs/lookup_response";
    private static final String GET_PRODUCT_SCHEMA_URI = "https://ucp.dev/schemas/shopping/catalog_lookup.json#/$defs/get_product_response";

    private static final String MATCH_EXACT = "exact";
    private static final String MATCH_FEATURED = "featured";
    private static final int DEFAULT_SEARCH_LIMIT = 10;
    private static final int MAX_SEARCH_LIMIT = 100;
    private static final String DEFAULT_CURRENCY = "USD";
    private static final String STATUS_IN_STOCK = "in_stock";
    private static final String STATUS_OUT_OF_STOCK = "out_of_stock";

    private final CatalogQueryPort catalogQueryPort;
    private final UcpSchemaValidationPort schemaValidator;
    private final ObjectMapper objectMapper;
    private final String version;

    @Autowired
    public UcpCatalogApplicationService(
            CatalogQueryPort catalogQueryPort,
            @Autowired(required = false) UcpSchemaValidationPort schemaValidator) {
        this.catalogQueryPort = catalogQueryPort;
        this.schemaValidator = schemaValidator;
        this.objectMapper = new ObjectMapper();
        this.version = PROTOCOL_VERSION;
    }

    public UcpCatalogSearchResponse searchCatalog(UcpCatalogSearchRequest request) {
        int limit = DEFAULT_SEARCH_LIMIT;
        if (request != null && request.pagination() != null && request.pagination().limit() != null) {
            limit = Math.clamp(request.pagination().limit(), 1, MAX_SEARCH_LIMIT);
        }

        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        if (request != null && request.filters() != null && request.filters().price() != null) {
            minPrice = UcpCurrencyUtil.fromMinorUnits(request.filters().price().min(), DEFAULT_CURRENCY);
            maxPrice = UcpCurrencyUtil.fromMinorUnits(request.filters().price().max(), DEFAULT_CURRENCY);
        }

        String query = request != null ? request.query() : null;
        CatalogQueryPort.SearchCriteria criteria = new CatalogQueryPort.SearchCriteria(query, limit, minPrice,
                maxPrice);

        List<CatalogQueryPort.ProductView> views = catalogQueryPort.search(criteria);
        List<UcpProductDto> products = views.stream()
                .map(v -> toProductDto(v, null, false))
                .flatMap(Optional::stream)
                .toList();

        boolean hasNextPage = views.size() >= limit;
        String nextCursor = hasNextPage ? ("cursor_" + limit) : null;
        UcpPaginationResponseDto pagination = new UcpPaginationResponseDto(hasNextPage, nextCursor, views.size());

        UcpCatalogSearchResponse response = new UcpCatalogSearchResponse(
                UcpResponseMetadata.success(version),
                products,
                pagination,
                null,
                null,
                null);

        validateSchema(CATALOG_SEARCH_SCHEMA_URI, response);
        return response;
    }

    public UcpCatalogLookupResponse lookupCatalog(UcpCatalogLookupRequest request) {
        if (request == null || request.ids() == null || request.ids().isEmpty()) {
            throw new UcpProtocolException(400, "invalid_request", "Missing required field: ids", "error", "$.ids");
        }

        CatalogQueryPort.LookupResult result = catalogQueryPort.lookupByProductOrSkuIds(request.ids());
        List<UcpProductDto> products = result.products().stream()
                .map(v -> toProductDto(v, request.ids(), true))
                .flatMap(Optional::stream)
                .toList();

        List<UcpMessage> messages = null;
        if (result.notFound() != null && !result.notFound().isEmpty()) {
            messages = result.notFound().stream()
                    .map(id -> UcpMessage.warning("item_not_found", "Identifier not found in catalog: " + id, "$.ids"))
                    .toList();
        }

        UcpCatalogLookupResponse response = new UcpCatalogLookupResponse(
                UcpResponseMetadata.success(version),
                products,
                null,
                messages,
                null);

        validateSchema(CATALOG_LOOKUP_SCHEMA_URI, response);
        return response;
    }

    public UcpProductDetailResponse getProduct(UcpProductDetailRequest request) {
        if (request == null || request.id() == null || request.id().isBlank()) {
            throw new UcpProtocolException(400, "invalid_request", "Missing required field: id", "error", "$.id");
        }

        CatalogQueryPort.ProductView view = catalogQueryPort.findByProductOrSkuId(request.id())
                .orElseThrow(() -> new UcpProtocolException(404, "item_not_found",
                        "Product or SKU not found: " + request.id(), "error", "$.id"));

        UcpProductDto productDto = toProductDto(view, null, false)
                .orElseThrow(() -> new UcpProtocolException(404, "item_not_found",
                        "Product has no purchasable variants: " + request.id(), "error", "$.id"));

        List<UcpSelectedOptionDto> selected = determineSelectedOptions(request, productDto);

        UcpProductDto detailedProduct = new UcpProductDto(
                productDto.id(),
                productDto.title(),
                productDto.description(),
                productDto.priceRange(),
                productDto.variants(),
                productDto.media(),
                productDto.options(),
                productDto.categories(),
                productDto.rating(),
                productDto.tags(),
                productDto.handle(),
                productDto.url(),
                selected);

        UcpProductDetailResponse response = new UcpProductDetailResponse(
                UcpResponseMetadata.success(version),
                detailedProduct,
                null,
                null,
                null);

        validateSchema(GET_PRODUCT_SCHEMA_URI, response);
        return response;
    }

    private Optional<UcpProductDto> toProductDto(CatalogQueryPort.ProductView view, List<String> requestedIds,
            boolean isLookup) {
        if (view == null || view.variants() == null || view.variants().isEmpty()) {
            return Optional.empty();
        }

        String currency = resolveCurrency(view.variants());
        List<UcpVariantDto> variants = mapVariants(view, requestedIds, isLookup, currency);
        if (variants.isEmpty()) {
            return Optional.empty();
        }

        UcpPriceRangeDto priceRange = calculatePriceRange(variants, currency);
        Map<String, Set<String>> optionValuesByName = aggregateOptionValues(view.variants());
        List<UcpProductOptionDto> options = buildOptions(optionValuesByName);
        List<UcpMediaDto> media = buildMedia(view);

        UcpDescriptionDto description = new UcpDescriptionDto(
                view.description() != null && !view.description().isBlank() ? view.description() : view.name());

        return Optional.of(new UcpProductDto(
                view.productId(),
                view.name(),
                description,
                priceRange,
                variants,
                media,
                options,
                null,
                null,
                null,
                null,
                null,
                null));
    }

    private String resolveCurrency(List<CatalogQueryPort.VariantView> variants) {
        for (CatalogQueryPort.VariantView v : variants) {
            if (v.currency() != null && !v.currency().isBlank()) {
                return v.currency();
            }
        }
        return DEFAULT_CURRENCY;
    }

    private List<UcpVariantDto> mapVariants(
            CatalogQueryPort.ProductView view,
            List<String> requestedIds,
            boolean isLookup,
            String defaultCurrency) {
        List<UcpVariantDto> variants = new ArrayList<>();
        for (int i = 0; i < view.variants().size(); i++) {
            CatalogQueryPort.VariantView v = view.variants().get(i);
            String currency = (v.currency() != null && !v.currency().isBlank()) ? v.currency() : defaultCurrency;
            long priceMinor = UcpCurrencyUtil.toMinorUnits(v.price(), currency);

            List<UcpSelectedOptionDto> selectedOptions = extractSelectedOptions(v.attributeValues());
            List<UcpInputCorrelationDto> inputs = determineVariantInputs(view.productId(), v.skuId(), i, requestedIds,
                    isLookup);

            UcpPriceDto variantPrice = new UcpPriceDto(priceMinor, currency);
            UcpAvailabilityDto availability = new UcpAvailabilityDto(v.available(),
                    v.available() ? STATUS_IN_STOCK : STATUS_OUT_OF_STOCK);

            String displayName = v.displayName() != null ? v.displayName() : view.name();
            UcpDescriptionDto desc = new UcpDescriptionDto(
                    view.description() != null && !view.description().isBlank() ? view.description() : view.name());

            variants.add(new UcpVariantDto(
                    v.skuId(),
                    displayName,
                    desc,
                    variantPrice,
                    v.skuId(),
                    availability,
                    selectedOptions,
                    null,
                    inputs));
        }
        return variants;
    }

    private List<UcpInputCorrelationDto> determineVariantInputs(
            String productId,
            String skuId,
            int variantIndex,
            List<String> requestedIds,
            boolean isLookup) {
        if (!isLookup || requestedIds == null) {
            return null;
        }

        List<UcpInputCorrelationDto> inputs = new ArrayList<>();
        if (requestedIds.contains(skuId)) {
            inputs.add(new UcpInputCorrelationDto(skuId, MATCH_EXACT));
        }
        if (requestedIds.contains(productId)) {
            inputs.add(new UcpInputCorrelationDto(productId, variantIndex == 0 ? MATCH_FEATURED : MATCH_EXACT));
        }
        if (inputs.isEmpty()) {
            inputs.add(new UcpInputCorrelationDto(productId, MATCH_FEATURED));
        }
        return inputs;
    }

    private List<UcpSelectedOptionDto> extractSelectedOptions(Map<String, String> attributeValues) {
        if (attributeValues == null || attributeValues.isEmpty()) {
            return null;
        }
        List<UcpSelectedOptionDto> selected = new ArrayList<>();
        for (Map.Entry<String, String> attr : attributeValues.entrySet()) {
            selected.add(new UcpSelectedOptionDto(attr.getKey(), attr.getValue(), null));
        }
        return selected;
    }

    private UcpPriceRangeDto calculatePriceRange(List<UcpVariantDto> variants, String currency) {
        long minPrice = variants.get(0).price().amount();
        long maxPrice = minPrice;
        for (UcpVariantDto v : variants) {
            long price = v.price().amount();
            if (price < minPrice) {
                minPrice = price;
            }
            if (price > maxPrice) {
                maxPrice = price;
            }
        }
        return new UcpPriceRangeDto(
                new UcpPriceDto(minPrice, currency),
                new UcpPriceDto(maxPrice, currency));
    }

    private Map<String, Set<String>> aggregateOptionValues(List<CatalogQueryPort.VariantView> variants) {
        Map<String, Set<String>> optionValuesByName = new LinkedHashMap<>();
        for (CatalogQueryPort.VariantView v : variants) {
            if (v.attributeValues() != null) {
                for (Map.Entry<String, String> entry : v.attributeValues().entrySet()) {
                    optionValuesByName.computeIfAbsent(entry.getKey(), k -> new LinkedHashSet<>())
                            .add(entry.getValue());
                }
            }
        }
        return optionValuesByName;
    }

    private List<UcpProductOptionDto> buildOptions(Map<String, Set<String>> optionValuesByName) {
        if (optionValuesByName.isEmpty()) {
            return null;
        }
        List<UcpProductOptionDto> options = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : optionValuesByName.entrySet()) {
            List<UcpOptionValueDto> optionValues = entry.getValue().stream()
                    .map(val -> new UcpOptionValueDto(val, null, true, true))
                    .toList();
            options.add(new UcpProductOptionDto(entry.getKey(), optionValues));
        }
        return options;
    }

    private List<UcpMediaDto> buildMedia(CatalogQueryPort.ProductView view) {
        if (view.imageUrls() == null || view.imageUrls().isEmpty()) {
            return null;
        }
        return view.imageUrls().stream()
                .map(url -> new UcpMediaDto("image", url, view.name()))
                .toList();
    }

    private List<UcpSelectedOptionDto> determineSelectedOptions(
            UcpProductDetailRequest request,
            UcpProductDto productDto) {
        if (request != null && request.selected() != null && !request.selected().isEmpty()) {
            return request.selected();
        }
        if (productDto.variants() != null && !productDto.variants().isEmpty()) {
            return productDto.variants().get(0).options();
        }
        return null;
    }

    private void validateSchema(String schemaUri, Object response) {
        if (schemaValidator == null) {
            return;
        }
        try {
            schemaValidator.validate(schemaUri, objectMapper.valueToTree(response));
        } catch (UcpProtocolException e) {
            throw e;
        } catch (Exception e) {
            throw new UcpProtocolException(500, "schema_validation_failed",
                    "Response schema validation error: " + e.getMessage(), "error", null);
        }
    }
}
