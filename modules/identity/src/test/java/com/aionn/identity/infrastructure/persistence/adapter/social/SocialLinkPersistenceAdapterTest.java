package com.aionn.identity.infrastructure.persistence.adapter.social;

import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.SocialLink;
import com.aionn.identity.domain.valueobject.AuthProvider;
import com.aionn.identity.infrastructure.persistence.entity.SocialAccountEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.mapper.SocialLinkDomainMapper;
import com.aionn.identity.infrastructure.persistence.repository.auth.SocialAccountRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialLinkPersistenceAdapterTest {

    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String PROVIDER_USER_ID = "google-123";

    @Mock
    private SocialAccountRepository socialAccountRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SocialLinkDomainMapper socialLinkDomainMapper;

    @InjectMocks
    private SocialLinkPersistenceAdapter adapter;

    private SocialLink socialLink() {
        return SocialLink.createNew("01HZSOC0000000000000000001", USER_ID, AuthProvider.GOOGLE, PROVIDER_USER_ID);
    }

    @Test
    void findByProviderAndProviderUserIdReturnsMappedWhenPresent() {
        SocialAccountEntity entity = mock(SocialAccountEntity.class);
        SocialLink link = socialLink();
        when(socialAccountRepository.findByProviderAndProviderUserId("GOOGLE", PROVIDER_USER_ID))
                .thenReturn(Optional.of(entity));
        when(socialLinkDomainMapper.toDomain(entity)).thenReturn(link);

        assertThat(adapter.findByProviderAndProviderUserId(AuthProvider.GOOGLE, PROVIDER_USER_ID)).contains(link);
    }

    @Test
    void findByProviderAndProviderUserIdReturnsEmptyWhenMissing() {
        when(socialAccountRepository.findByProviderAndProviderUserId("GOOGLE", PROVIDER_USER_ID))
                .thenReturn(Optional.empty());

        assertThat(adapter.findByProviderAndProviderUserId(AuthProvider.GOOGLE, PROVIDER_USER_ID)).isEmpty();
    }

    @Test
    void findByUserIdAndProviderReturnsMappedWhenPresent() {
        SocialAccountEntity entity = mock(SocialAccountEntity.class);
        SocialLink link = socialLink();
        when(socialAccountRepository.findByUser_UserIdAndProvider(USER_ID, "GOOGLE"))
                .thenReturn(Optional.of(entity));
        when(socialLinkDomainMapper.toDomain(entity)).thenReturn(link);

        assertThat(adapter.findByUserIdAndProvider(USER_ID, AuthProvider.GOOGLE)).contains(link);
    }

    @Test
    void findByUserIdAndProviderReturnsEmptyWhenMissing() {
        when(socialAccountRepository.findByUser_UserIdAndProvider(USER_ID, "GOOGLE"))
                .thenReturn(Optional.empty());

        assertThat(adapter.findByUserIdAndProvider(USER_ID, AuthProvider.GOOGLE)).isEmpty();
    }

    @Test
    void existsByProviderAndProviderUserIdDelegates() {
        when(socialAccountRepository.existsByProviderAndProviderUserId("GOOGLE", PROVIDER_USER_ID)).thenReturn(true);

        assertThat(adapter.existsByProviderAndProviderUserId(AuthProvider.GOOGLE, PROVIDER_USER_ID)).isTrue();
    }

    @Test
    void saveAttachesUserAndMapsBack() {
        SocialLink link = socialLink();
        SocialAccountEntity entity = mock(SocialAccountEntity.class);
        UserEntity user = UserEntity.builder().userId(USER_ID).build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(socialLinkDomainMapper.toEntity(link, user)).thenReturn(entity);
        when(socialAccountRepository.save(entity)).thenReturn(entity);
        when(socialLinkDomainMapper.toDomain(entity)).thenReturn(link);

        assertThat(adapter.save(link, USER_ID)).isSameAs(link);
    }

    @Test
    void saveThrowsWhenUserMissing() {
        SocialLink link = socialLink();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.save(link, USER_ID))
                .isInstanceOfSatisfying(IdentityException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.USER_NOT_FOUND.getCode()));
        verify(socialAccountRepository, never()).save(any());
    }

    @Test
    void deleteByUserIdAndProviderDelegates() {
        adapter.deleteByUserIdAndProvider(USER_ID, AuthProvider.GOOGLE);

        verify(socialAccountRepository).deleteByUser_UserIdAndProvider(USER_ID, "GOOGLE");
    }
}
