package com.aionn.ordering.infrastructure.persistence.repository;

import com.aionn.ordering.infrastructure.persistence.entity.CartEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface CartRepository extends JpaRepository<CartEntity, String> {

    @EntityGraph(attributePaths = "items")
    Optional<CartEntity> findByUserId(String userId);

    @Override
    @EntityGraph(attributePaths = "items")
    Optional<CartEntity> findById(String cartId);

    @Modifying
    @Query(value = """
            INSERT INTO carts (cart_id, user_id, created_at, updated_at, version)
            VALUES (:cartId, :userId, :now, :now, 0)
            ON CONFLICT (user_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("cartId") String cartId, @Param("userId") String userId,
            @Param("now") Instant now);
}

