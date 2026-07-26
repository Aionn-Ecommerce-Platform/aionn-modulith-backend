package com.aionn.promotion.application.service;

import com.aionn.promotion.application.dto.campaign.command.CampaignCommands;
import com.aionn.promotion.application.dto.campaign.result.CampaignResult;
import com.aionn.promotion.application.dto.voucher.result.VoucherResult;
import com.aionn.promotion.application.mapper.PromotionResultMapper;
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

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PromotionCampaignService {

    private final PromotionCampaignPersistencePort campaignRepository;
    private final VoucherPersistencePort voucherRepository;
    private final PromotionResultMapper mapper;
    private final EventPublisher eventPublisher;

    public CampaignResult create(CampaignCommands.CreateCampaign command) {
        Money budget = Money.of(command.budget(), command.currency() == null ? "VND" : command.currency());
        PromotionCampaign c = PromotionCampaign.create(IdGenerator.ulid(),
                command.name(), command.type(), budget,
                command.startDate(), command.endDate(), command.createdBy());
        PromotionCampaign saved = campaignRepository.save(c);
        eventPublisher.publish(c.pullEvents());
        return mapper.toResult(saved);
    }

    public CampaignResult activate(CampaignCommands.ActivateCampaign command) {
        PromotionCampaign c = required(command.campaignId());
        c.activate();
        PromotionCampaign saved = campaignRepository.save(c);
        eventPublisher.publish(c.pullEvents());
        return mapper.toResult(saved);
    }

    public CampaignResult end(CampaignCommands.EndCampaign command) {
        PromotionCampaign c = required(command.campaignId());
        c.end();
        PromotionCampaign saved = campaignRepository.save(c);
        eventPublisher.publish(c.pullEvents());
        return mapper.toResult(saved);
    }

    public CampaignResult cancel(CampaignCommands.CancelCampaign command) {
        PromotionCampaign c = required(command.campaignId());
        c.cancel(command.reason());
        PromotionCampaign saved = campaignRepository.save(c);
        eventPublisher.publish(c.pullEvents());
        return mapper.toResult(saved);
    }

    public CampaignResult configureCondition(CampaignCommands.ConfigureCondition command) {
        PromotionCampaign c = required(command.campaignId());
        PromotionCondition condition = new PromotionCondition(
                command.minOrderValue(),
                command.applicableCategoryIds(),
                command.maxClaimsPerUser(),
                command.maxUsesPerVoucher());
        c.configureCondition(condition);
        PromotionCampaign saved = campaignRepository.save(c);
        eventPublisher.publish(c.pullEvents());
        return mapper.toResult(saved);
    }

    public VoucherResult issueVoucher(CampaignCommands.IssueVoucher command) {
        PromotionCampaign campaign = required(command.campaignId());
        if (voucherRepository.findByCode(command.voucherCode()).isPresent()) {
            throw new PromotionException(PromotionErrorCode.VOUCHER_DUPLICATE_CODE);
        }
        Money discount = Money.of(command.discountAmount(),
                command.currency() == null ? campaign.getBudget().currency() : command.currency());
        Voucher v = Voucher.issue(command.voucherCode(), campaign.getCampaignId(),
                discount, command.usageLimit(), command.validFrom(), command.validUntil());
        Voucher saved = voucherRepository.save(v);
        eventPublisher.publish(v.pullEvents());
        return mapper.toResult(saved);
    }

    public int processScheduledTransitions(Instant now, int batchSize) {
        int changed = 0;
        for (PromotionCampaign c : campaignRepository.findToActivate(now, batchSize)) {
            try {
                c.activate();
                campaignRepository.save(c);
                eventPublisher.publish(c.pullEvents());
                changed++;
            } catch (PromotionException ex) {
                log.warn("Skip activate for {}: {}", c.getCampaignId(), ex.getMessage());
            }
        }
        for (PromotionCampaign c : campaignRepository.findToEnd(now, batchSize)) {
            try {
                c.end();
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
    public CampaignResult get(String campaignId) {
        return mapper.toResult(required(campaignId));
    }

    @Transactional(readOnly = true)
    public List<CampaignResult> listByStatus(String status, int limit) {
        return campaignRepository.listByStatus(status, limit).stream()
                .map(mapper::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VoucherResult> listVouchersByCampaignId(String campaignId, int limit) {
        return voucherRepository.findByCampaignId(campaignId, limit).stream()
                .map(mapper::toResult)
                .toList();
    }

    private PromotionCampaign required(String campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.CAMPAIGN_NOT_FOUND));
    }
}
