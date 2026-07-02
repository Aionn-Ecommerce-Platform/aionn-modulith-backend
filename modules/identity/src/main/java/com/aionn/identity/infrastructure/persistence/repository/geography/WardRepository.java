package com.aionn.identity.infrastructure.persistence.repository.geography;

import com.aionn.identity.infrastructure.persistence.entity.geography.WardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WardRepository extends JpaRepository<WardEntity, String> {

    List<WardEntity> findByDistrictCodeAndActiveTrue(String districtCode);

    Optional<WardEntity> findByCodeAndActiveTrue(String code);

    @Query("SELECT w FROM WardEntity w "
            + "JOIN FETCH w.district d "
            + "JOIN FETCH d.province p "
            + "WHERE w.code = :code AND w.active = true")
    Optional<WardEntity> findByCodeWithDistrictAndProvince(@Param("code") String code);
}
