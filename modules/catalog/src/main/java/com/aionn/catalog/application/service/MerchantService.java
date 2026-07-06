package com.aionn.catalog.application.service;

import com.aionn.catalog.application.dto.merchant.command.ActivateMerchantCommand;
import com.aionn.catalog.application.dto.merchant.command.CloseMerchantCommand;
import com.aionn.catalog.application.dto.merchant.command.RegisterMerchantCommand;
import com.aionn.catalog.application.dto.merchant.command.SuspendMerchantCommand;
import com.aionn.catalog.application.dto.merchant.command.UpdateMerchantProfileCommand;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;
import com.aionn.catalog.application.mapper.MerchantResultMapper;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.catalog.application.port.out.merchant.MerchantPersistencePort;
import com.aionn.catalog.application.port.out.observability.CatalogMetricsPort;
import com.aionn.sharedkernel.integration.port.identity.AddressLookupPort;
import com.aionn.sharedkernel.integration.port.ordering.OrderQueryPort;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.model.Merchant;
import com.aionn.sharedkernel.util.IdGenerator;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import com.aionn.catalog.application.port.out.settings.CatalogSettingsPort;
import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.merchant.command.UpdateMerchantCommissionRateCommand;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantPersistencePort merchantRepository;
    private final MerchantResultMapper merchantResultMapper;
    private final EventPublisher eventPublisher;
    private final OrderQueryPort orderQueryPort;
    private final AddressLookupPort addressLookupPort;
    private final CatalogMetricsPort metricsPort;
    private final CatalogSettingsPort catalogSettingsPort;

    public MerchantResult register(RegisterMerchantCommand command) {
        if (merchantRepository.existsByOwnerId(command.ownerId())) {
            throw new CatalogException(CatalogErrorCode.MERCHANT_ALREADY_EXISTS);
        }
        java.math.BigDecimal rate = catalogSettingsPort.getDefaultCommissionRate();
        Merchant merchant = Merchant.register(IdGenerator.ulid(), command.ownerId(), command.name(), rate);
        Merchant saved = merchantRepository.save(merchant);
        eventPublisher.publish(merchant.pullEvents());
        metricsPort.merchantLifecycle("registered");
        return merchantResultMapper.toResult(saved);
    }

    public MerchantResult updateProfile(UpdateMerchantProfileCommand command) {
        Merchant merchant = ownedBy(command.merchantId(), command.ownerId());
        String provinceCode = command.provinceCode();
        String provinceName = null;
        if (provinceCode != null && !provinceCode.isBlank()) {
            // Validate against identity. Reject unknown codes so the catalog never
            // stores a province that the storefront filter (which reads from
            // /geography/provinces) cannot match.
            provinceName = addressLookupPort.resolveProvince(provinceCode)
                    .orElseThrow(() -> new CatalogException(CatalogErrorCode.INVALID_ARGUMENT,
                            "Unknown province code: " + command.provinceCode()))
                    .name();
        } else {
            provinceCode = null;
        }
        merchant.updateProfile(command.name(), command.logoUrl(), command.description(),
                provinceCode, provinceName);
        Merchant saved = merchantRepository.save(merchant);
        eventPublisher.publish(merchant.pullEvents());
        return merchantResultMapper.toResult(saved);
    }

    public MerchantResult suspend(SuspendMerchantCommand command) {
        Merchant merchant = required(command.merchantId());
        merchant.suspend(command.adminId(), command.reason());
        Merchant saved = merchantRepository.save(merchant);
        eventPublisher.publish(merchant.pullEvents());
        metricsPort.merchantLifecycle("suspended");
        return merchantResultMapper.toResult(saved);
    }

    public MerchantResult activate(ActivateMerchantCommand command) {
        Merchant merchant = required(command.merchantId());
        merchant.activate(command.adminId(), command.reason());
        Merchant saved = merchantRepository.save(merchant);
        eventPublisher.publish(merchant.pullEvents());
        metricsPort.merchantLifecycle("activated");
        return merchantResultMapper.toResult(saved);
    }

    public MerchantResult close(CloseMerchantCommand command) {
        Merchant merchant = ownedBy(command.merchantId(), command.ownerId());
        if (orderQueryPort.hasOpenOrdersForMerchant(merchant.getMerchantId())) {
            throw new CatalogException(CatalogErrorCode.MERCHANT_HAS_OPEN_ORDERS);
        }
        merchant.close(command.reason());
        Merchant saved = merchantRepository.save(merchant);
        eventPublisher.publish(merchant.pullEvents());
        metricsPort.merchantLifecycle("closed");
        return merchantResultMapper.toResult(saved);
    }

    @Transactional(readOnly = true)
    public MerchantResult get(String merchantId) {
        return merchantResultMapper.toResult(required(merchantId));
    }

    @Transactional(readOnly = true)
    public MerchantResult getByOwner(String ownerId) {
        Merchant merchant = merchantRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.MERCHANT_NOT_FOUND));
        return merchantResultMapper.toResult(merchant);
    }

    @Transactional(readOnly = true)
    public List<MerchantResult> list(int page, int size) {
        return merchantRepository.list(OffsetPagination.of(page, size)).stream()
                .map(merchantResultMapper::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<MerchantResult> list(OffsetPagination pagination) {
        List<MerchantResult> results = merchantRepository.list(pagination).stream()
                .map(merchantResultMapper::toResult)
                .toList();
        return new PageResult<>(
                results, pagination.page(), pagination.size(), results.size());
    }

    public MerchantResult updateCommissionRate(UpdateMerchantCommissionRateCommand command) {
        Merchant merchant = required(command.merchantId());
        merchant.updateCommissionRate(command.commissionRate());
        Merchant saved = merchantRepository.save(merchant);
        eventPublisher.publish(merchant.pullEvents());
        return merchantResultMapper.toResult(saved);
    }

    private Merchant required(String merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.MERCHANT_NOT_FOUND));
    }

    private Merchant ownedBy(String merchantId, String ownerId) {
        Merchant merchant = required(merchantId);
        if (!merchant.getOwnerId().equals(ownerId)) {
            throw new CatalogException(CatalogErrorCode.MERCHANT_FORBIDDEN);
        }
        return merchant;
    }
}
