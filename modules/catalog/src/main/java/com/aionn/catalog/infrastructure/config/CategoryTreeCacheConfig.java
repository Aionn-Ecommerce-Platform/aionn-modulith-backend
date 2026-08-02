package com.aionn.catalog.infrastructure.config;

import com.aionn.catalog.application.dto.category.result.CategoryTreeNode;
import com.aionn.catalog.infrastructure.config.properties.CategoryTreeCacheProperties;
import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCache;
import com.aionn.sharedkernel.infrastructure.cache.factory.TwoTierCacheFactory;
import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCacheProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class CategoryTreeCacheConfig {

        @Bean(name = "catalogCategoryTreeCache")
        public TwoTierCache<String, List<CategoryTreeNode>> catalogCategoryTreeCache(
                        TwoTierCacheFactory factory,
                        CategoryTreeCacheProperties properties) {
                return factory.create(
                                new TwoTierCacheProperties(
                                                "catalog.category.tree",
                                                Duration.ofSeconds(properties.l1TtlSeconds()),
                                                properties.l1MaxSize(),
                                                Duration.ofSeconds(properties.l2TtlSeconds())),
                                new TypeReference<>() {
                                });
        }
}
