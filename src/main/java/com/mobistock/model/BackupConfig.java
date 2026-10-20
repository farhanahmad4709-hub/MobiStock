package com.mobistock.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "backup_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    @Builder.Default
    private String localBackupDir = "D:\\MobiStock_Backups";

    @Builder.Default
    private Boolean autoBackupEnabled = true;

    @Column(length = 50)
    @Builder.Default
    private String backupSchedule = "DAILY_NIGHT"; // DAILY_NIGHT, EVERY_6_HOURS, EVERY_12_HOURS

    @Builder.Default
    private Boolean googleDriveEnabled = false;

    @Column(length = 50)
    @Builder.Default
    private String googleDriveMode = "DESKTOP_SYNC"; // DESKTOP_SYNC or SERVICE_ACCOUNT

    @Column(length = 500)
    private String googleDriveDesktopPath; // Path to Google Drive synced folder on PC

    @Column(length = 200)
    private String googleDriveFolderId; // Cloud folder ID if using API

    @Column(columnDefinition = "MEDIUMTEXT")
    private String googleServiceAccountJson; // Service account credentials JSON

    private LocalDateTime lastBackupAt;

    private LocalDateTime lastDriveUploadAt;
}
