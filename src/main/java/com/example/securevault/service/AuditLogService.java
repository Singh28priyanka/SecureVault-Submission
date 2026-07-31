package com.example.securevault.service;

import com.example.securevault.entity.AuditLog;
import com.example.securevault.entity.User;
import com.example.securevault.exception.UserNotFoundException;
import com.example.securevault.repository.AuditLogRepository;
import com.example.securevault.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists audit entries. Methods join the caller's transaction so a failure
 * here rolls back the vault change.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public AuditLog record(String action,
                           String entityType,
                           Long entityId,
                           Long performedByUserId) {
        User performer = userRepository.findById(performedByUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        AuditLog auditLog = new AuditLog(action, entityType, entityId, performer);
        return auditLogRepository.save(auditLog);
    }
}
