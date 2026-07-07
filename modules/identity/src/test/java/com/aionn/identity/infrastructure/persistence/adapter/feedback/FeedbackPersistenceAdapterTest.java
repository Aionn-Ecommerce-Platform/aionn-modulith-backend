package com.aionn.identity.infrastructure.persistence.adapter.feedback;

import com.aionn.identity.application.dto.common.PageResult;
import com.aionn.identity.domain.model.Feedback;
import com.aionn.identity.domain.valueobject.FeedbackCategory;
import com.aionn.identity.domain.valueobject.FeedbackStatus;
import com.aionn.identity.infrastructure.persistence.entity.UserFeedbackEntity;
import com.aionn.identity.infrastructure.persistence.mapper.FeedbackDomainMapper;
import com.aionn.identity.infrastructure.persistence.repository.feedback.UserFeedbackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackPersistenceAdapterTest {

    private static final String USER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String FEEDBACK_ID = "01HZFBK0000000000000000001";

    @Mock
    private UserFeedbackRepository repository;
    @Mock
    private FeedbackDomainMapper feedbackDomainMapper;

    @InjectMocks
    private FeedbackPersistenceAdapter adapter;

    private Feedback feedback() {
        return Feedback.createNew(FEEDBACK_ID, USER_ID, FeedbackCategory.BUG, "subject", "content",
                5, "a@b.com", "+84912345678");
    }

    @Test
    void saveMapsThroughEntityAndBack() {
        Feedback feedback = feedback();
        UserFeedbackEntity entity = mock(UserFeedbackEntity.class);
        when(feedbackDomainMapper.toEntity(feedback)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(feedbackDomainMapper.toDomain(entity)).thenReturn(feedback);

        assertThat(adapter.save(feedback)).isSameAs(feedback);
    }

    @Test
    void findByIdReturnsMappedWhenPresent() {
        UserFeedbackEntity entity = mock(UserFeedbackEntity.class);
        Feedback feedback = feedback();
        when(repository.findById(FEEDBACK_ID)).thenReturn(Optional.of(entity));
        when(feedbackDomainMapper.toDomain(entity)).thenReturn(feedback);

        assertThat(adapter.findById(FEEDBACK_ID)).contains(feedback);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        when(repository.findById(FEEDBACK_ID)).thenReturn(Optional.empty());

        assertThat(adapter.findById(FEEDBACK_ID)).isEmpty();
    }

    @Test
    void findByUserIdMapsResults() {
        UserFeedbackEntity entity = mock(UserFeedbackEntity.class);
        Feedback feedback = feedback();
        when(repository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(entity));
        when(feedbackDomainMapper.toDomain(entity)).thenReturn(feedback);

        assertThat(adapter.findByUserId(USER_ID)).containsExactly(feedback);
    }

    @Test
    void findAdminPageUsesStatusQueryWhenStatusProvided() {
        UserFeedbackEntity entity = mock(UserFeedbackEntity.class);
        Feedback feedback = feedback();
        when(repository.findByStatusOrderByCreatedAtDesc(eq(FeedbackStatus.OPEN), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1));
        when(feedbackDomainMapper.toDomain(entity)).thenReturn(feedback);

        PageResult<Feedback> result = adapter.findAdminPage(FeedbackStatus.OPEN, 0, 20);

        assertThat(result.content()).containsExactly(feedback);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void findAdminPageUsesUnfilteredQueryAndClampsPaging() {
        UserFeedbackEntity entity = mock(UserFeedbackEntity.class);
        Feedback feedback = feedback();
        when(repository.findAllByOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 100), 1));
        when(feedbackDomainMapper.toDomain(entity)).thenReturn(feedback);

        PageResult<Feedback> result = adapter.findAdminPage(null, -5, 500);

        assertThat(result.content()).containsExactly(feedback);
        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(repository).findAllByOrderByCreatedAtDesc(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }
}
