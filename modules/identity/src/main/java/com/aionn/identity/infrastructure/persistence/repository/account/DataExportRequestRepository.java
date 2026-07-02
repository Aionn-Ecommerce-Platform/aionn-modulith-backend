package com.aionn.identity.infrastructure.persistence.repository.account;

import com.aionn.identity.domain.valueobject.DataExportStatus;
import com.aionn.identity.infrastructure.persistence.entity.DataExportRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface DataExportRequestRepository extends JpaRepository<DataExportRequestEntity, String> {

    boolean existsByUser_UserIdAndStatusIn(String userId, Collection<DataExportStatus> statuses);
}


