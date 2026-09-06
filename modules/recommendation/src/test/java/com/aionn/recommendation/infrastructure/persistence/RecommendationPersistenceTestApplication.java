package com.aionn.recommendation.infrastructure.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Minimal Spring Boot configuration for slice tests in this module.
 *
 * <p>Needed because {@code @DataJpaTest} searches upwards for a {@code @SpringBootConfiguration} and
 * the only one in the repository lives in {@code app}, which no business module may depend on. Scoped
 * to persistence so a slice test never drags in the module's schedulers or REST layer.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan("com.aionn.recommendation.infrastructure.persistence.entity")
@EnableJpaRepositories("com.aionn.recommendation.infrastructure.persistence.repository")
class RecommendationPersistenceTestApplication {
}
