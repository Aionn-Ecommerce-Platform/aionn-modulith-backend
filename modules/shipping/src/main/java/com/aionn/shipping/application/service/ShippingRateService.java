package com.aionn.shipping.application.service;

import java.math.BigDecimal;

import com.aionn.shipping.application.dto.rate.command.ConfigureRateCommand;
import com.aionn.shipping.application.dto.rate.command.UpdateRateCommand;
import com.aionn.shipping.application.policy.ShippingDefaultsPolicy;
import com.aionn.shipping.application.port.out.ShippingRatePersistencePort;
import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import com.aionn.shipping.domain.model.ShippingRate;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShippingRateService {

    private final ShippingRatePersistencePort repository;
    private final EventPublisher eventPublisher;
    private final ShippingDefaultsPolicy defaultsPolicy;
    private final Clock clock;

    public ShippingRate configure(ConfigureRateCommand command) {
        if (repository.findByZoneCode(command.zoneCode()).isPresent()) {
            throw new ShippingException(ShippingErrorCode.RATE_DUPLICATE);
        }
        ShippingRate rate = ShippingRate.configure(IdGenerator.ulid(),
                command.zoneCode(), command.baseFee(),
                command.currency() == null ? defaultsPolicy.defaultCurrency() : command.currency(),
                command.condition(),
                orDefault(command.baseWeightGram(), defaultsPolicy.defaultBaseWeightGram()),
                orDefault(command.stepWeightGram(), defaultsPolicy.defaultStepWeightGram()),
                command.stepFee() == null ? BigDecimal.ZERO : command.stepFee(),
                command.maxFee(), command.freeShippingThreshold(),
                command.expressExtraFee() == null ? BigDecimal.ZERO : command.expressExtraFee(),
                command.expressEtaDays(), clock);
        ShippingRate saved;
        try {
            saved = repository.save(rate);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ShippingException(ShippingErrorCode.RATE_DUPLICATE);
        }
        eventPublisher.publish(rate.pullEvents());
        return saved;
    }

    public ShippingRate update(UpdateRateCommand command) {
        ShippingRate rate = repository.findById(command.rateId())
                .orElseThrow(() -> new ShippingException(ShippingErrorCode.RATE_NOT_FOUND));
        rate.update(command.baseFee(), command.condition(), clock);
        rate.updateExpress(command.expressExtraFee(), command.expressEtaDays(), clock);
        if (command.baseWeightGram() != null || command.stepWeightGram() != null
                || command.stepFee() != null || command.maxFee() != null
                || command.freeShippingThreshold() != null) {
            rate.updateWeightLadder(
                    orDefault(command.baseWeightGram(), rate.getBaseWeightGram()),
                    orDefault(command.stepWeightGram(), rate.getStepWeightGram()),
                    command.stepFee() == null ? rate.getStepFee() : command.stepFee(),
                    command.maxFee() == null ? rate.getMaxFee() : command.maxFee(),
                    command.freeShippingThreshold() == null
                            ? rate.getFreeShippingThreshold() : command.freeShippingThreshold(),
                    clock);
        }
        ShippingRate saved = repository.save(rate);
        eventPublisher.publish(rate.pullEvents());
        return saved;
    }

    @Transactional(readOnly = true)
    public ShippingRate get(String rateId) {
        return repository.findById(rateId)
                .orElseThrow(() -> new ShippingException(ShippingErrorCode.RATE_NOT_FOUND));
    }

    private static int orDefault(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
