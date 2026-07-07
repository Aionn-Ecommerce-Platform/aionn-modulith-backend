package com.aionn.catalog.application.port.out.settings;

import java.math.BigDecimal;

public interface CatalogSettingsPort {
    BigDecimal getDefaultCommissionRate();
    void updateDefaultCommissionRate(BigDecimal rate);
}
