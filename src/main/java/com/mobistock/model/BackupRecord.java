package com.mobistock.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "backup_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false, length = 500)
    private String filePath;

    private Long fileSizeBytes;

    @Column(nullable = false, length = 20)
    private String backupType; // MANUAL or AUTOMATIC

    @Column(length = 100)
    private String triggeredBy; // Username or "SYSTEM"

    @Column(nullable = false, length = 20)
    private String status; // SUCCESS or FAILED

    @Column(length = 30)
    private String googleDriveStatus; // UPLOADED, PENDING, NOT_CONFIGURED, FAILED

    @Column(length = 150)
    private String googleDriveFileId;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
