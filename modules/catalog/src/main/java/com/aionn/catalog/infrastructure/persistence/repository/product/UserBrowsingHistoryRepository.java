package com.aionn.catalog.infrastructure.persistence.repository.product;

import com.aionn.catalog.infrastructure.persistence.entity.UserBrowsingHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBrowsingHistoryRepository extends JpaRepository<UserBrowsingHistoryEntity, String> {
}
