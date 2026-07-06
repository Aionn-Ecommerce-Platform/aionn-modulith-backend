package com.aionn.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "product_variants", indexes = {
        @Index(name = "idx_variants_product", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantEntity {

    @Id
    @Column(name = "sku_id", nullable = false, length = 50)
    private String skuId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attribute_values", columnDefinition = "jsonb")
    private Map<String, String> attributeValues;

    @Column(name = "price", precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "original_price", precision = 18, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "currency", length = 3)
    private String currency;
}

