package com.mobistock.service;

import com.mobistock.model.AuditLog;
import com.mobistock.model.User;
import com.mobistock.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository repository;

    @Transactional
    public void log(User user, String action, String entityType, Long entityId, String oldValues, String newValues) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setOldValues(oldValues);
        log.setNewValues(newValues);
        log.setCreatedAt(LocalDateTime.now());
        repository.save(log);
    }

    public List<AuditLog> getLogsForEntity(String entityType, Long entityId) {
        return repository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    public List<AuditLog> getAllLogs() {
        return repository.findAllByOrderByCreatedAtDesc();
    }
}
