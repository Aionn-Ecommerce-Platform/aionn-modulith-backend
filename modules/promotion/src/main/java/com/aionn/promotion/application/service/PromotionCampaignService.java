package com.aionn.promotion.application.service;

import com.aionn.promotion.application.dto.campaign.command.CampaignCommands;
import com.aionn.promotion.application.policy.PromotionDefaultsPolicy;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.promotion.application.port.out.PromotionCampaignPersistencePort;
import com.aionn.promotion.application.port.out.VoucherPersistencePort;
import com.aionn.promotion.domain.exception.PromotionErrorCode;
import com.aionn.promotion.domain.exception.PromotionException;
import com.aionn.promotion.domain.model.PromotionCampaign;
import com.aionn.promotion.domain.model.Voucher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.promotion.domain.valueobject.PromotionCondition;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PromotionCampaignService {

    private final PromotionCampaignPersistencePort promotionCampaignPersistencePort;
    private final VoucherPersistencePort voucherPersistencePort;
    private final EventPublisher eventPublisher;
    private final PromotionDefaultsPolicy defaultsPolicy;
    private final Clock clock;

    public PromotionCampaign create(CampaignCommands.CreateCampaign command) {
        Money budget = Money.of(command.budget(),
                command.currency() == null ? defaultsPolicy.defaultCurrency() : command.currency());
        PromotionCampaign c = PromotionCampaign.create(IdGenerator.ulid(),
                command.name(), command.type(), budget,
                command.startDate(), command.endDate(), command.createdBy(), clock);
        PromotionCampaign saved = promotionCampaignPersistencePort.save(c);
        eventPublisher.publish(c.pullEvents());
        return saved;
    }

    public PromotionCampaign activate(CampaignCommands.ActivateCampaign command) {
        PromotionCampaign c = required(command.campaignId());
        c.activate(clock);
        PromotionCampaign saved = promotionCampaignPersistencePort.save(c);
        eventPublisher.publish(c.pullEvents());
        return saved;
    }

    public PromotionCampaign end(CampaignCommands.EndCampaign command) {
        PromotionCampaign c = required(command.campaignId());
        c.end(clock);
        PromotionCampaign saved = promotionCampaignPersistencePort.save(c);
        eventPublisher.publish(c.pullEvents());
        return saved;
    }

    public PromotionCampaign cancel(CampaignCommands.CancelCampaign command) {
        PromotionCampaign c = required(command.campaignId());
        c.cancel(command.reason(), clock);
        PromotionCampaign saved = promotionCampaignPersistencePort.save(c);
        eventPublisher.publish(c.pullEvents());
        return saved;
    }

    public PromotionCampaign configureCondition(CampaignCommands.ConfigureCondition command) {
        PromotionCampaign c = required(command.campaignId());
        PromotionCondition condition = new PromotionCondition(
                command.minOrderValue(),
                command.applicableCategoryIds(),
                command.maxClaimsPerUser(),
                command.maxUsesPerVoucher());
        c.configureCondition(condition, clock);
        PromotionCampaign saved = promotionCampaignPersistencePort.save(c);
        eventPublisher.publish(c.pullEvents());
        return saved;
    }

    public Voucher issueVoucher(CampaignCommands.IssueVoucher command) {
        PromotionCampaign campaign = required(command.campaignId());
        if (voucherPersistencePort.findByCode(command.voucherCode()).isPresent()) {
            throw new PromotionException(PromotionErrorCode.VOUCHER_DUPLICATE_CODE);
        }
        Money discount = Money.of(command.discountAmount(),
                command.currency() == null ? campaign.getBudget().currency() : command.currency());
        if (!discount.currency().equalsIgnoreCase(campaign.getBudget().currency())) {
            throw new PromotionException(PromotionErrorCode.INVALID_ARGUMENT,
                    "Voucher currency must match the campaign budget currency");
        }
        Voucher v = Voucher.issue(command.voucherCode(), campaign.getCampaignId(), command.type(),
                discount, command.usageLimit(), command.validFrom(), command.validUntil(), clock);
        Voucher saved;
        try {
            saved = voucherPersistencePort.save(v);
        } catch (DataIntegrityViolationException ex) {
            throw new PromotionException(PromotionErrorCode.VOUCHER_DUPLICATE_CODE);
        }
        eventPublisher.publish(v.pullEvents());
        return saved;
    }

    public int processScheduledTransitions(Instant now, int batchSize) {
        int changed = 0;
        for (PromotionCampaign c : promotionCampaignPersistencePort.findToActivate(now, batchSize)) {
            try {
                c.activate(clock);
                promotionCampaignPersistencePort.save(c);
                eventPublisher.publish(c.pullEvents());
                changed++;
            } catch (PromotionException ex) {
                log.warn("Skip activate for {}: {}", c.getCampaignId(), ex.getMessage());
            }
        }
        for (PromotionCampaign c : promotionCampaignPersistencePort.findToEnd(now, batchSize)) {
            try {
                c.end(clock);
                promotionCampaignPersistencePort.save(c);
                eventPublisher.publish(c.pullEvents());
                changed++;
            } catch (PromotionException ex) {
                log.warn("Skip end for {}: {}", c.getCampaignId(), ex.getMessage());
            }
        }
        return changed;
    }

    @Transactional(readOnly = true)
    public PromotionCampaign get(String campaignId) {
        return required(campaignId);
    }

    @Transactional(readOnly = true)
    public List<PromotionCampaign> listByStatus(String status, int limit) {
        return promotionCampaignPersistencePort.listByStatus(status, limit);
    }

    @Transactional(readOnly = true)
    public List<Voucher> listVouchersByCampaignId(String campaignId, int limit) {
        return voucherPersistencePort.findByCampaignId(campaignId, limit);
    }

    private PromotionCampaign required(String campaignId) {
        return promotionCampaignPersistencePort.findById(campaignId)
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.CAMPAIGN_NOT_FOUND));
    }
}
