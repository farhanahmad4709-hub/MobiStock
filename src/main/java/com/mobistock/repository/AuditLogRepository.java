package com.mobistock.repository;

import com.mobistock.model.AuditLog;
import com.mobistock.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);
    List<AuditLog> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE AuditLog a SET a.user = :newUser WHERE a.user = :oldUser")
    void reassignLogs(@Param("oldUser") User oldUser, @Param("newUser") User newUser);
}

