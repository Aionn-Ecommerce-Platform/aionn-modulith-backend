package com.aionn.recommendation.infrastructure.persistence;

import com.aionn.recommendation.infrastructure.config.properties.RecommendationJobProperties;
import com.aionn.recommendation.infrastructure.persistence.adapter.InteractionPersistenceAdapter;
import com.aionn.recommendation.infrastructure.persistence.mapper.InteractionDomainMapper;
import com.aionn.recommendation.infrastructure.persistence.repository.InteractionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InteractionPersistenceAdapterTest {

    @Test
    void rejectsNullBeforeMappingOrWritingAnInteraction() {
        InteractionRepository repository = mock(InteractionRepository.class);
        InteractionDomainMapper mapper = mock(InteractionDomainMapper.class);
        Clock clock = mock(Clock.class);
        RecommendationJobProperties properties = mock(RecommendationJobProperties.class);
        when(properties.execution()).thenReturn(new RecommendationJobProperties.Execution(60, 500));
        InteractionPersistenceAdapter adapter = new InteractionPersistenceAdapter(
                repository, mapper, clock, mock(PlatformTransactionManager.class), properties);

        assertThatThrownBy(() -> adapter.append(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("interaction must not be null");

        verifyNoInteractions(repository, mapper, clock);
    }
}
