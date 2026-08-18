package com.aionn.promotion.application.service;

import com.aionn.promotion.application.dto.banner.command.BannerCommands;
import com.aionn.promotion.application.port.out.PromotionBannerPersistencePort;
import com.aionn.promotion.domain.exception.PromotionException;
import com.aionn.promotion.domain.model.PromotionBanner;
import com.aionn.promotion.application.dto.common.PageResult;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionBannerServiceTest {

    @Mock
    private PromotionBannerPersistencePort bannerRepository;

    @InjectMocks
    private PromotionBannerService service;

    private static PromotionBanner banner(String id, boolean active) {
        return PromotionBanner.create(id, "Title " + id, "https://cdn/" + id + ".png",
                "aionn/promotion/banners/" + id, "https://shop/" + id, 1, active);
    }

    @Test
    void listActiveDelegatesToRepository() {
        var pagination = OffsetPagination.of(0, 20);
        when(bannerRepository.findAllActive(pagination))
                .thenReturn(new PageResult<>(List.of(banner("BAN_1", true)), 0, 20, 1));

        assertThat(service.listActive(pagination).content()).hasSize(1);
    }

    @Test
    void listAllDelegatesToRepository() {
        var pagination = OffsetPagination.of(0, 20);
        when(bannerRepository.findAll(pagination))
                .thenReturn(new PageResult<>(List.of(banner("BAN_1", true), banner("BAN_2", false)), 0, 20, 2));

        assertThat(service.listAll(pagination).content()).hasSize(2);
    }

    @Test
    void getReturnsBanner() {
        when(bannerRepository.findById("BAN_1")).thenReturn(Optional.of(banner("BAN_1", true)));

        assertThat(service.get("BAN_1").getBannerId()).isEqualTo("BAN_1");
    }

    @Test
    void getThrowsWhenMissing() {
        when(bannerRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("nope")).isInstanceOf(PromotionException.class);
    }

    @Test
    void createGeneratesIdAndSaves() {
        when(bannerRepository.save(any(PromotionBanner.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PromotionBanner saved = service.create(new BannerCommands.CreateBanner(
                "Summer", "https://cdn/a.png", "aionn/promotion/banners/a",
                "https://shop/sale", 3, true));

        ArgumentCaptor<PromotionBanner> captor = ArgumentCaptor.forClass(PromotionBanner.class);
        verify(bannerRepository).save(captor.capture());
        assertThat(captor.getValue().getBannerId()).startsWith("BAN_");
        assertThat(saved.getTitle()).isEqualTo("Summer");
        assertThat(saved.getDisplayOrder()).isEqualTo(3);
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void updateAppliesChangesAndSaves() {
        when(bannerRepository.findById("BAN_1")).thenReturn(Optional.of(banner("BAN_1", true)));
        when(bannerRepository.save(any(PromotionBanner.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PromotionBanner saved = service.update(new BannerCommands.UpdateBanner(
                "BAN_1", "Winter", null, null, null, 9, false));

        assertThat(saved.getTitle()).isEqualTo("Winter");
        assertThat(saved.getDisplayOrder()).isEqualTo(9);
        assertThat(saved.isActive()).isFalse();
    }

    @Test
    void updateThrowsWhenMissing() {
        when(bannerRepository.findById("nope")).thenReturn(Optional.empty());

        BannerCommands.UpdateBanner command = new BannerCommands.UpdateBanner(
                "nope", "x", null, null, null, null, null);

        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(PromotionException.class);
        verify(bannerRepository, never()).save(any());
    }

    @Test
    void deleteRemovesExistingBanner() {
        when(bannerRepository.findById("BAN_1")).thenReturn(Optional.of(banner("BAN_1", true)));

        service.delete(new BannerCommands.DeleteBanner("BAN_1"));

        verify(bannerRepository).deleteById("BAN_1");
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(bannerRepository.findById("nope")).thenReturn(Optional.empty());

        BannerCommands.DeleteBanner command = new BannerCommands.DeleteBanner("nope");

        assertThatThrownBy(() -> service.delete(command))
                .isInstanceOf(PromotionException.class);
        verify(bannerRepository, never()).deleteById(any());
    }
}
