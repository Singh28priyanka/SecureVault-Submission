package com.example.securevault.repository;

import com.example.securevault.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(
            String entityType,
            Long entityId
    );

    long countByEntityTypeAndEntityIdAndAction(
            String entityType,
            Long entityId,
            String action
    );
}
