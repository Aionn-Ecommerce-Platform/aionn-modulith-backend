package com.aionn.catalog.adapter.rest.dto.brand;

import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateBrandRequest(
                @Size(min = 1, max = 150) String name,
                @URL @Size(max = 2048) String logoUrl,
                @Size(max = 2000) String description) {
}
