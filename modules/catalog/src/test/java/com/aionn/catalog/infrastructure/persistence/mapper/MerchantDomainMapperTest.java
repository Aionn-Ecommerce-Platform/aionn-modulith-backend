package com.aionn.catalog.infrastructure.persistence.mapper;

import com.aionn.catalog.domain.model.Merchant;
import com.aionn.catalog.infrastructure.persistence.entity.MerchantEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantDomainMapperTest {

    private final MerchantDomainMapper mapper = new MerchantDomainMapperImpl();

    @Test
    void roundTripsMerchant() {
        Merchant merchant = Merchant.register("m1", "owner-1", "Acme", new BigDecimal("0.05"));

        MerchantEntity entity = mapper.toEntity(merchant);
        assertThat(entity.getMerchantId()).isEqualTo("m1");
        assertThat(entity.getStatus()).isEqualTo(merchant.getStatus().name());

        Merchant back = mapper.toDomain(entity);
        assertThat(back.getMerchantId()).isEqualTo("m1");
        assertThat(back.getOwnerId()).isEqualTo("owner-1");
        assertThat(back.getStatus()).isEqualTo(merchant.getStatus());
    }

    @Test
    void nullInputsReturnNull() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
