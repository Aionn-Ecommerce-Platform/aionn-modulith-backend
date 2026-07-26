package com.aionn.promotion.application.service;

import com.aionn.promotion.application.dto.banner.command.BannerCommands;
import com.aionn.promotion.application.dto.banner.result.PromotionBannerResult;
import com.aionn.promotion.application.mapper.PromotionResultMapper;
import com.aionn.promotion.application.port.out.PromotionBannerPersistencePort;
import com.aionn.promotion.domain.exception.PromotionErrorCode;
import com.aionn.promotion.domain.exception.PromotionException;
import com.aionn.promotion.domain.model.PromotionBanner;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromotionBannerService {

    private final PromotionBannerPersistencePort bannerRepository;
    private final PromotionResultMapper mapper;

    public List<PromotionBannerResult> listActive() {
        return bannerRepository.findAllActive().stream()
                .map(mapper::toResult)
                .toList();
    }

    public List<PromotionBannerResult> listAll() {
        return bannerRepository.findAll().stream()
                .map(mapper::toResult)
                .toList();
    }

    public PromotionBannerResult get(String bannerId) {
        return mapper.toResult(required(bannerId));
    }

    @Transactional
    public PromotionBannerResult create(BannerCommands.CreateBanner command) {
        PromotionBanner banner = PromotionBanner.create(
                "BAN_" + IdGenerator.ulid(),
                command.title(),
                command.imageUrl(),
                command.linkUrl(),
                command.displayOrder(),
                command.active());
        return mapper.toResult(bannerRepository.save(banner));
    }

    @Transactional
    public PromotionBannerResult update(BannerCommands.UpdateBanner command) {
        PromotionBanner banner = required(command.bannerId());
        banner.update(command.title(), command.imageUrl(), command.linkUrl(),
                command.displayOrder(), command.active());
        return mapper.toResult(bannerRepository.save(banner));
    }

    @Transactional
    public void delete(BannerCommands.DeleteBanner command) {
        required(command.bannerId());
        bannerRepository.deleteById(command.bannerId());
    }

    private PromotionBanner required(String bannerId) {
        return bannerRepository.findById(bannerId)
                .orElseThrow(() -> new PromotionException(PromotionErrorCode.BANNER_NOT_FOUND));
    }
}
