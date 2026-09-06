package com.aionn.recommendation.infrastructure.persistence.repository;

import com.aionn.recommendation.infrastructure.persistence.entity.UserAffinityProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAffinityProfileRepository
        extends JpaRepository<UserAffinityProfileEntity, String> {

    @Modifying
    @Query(value = "DELETE FROM recommendation_user_profiles WHERE user_id = :userId",
            nativeQuery = true)
    int deleteByUserId(@Param("userId") String userId);
}
