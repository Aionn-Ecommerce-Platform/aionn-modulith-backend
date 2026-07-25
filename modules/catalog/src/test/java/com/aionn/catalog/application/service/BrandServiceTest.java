package com.aionn.catalog.application.service;

import com.aionn.catalog.application.dto.brand.command.CreateBrandCommand;
import com.aionn.catalog.application.dto.brand.command.DeleteBrandCommand;
import com.aionn.catalog.application.dto.brand.command.UpdateBrandCommand;
import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.port.out.brand.BrandPersistencePort;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.model.Brand;
import com.aionn.catalog.domain.valueobject.BrandStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    private static final String BRAND_ID = "01HZBRD0000000000000000001";

    @Mock
    private BrandPersistencePort brandRepository;
    @Mock
    private EventPublisher eventPublisher;

    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

    @InjectMocks
    private BrandService brandService;

    @Test
    void createPersistsAndPublishesEvents() {
        when(brandRepository.existsByName("Acme")).thenReturn(false);
        when(brandRepository.save(any(Brand.class))).thenAnswer(inv -> inv.getArgument(0));

        Brand result = brandService.create(new CreateBrandCommand("Acme", null, "desc"));

        assertThat(result.getName()).isEqualTo("Acme");
        assertThat(result.getDescription()).isEqualTo("desc");
        ArgumentCaptor<Brand> captor = ArgumentCaptor.forClass(Brand.class);
        verify(brandRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Acme");
        verify(eventPublisher).publish(anyCollection());
    }

    @Test
    void createThrowsWhenNameAlreadyExists() {
        when(brandRepository.existsByName("Acme")).thenReturn(true);

        assertThatThrownBy(() -> brandService.create(new CreateBrandCommand("Acme", null, null)))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.BRAND_NAME_CONFLICT.getCode());

        verify(brandRepository, never()).save(any());
    }

    @Test
    void updateAppliesChangesAndSaves() {
        Brand brand = Brand.create(BRAND_ID, "Acme", null, null);
        brand.pullEvents();
        when(brandRepository.findById(BRAND_ID)).thenReturn(Optional.of(brand));
        when(brandRepository.existsByName("Acme New")).thenReturn(false);
        when(brandRepository.save(any(Brand.class))).thenAnswer(inv -> inv.getArgument(0));

        Brand result = brandService.update(new UpdateBrandCommand(BRAND_ID, "Acme New", "logo", "new desc"));

        assertThat(result.getName()).isEqualTo("Acme New");
        assertThat(result.getLogoUrl()).isEqualTo("logo");
        assertThat(result.getDescription()).isEqualTo("new desc");
    }

    @Test
    void updateRejectsWhenNewNameCollides() {
        Brand brand = Brand.create(BRAND_ID, "Acme", null, null);
        brand.pullEvents();
        when(brandRepository.findById(BRAND_ID)).thenReturn(Optional.of(brand));
        when(brandRepository.existsByName("Zenith")).thenReturn(true);

        assertThatThrownBy(() -> brandService.update(new UpdateBrandCommand(BRAND_ID, "Zenith", null, null)))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.BRAND_NAME_CONFLICT.getCode());

        verify(brandRepository, never()).save(any());
    }

    @Test
    void updateAllowsSameNameCaseInsensitive() {
        Brand brand = Brand.create(BRAND_ID, "Acme", null, null);
        brand.pullEvents();
        when(brandRepository.findById(BRAND_ID)).thenReturn(Optional.of(brand));
        when(brandRepository.save(any(Brand.class))).thenAnswer(inv -> inv.getArgument(0));

        brandService.update(new UpdateBrandCommand(BRAND_ID, "acme", null, null));

        verify(brandRepository, never()).existsByName(anyString());
    }

    @Test
    void updateThrowsWhenBrandNotFound() {
        when(brandRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.update(new UpdateBrandCommand("missing", "X", null, null)))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.BRAND_NOT_FOUND.getCode());
    }

    @Test
    void deleteSoftDeletesAndPublishesEvent() {
        Brand brand = Brand.create(BRAND_ID, "Acme", null, null);
        brand.pullEvents();
        when(brandRepository.findById(BRAND_ID)).thenReturn(Optional.of(brand));

        brandService.delete(new DeleteBrandCommand(BRAND_ID, "policy"));

        assertThat(brand.getStatus()).isEqualTo(BrandStatus.DELETED);
        verify(brandRepository).save(brand);
        verify(eventPublisher).publish(anyCollection());
    }

    @Test
    void deleteThrowsWhenBrandNotFound() {
        when(brandRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.delete(new DeleteBrandCommand("missing", "reason")))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.BRAND_NOT_FOUND.getCode());
    }

    @Test
    void getReturnsResultWhenBrandFound() {
        Brand brand = Brand.create(BRAND_ID, "Acme", null, null);
        when(brandRepository.findById(BRAND_ID)).thenReturn(Optional.of(brand));

        Brand result = brandService.get(BRAND_ID);

        assertThat(result.getBrandId()).isEqualTo(BRAND_ID);
        assertThat(result.getName()).isEqualTo("Acme");
    }

    @Test
    void getThrowsWhenBrandMissing() {
        when(brandRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.get("missing"))
                .isInstanceOf(CatalogException.class)
                .extracting("errorCode")
                .isEqualTo(CatalogErrorCode.BRAND_NOT_FOUND.getCode());
    }

    @Test
    void listReturnsPagedResults() {
        Brand brand = Brand.create(BRAND_ID, "Acme", null, null);
        when(brandRepository.list(any(OffsetPagination.class))).thenReturn(List.of(brand));
        when(brandRepository.count()).thenReturn(1L);

        PageResult<Brand> page = brandService.list(OffsetPagination.of(0, 20));

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).getName()).isEqualTo("Acme");
        assertThat(page.totalElements()).isEqualTo(1L);
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
    }
}
