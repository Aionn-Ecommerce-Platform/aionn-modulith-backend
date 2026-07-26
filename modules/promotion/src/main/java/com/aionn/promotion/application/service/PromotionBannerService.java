package com.aionn.promotion.application.service;

import com.aionn.promotion.application.dto.banner.command.BannerCommands;
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

    public List<PromotionBanner> listActive() {
        return bannerRepository.findAllActive();
    }

    public List<PromotionBanner> listAll() {
        return bannerRepository.findAll();
    }

    public PromotionBanner get(String bannerId) {
        return required(bannerId);
    }

    @Transactional
    public PromotionBanner create(BannerCommands.CreateBanner command) {
        PromotionBanner banner = PromotionBanner.create(
                "BAN_" + IdGenerator.ulid(),
                command.title(),
                command.imageUrl(),
                command.linkUrl(),
                command.displayOrder(),
                command.active());
        return bannerRepository.save(banner);
    }

    @Transactional
    public PromotionBanner update(BannerCommands.UpdateBanner command) {
        PromotionBanner banner = required(command.bannerId());
        banner.update(command.title(), command.imageUrl(), command.linkUrl(),
                command.displayOrder(), command.active());
        return bannerRepository.save(banner);
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
