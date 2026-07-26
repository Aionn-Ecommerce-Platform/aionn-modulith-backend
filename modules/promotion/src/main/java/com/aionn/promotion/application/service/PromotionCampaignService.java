package com.aionn.promotion.application.service;

import com.aionn.promotion.application.dto.campaign.command.CampaignCommands;
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

    private final PromotionCampaignPersistencePort campaignRepository;
    private final VoucherPersistencePort voucherRepository;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public PromotionCampaign create(CampaignCommands.CreateCampaign command) {
        Money budget = Money.of(command.budget(), command.currency() == null ? "VND" : command.currency());
        PromotionCampaign c = PromotionCampaign.create(IdGenerator.ulid(),
                command.name(), command.type(), budget,
                command.startDate(), command.endDate(), command.createdBy(), clock);
        PromotionCampaign saved = campaignRepository.save(c);
        eventPublisher.publish(c.pullEvents());
        return saved;
    }

    public PromotionCampaign activate(CampaignCommands.ActivateCampaign command) {
        PromotionCampaign c = required(command.campaignId());
        c.activate(clock);
        PromotionCampaign saved = campaignRepository.save(c);
        eventPublisher.publish(c.pullEvents());
        return saved;
    }

    public PromotionCampaign end(CampaignCommands.EndCampaign command) {
        PromotionCampaign c = required(command.campaignId());
        c.end(clock);
        PromotionCampaign saved = campaignRepository.save(c);
        eventPublisher.publish(c.pullEvents());
        return saved;
    }

    public PromotionCampaign cancel(CampaignCommands.CancelCampaign command) {
        PromotionCampaign c = required(command.campaignId());
        c.cancel(command.reason(), clock);
        PromotionCampaign saved = campaignRepository.save(c);
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
        PromotionCampaign saved = campaignRepository.save(c);
        eventPublisher.publish(c.pullEvents());
        return saved;
    }

    public Voucher issueVoucher(CampaignCommands.IssueVoucher command) {
        PromotionCampaign campaign = required(command.campaignId());
        if (voucherRepository.findByCode(command.voucherCode()).isPresent()) {
            throw new PromotionException(PromotionErrorCode.VOUCHER_DUPLICATE_CODE);
        }
        Money discount = Money.of(command.discountAmount(),
                command.currency() == null ? campaign.getBudget().currency() : command.currency());
        Voucher v = Voucher.issue(command.voucherCode(), campaign.getCampaignId(),
                discount, command.usageLimit(), command.validFrom(), command.validUntil(), clock);
        Voucher saved = voucherRepository.save(v);
        eventPublisher.publish(v.pullEvents());
        return saved;
    }

    public int processScheduledTransitions(Instant now, int batchSize) {
        int changed = 0;
        for (PromotionCampaign c : campaignRepository.findToActivate(now, batchSize)) {
            try {
                c.activate(clock);
                campaignRepository.save(c);
                eventPublisher.publish(c.pullEvents());
                changed++;
            } catch (PromotionException ex) {
                log.warn("Skip activate for {}: {}", c.getCampaignId(), ex.getMessage());
            }
        }
        for (PromotionCampaign c : campaignRepository.findToEnd(now, batchSize)) {
            try {
                c.end(clock);
                campaignRepository.save(c);
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
        return campaignRepository.listByStatus(status, limit);
    }

    @Transactional(readOnly = true)
    public List<Voucher> listVouchersByCampaignId(String campaignId, int limit) {
        return voucherRepository.findByCampaignId(campaignId, limit);
    }

    private PromotionCampaign required(String campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.CAMPAIGN_NOT_FOUND));
    }
}
