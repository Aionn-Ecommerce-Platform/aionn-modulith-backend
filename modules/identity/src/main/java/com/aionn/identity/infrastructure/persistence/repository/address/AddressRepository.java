package com.aionn.identity.infrastructure.persistence.repository.address;

import com.aionn.identity.infrastructure.persistence.entity.UserAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<UserAddressEntity, String> {

    List<UserAddressEntity> findByUser_UserIdOrderByCreatedAtDesc(String userId);

    Optional<UserAddressEntity> findByAddressIdAndUser_UserId(String addressId, String userId);

    long countByUser_UserId(String userId);

    @Modifying
    @Query("UPDATE UserAddressEntity a SET a.isDefault = false WHERE a.user.userId = :userId AND a.isDefault = true")
    void clearDefaultAddressByUserId(@Param("userId") String userId);
}
