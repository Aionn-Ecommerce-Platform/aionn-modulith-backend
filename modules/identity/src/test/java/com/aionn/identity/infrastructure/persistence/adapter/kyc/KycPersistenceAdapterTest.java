package com.aionn.identity.infrastructure.persistence.adapter.kyc;

import com.aionn.identity.domain.model.KycDocument;
import com.aionn.identity.domain.model.KycProfile;
import com.aionn.identity.domain.valueobject.KycStatus;
import com.aionn.identity.infrastructure.persistence.entity.KycDocumentEntity;
import com.aionn.identity.infrastructure.persistence.entity.KycProfileEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.mapper.KycDocumentDomainMapper;
import com.aionn.identity.infrastructure.persistence.mapper.KycDomainMapper;
import com.aionn.identity.infrastructure.persistence.repository.kyc.KycDocumentRepository;
import com.aionn.identity.infrastructure.persistence.repository.kyc.KycProfileRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KycPersistenceAdapterTest {

    private static final String KYC_ID = "01HZKYC0000000000000000001";
    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    @Mock
    private KycProfileRepository kycRepository;
    @Mock
    private KycDocumentRepository kycDocumentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private KycDomainMapper mapper;
    @Mock
    private KycDocumentDomainMapper documentMapper;

    @InjectMocks
    private KycPersistenceAdapter adapter;

    private KycProfile profile() {
        KycProfile profile = mock(KycProfile.class);
        lenient().when(profile.getKycId()).thenReturn(KYC_ID);
        return profile;
    }

    @Test
    void saveUpdatesManagedEntityWhenPresent() {
        KycProfile profile = profile();
        KycProfileEntity existing = mock(KycProfileEntity.class);
        when(kycRepository.findById(KYC_ID)).thenReturn(Optional.of(existing));
        when(kycRepository.save(existing)).thenReturn(existing);
        when(mapper.toDomain(existing)).thenReturn(profile);

        assertThat(adapter.save(profile)).isSameAs(profile);
        verify(mapper).updateEntity(existing, profile);
    }

    @Test
    void saveInsertsNewEntityWithUserReferenceWhenAbsent() {
        KycProfile profile = profile();
        when(profile.getUserId()).thenReturn(USER_ID);
        KycProfileEntity entity = mock(KycProfileEntity.class);
        UserEntity userRef = UserEntity.builder().userId(USER_ID).build();
        when(kycRepository.findById(KYC_ID)).thenReturn(Optional.empty());
        when(mapper.toEntity(profile)).thenReturn(entity);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(userRef);
        when(kycRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(profile);

        assertThat(adapter.save(profile)).isSameAs(profile);
        verify(entity).setUser(userRef);
    }

    @Test
    void findByKycIdAndUserIdReturnsMappedWhenPresent() {
        KycProfileEntity entity = mock(KycProfileEntity.class);
        KycProfile profile = profile();
        when(kycRepository.findByKycIdAndUser_UserId(KYC_ID, USER_ID)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(profile);

        assertThat(adapter.findByKycIdAndUserId(KYC_ID, USER_ID)).contains(profile);
    }

    @Test
    void findByKycIdAndUserIdReturnsEmptyWhenMissing() {
        when(kycRepository.findByKycIdAndUser_UserId(KYC_ID, USER_ID)).thenReturn(Optional.empty());

        assertThat(adapter.findByKycIdAndUserId(KYC_ID, USER_ID)).isEmpty();
    }

    @Test
    void findByIdReturnsMappedWhenPresent() {
        KycProfileEntity entity = mock(KycProfileEntity.class);
        KycProfile profile = profile();
        when(kycRepository.findById(KYC_ID)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(profile);

        assertThat(adapter.findById(KYC_ID)).contains(profile);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        when(kycRepository.findById(KYC_ID)).thenReturn(Optional.empty());

        assertThat(adapter.findById(KYC_ID)).isEmpty();
    }

    @Test
    void findByProviderApplicantIdReturnsMappedWhenPresent() {
        KycProfileEntity entity = mock(KycProfileEntity.class);
        KycProfile profile = profile();
        when(kycRepository.findByProviderApplicantId("app-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(profile);

        assertThat(adapter.findByProviderApplicantId("app-1")).contains(profile);
    }

    @Test
    void findByProviderApplicantIdReturnsEmptyWhenMissing() {
        when(kycRepository.findByProviderApplicantId("app-1")).thenReturn(Optional.empty());

        assertThat(adapter.findByProviderApplicantId("app-1")).isEmpty();
    }

    @Test
    void findByUserIdOrderBySubmittedAtDescMapsResults() {
        KycProfileEntity entity = mock(KycProfileEntity.class);
        KycProfile profile = profile();
        when(kycRepository.findByUser_UserIdOrderBySubmittedAtDesc(USER_ID)).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(profile);

        assertThat(adapter.findByUserIdOrderBySubmittedAtDesc(USER_ID)).containsExactly(profile);
    }

    @Test
    void findByStatusClampsLimitAndMapsResults() {
        KycProfileEntity entity = mock(KycProfileEntity.class);
        KycProfile profile = profile();
        when(kycRepository.findByStatusOrderBySubmittedAtDesc(eq("SUBMITTED"), any(PageRequest.class)))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(profile);

        assertThat(adapter.findByStatus(KycStatus.SUBMITTED, 500)).containsExactly(profile);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(kycRepository).findByStatusOrderBySubmittedAtDesc(eq("SUBMITTED"), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(200);
        assertThat(captor.getValue().getPageNumber()).isZero();
    }

    @Test
    void saveDocumentAttachesKycReferenceAndMapsBack() {
        KycDocument document = mock(KycDocument.class);
        when(document.getKycId()).thenReturn(KYC_ID);
        KycDocumentEntity entity = mock(KycDocumentEntity.class);
        KycProfileEntity kycRef = mock(KycProfileEntity.class);
        when(documentMapper.toEntity(document)).thenReturn(entity);
        when(kycRepository.getReferenceById(KYC_ID)).thenReturn(kycRef);
        when(kycDocumentRepository.save(entity)).thenReturn(entity);
        when(documentMapper.toDomain(entity)).thenReturn(document);

        assertThat(adapter.saveDocument(document)).isSameAs(document);
        verify(entity).setKyc(kycRef);
    }

    @Test
    void findDocumentsByKycIdMapsResults() {
        KycDocumentEntity entity = mock(KycDocumentEntity.class);
        KycDocument document = mock(KycDocument.class);
        when(kycDocumentRepository.findByKyc_KycIdOrderByUploadedAtAsc(KYC_ID)).thenReturn(List.of(entity));
        when(documentMapper.toDomain(entity)).thenReturn(document);

        assertThat(adapter.findDocumentsByKycId(KYC_ID)).containsExactly(document);
    }

    @Test
    void deleteRemovesByKycId() {
        KycProfile profile = profile();

        adapter.delete(profile);

        verify(kycRepository).deleteById(KYC_ID);
    }
}
