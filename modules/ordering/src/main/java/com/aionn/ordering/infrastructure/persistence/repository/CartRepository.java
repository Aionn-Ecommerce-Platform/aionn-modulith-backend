package com.aionn.ordering.infrastructure.persistence.repository;

import com.aionn.ordering.infrastructure.persistence.entity.CartEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<CartEntity, String> {

    @EntityGraph(attributePaths = "items")
    Optional<CartEntity> findByUserId(String userId);

    @Override
    @EntityGraph(attributePaths = "items")
    Optional<CartEntity> findById(String cartId);
}

