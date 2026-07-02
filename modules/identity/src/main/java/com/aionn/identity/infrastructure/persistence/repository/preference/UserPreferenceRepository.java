package com.aionn.identity.infrastructure.persistence.repository.preference;

import com.aionn.identity.infrastructure.persistence.entity.UserPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceRepository extends JpaRepository<UserPreferenceEntity, String> {
}



