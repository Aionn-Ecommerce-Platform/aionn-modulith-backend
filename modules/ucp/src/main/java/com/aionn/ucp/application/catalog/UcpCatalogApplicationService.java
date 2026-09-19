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
        int limit = 10;
        if (request != null && request.pagination() != null && request.pagination().limit() != null) {
            limit = Math.max(1, request.pagination().limit());
        }

        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        if (request != null && request.filters() != null && request.filters().price() != null) {
            minPrice = UcpCurrencyUtil.fromMinorUnits(request.filters().price().min(), "USD");
            maxPrice = UcpCurrencyUtil.fromMinorUnits(request.filters().price().max(), "USD");
        }

        String query = request != null ? request.query() : null;
        CatalogQueryPort.SearchCriteria criteria = new CatalogQueryPort.SearchCriteria(query, limit, minPrice,
                maxPrice);

        List<CatalogQueryPort.ProductView> views = catalogQueryPort.search(criteria);
        List<UcpProductDto> products = views.stream()
                .map(v -> toProductDto(v, null, false))
                .toList();

        boolean hasNextPage = views.size() >= limit;
        String nextCursor = hasNextPage ? "cursor_" + limit : null;
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

        UcpProductDto productDto = toProductDto(view, null, false);

        // If specific option selections are provided, reflect them in selected field
        List<UcpSelectedOptionDto> selected = (request.selected() != null && !request.selected().isEmpty())
                ? request.selected()
                : (productDto.variants() != null && !productDto.variants().isEmpty()
                        ? productDto.variants().get(0).options()
                        : null);

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

    private UcpProductDto toProductDto(CatalogQueryPort.ProductView view, List<String> requestedIds, boolean isLookup) {
        String currency = "USD";
        long minPrice = 0L;
        long maxPrice = 0L;

        List<UcpVariantDto> variants = new ArrayList<>();
        Map<String, Set<String>> optionValuesByName = new LinkedHashMap<>();

        if (view.variants() != null && !view.variants().isEmpty()) {
            for (int i = 0; i < view.variants().size(); i++) {
                CatalogQueryPort.VariantView v = view.variants().get(i);
                if (v.currency() != null && !v.currency().isBlank()) {
                    currency = v.currency();
                }
                long priceMinor = UcpCurrencyUtil.toMinorUnits(v.price(), currency);
                if (i == 0) {
                    minPrice = priceMinor;
                    maxPrice = priceMinor;
                } else {
                    if (priceMinor < minPrice)
                        minPrice = priceMinor;
                    if (priceMinor > maxPrice)
                        maxPrice = priceMinor;
                }

                List<UcpSelectedOptionDto> selectedOptions = null;
                if (v.attributeValues() != null && !v.attributeValues().isEmpty()) {
                    selectedOptions = new ArrayList<>();
                    for (Map.Entry<String, String> attr : v.attributeValues().entrySet()) {
                        selectedOptions.add(new UcpSelectedOptionDto(attr.getKey(), attr.getValue(), null));
                        optionValuesByName.computeIfAbsent(attr.getKey(), k -> new LinkedHashSet<>())
                                .add(attr.getValue());
                    }
                }

                List<UcpInputCorrelationDto> inputs = null;
                if (isLookup && requestedIds != null) {
                    inputs = new ArrayList<>();
                    if (requestedIds.contains(v.skuId())) {
                        inputs.add(new UcpInputCorrelationDto(v.skuId(), "exact"));
                    }
                    if (requestedIds.contains(view.productId())) {
                        inputs.add(new UcpInputCorrelationDto(view.productId(), i == 0 ? "featured" : "exact"));
                    }
                    if (inputs.isEmpty()) {
                        inputs.add(new UcpInputCorrelationDto(view.productId(), "featured"));
                    }
                }

                UcpPriceDto variantPrice = new UcpPriceDto(priceMinor, currency);
                UcpAvailabilityDto availability = new UcpAvailabilityDto(v.available(),
                        v.available() ? "in_stock" : "out_of_stock");

                variants.add(new UcpVariantDto(
                        v.skuId(),
                        v.displayName() != null ? v.displayName() : view.name(),
                        new UcpDescriptionDto(
                                view.description() != null && !view.description().isBlank() ? view.description()
                                        : view.name()),
                        variantPrice,
                        v.skuId(),
                        availability,
                        selectedOptions,
                        null,
                        inputs));
            }
        } else {
            // Default single variant to satisfy schema minItems: 1
            List<UcpInputCorrelationDto> inputs = isLookup && requestedIds != null
                    ? List.of(new UcpInputCorrelationDto(view.productId(), "featured"))
                    : null;
            variants.add(new UcpVariantDto(
                    view.productId(),
                    view.name(),
                    new UcpDescriptionDto(
                            view.description() != null && !view.description().isBlank() ? view.description()
                                    : view.name()),
                    new UcpPriceDto(0L, currency),
                    null,
                    new UcpAvailabilityDto(true, "in_stock"),
                    null,
                    null,
                    inputs));
        }

        UcpPriceRangeDto priceRange = new UcpPriceRangeDto(
                new UcpPriceDto(minPrice, currency),
                new UcpPriceDto(maxPrice, currency));

        List<UcpMediaDto> media = null;
        if (view.imageUrls() != null && !view.imageUrls().isEmpty()) {
            media = view.imageUrls().stream()
                    .map(url -> new UcpMediaDto("image", url, view.name()))
                    .toList();
        }

        List<UcpProductOptionDto> options = null;
        if (!optionValuesByName.isEmpty()) {
            options = new ArrayList<>();
            for (Map.Entry<String, Set<String>> entry : optionValuesByName.entrySet()) {
                List<UcpOptionValueDto> optionValues = entry.getValue().stream()
                        .map(val -> new UcpOptionValueDto(val, null, true, true))
                        .toList();
                options.add(new UcpProductOptionDto(entry.getKey(), optionValues));
            }
        }

        return new UcpProductDto(
                view.productId(),
                view.name(),
                new UcpDescriptionDto(
                        view.description() != null && !view.description().isBlank() ? view.description() : view.name()),
                priceRange,
                variants,
                media,
                options,
                null,
                null,
                null,
                null,
                null,
                null);
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
