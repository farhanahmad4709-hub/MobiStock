package com.mobistock.repository;

import com.mobistock.model.BackupRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BackupRecordRepository extends JpaRepository<BackupRecord, Long> {
    List<BackupRecord> findAllByOrderByCreatedAtDesc();
    List<BackupRecord> findByGoogleDriveStatus(String googleDriveStatus);
}
