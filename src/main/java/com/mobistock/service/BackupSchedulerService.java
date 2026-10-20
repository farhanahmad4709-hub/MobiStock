package com.mobistock.service;

import com.mobistock.model.BackupConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupSchedulerService {

    private final DatabaseBackupService databaseBackupService;

    /**
     * Nightly automatic backup at 23:00 (11:00 PM).
     */
    @Scheduled(cron = "${mobistock.backup.cron:0 0 23 * * ?}")
    public void runNightlyAutoBackup() {
        BackupConfig config = databaseBackupService.getOrCreateConfig();
        if (Boolean.TRUE.equals(config.getAutoBackupEnabled())) {
            log.info("Triggering scheduled automated nightly backup...");
            try {
                databaseBackupService.createBackup("AUTOMATIC", "SYSTEM");
            } catch (Exception e) {
                log.error("Scheduled automated backup failed: {}", e.getMessage());
            }
        }
    }

    /**
     * Periodic Google Drive sync for pending offline backups (every 30 minutes).
     */
    @Scheduled(fixedDelay = 1800000, initialDelay = 60000)
    public void syncPendingToGoogleDrive() {
        try {
            int synced = databaseBackupService.syncPendingBackups();
            if (synced > 0) {
                log.info("Synced {} pending backup(s) to Google Drive.", synced);
            }
        } catch (Exception e) {
            log.debug("Google Drive sync check completed: {}", e.getMessage());
        }
    }
}
