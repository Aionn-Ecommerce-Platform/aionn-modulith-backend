package com.aionn.catalog.infrastructure.config;

import com.aionn.catalog.application.dto.category.result.CategoryTreeNode;
import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCache;
import com.aionn.sharedkernel.infrastructure.cache.factory.TwoTierCacheFactory;
import com.aionn.sharedkernel.infrastructure.cache.core.TwoTierCacheProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class CategoryTreeCacheConfig {

        @Bean(name = "catalogCategoryTreeCache")
        public TwoTierCache<String, List<CategoryTreeNode>> catalogCategoryTreeCache(
                        TwoTierCacheFactory factory,
                        @Value("${catalog.cache.category-tree.l1-ttl-seconds:60}") long l1TtlSeconds,
                        @Value("${catalog.cache.category-tree.l1-max-size:10}") long l1MaxSize,
                        @Value("${catalog.cache.category-tree.l2-ttl-seconds:300}") long l2TtlSeconds) {
                return factory.create(
                                new TwoTierCacheProperties(
                                                "catalog.category.tree",
                                                Duration.ofSeconds(l1TtlSeconds),
                                                l1MaxSize,
                                                Duration.ofSeconds(l2TtlSeconds)),
                                new TypeReference<>() {
                                });
        }
}
