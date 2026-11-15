package com.aionn.recommendation.infrastructure.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@ConfigurationProperties(prefix = "recommendation.weights")
public record RecommendationWeightProperties(
                @NotNull @Valid @DefaultValue Signal view,
                @NotNull @Valid @DefaultValue Signal search,
                @NotNull @Valid @DefaultValue Signal wishlistAdd,
                @NotNull @Valid @DefaultValue Signal cartAdd,
                @NotNull @Valid @DefaultValue Signal orderPlaced,
                @NotNull @Valid @DefaultValue Signal purchase) {

        public record Signal(
                        @DecimalMin("0.01") @DecimalMax("999.99") @DefaultValue("1.0") BigDecimal base,
                        @Min(1) @Max(3650) @DefaultValue("30") long halfLifeDays) {
        }
}
