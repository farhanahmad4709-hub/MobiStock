package com.mobistock.service;

import com.mobistock.model.BackupConfig;
import com.mobistock.model.BackupRecord;
import com.mobistock.repository.BackupConfigRepository;
import com.mobistock.repository.BackupRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseBackupService {

    private final DataSource dataSource;
    private final BackupRecordRepository backupRecordRepository;
    private final BackupConfigRepository backupConfigRepository;
    private final GoogleDriveService googleDriveService;
    private final AuditService auditService;

    public synchronized BackupConfig getOrCreateConfig() {
        return backupConfigRepository.findAll().stream().findFirst().orElseGet(() -> {
            BackupConfig defaultCfg = BackupConfig.builder()
                    .localBackupDir("D:\\MobiStock_Backups")
                    .autoBackupEnabled(true)
                    .backupSchedule("DAILY_NIGHT")
                    .googleDriveEnabled(false)
                    .googleDriveMode("DESKTOP_SYNC")
                    .googleDriveDesktopPath("G:\\My Drive\\MobiStock_Backups")
                    .build();
            return backupConfigRepository.save(defaultCfg);
        });
    }

    public synchronized BackupConfig updateConfigValues(
            String localBackupDir,
            boolean autoBackupEnabled,
            String backupSchedule,
            boolean googleDriveEnabled,
            String googleDriveMode,
            String googleDriveDesktopPath,
            String googleDriveFolderId) {
        BackupConfig current = getOrCreateConfig();
        if (localBackupDir != null && !localBackupDir.trim().isEmpty()) {
            current.setLocalBackupDir(localBackupDir.trim());
        }
        current.setAutoBackupEnabled(autoBackupEnabled);
        if (backupSchedule != null && !backupSchedule.trim().isEmpty()) {
            current.setBackupSchedule(backupSchedule.trim());
        }
        current.setGoogleDriveEnabled(googleDriveEnabled);
        if (googleDriveMode != null && !googleDriveMode.trim().isEmpty()) {
            current.setGoogleDriveMode(googleDriveMode.trim());
        }
        current.setGoogleDriveDesktopPath(googleDriveDesktopPath != null ? googleDriveDesktopPath.trim() : null);
        current.setGoogleDriveFolderId(googleDriveFolderId != null ? googleDriveFolderId.trim() : null);
        return backupConfigRepository.save(current);
    }

    public synchronized BackupConfig updateConfig(BackupConfig newConfig) {
        BackupConfig current = getOrCreateConfig();
        current.setLocalBackupDir(newConfig.getLocalBackupDir());
        current.setAutoBackupEnabled(newConfig.getAutoBackupEnabled());
        current.setBackupSchedule(newConfig.getBackupSchedule());
        current.setGoogleDriveEnabled(newConfig.getGoogleDriveEnabled());
        current.setGoogleDriveMode(newConfig.getGoogleDriveMode());
        current.setGoogleDriveDesktopPath(newConfig.getGoogleDriveDesktopPath());
        current.setGoogleDriveFolderId(newConfig.getGoogleDriveFolderId());
        if (newConfig.getGoogleServiceAccountJson() != null && !newConfig.getGoogleServiceAccountJson().trim().isEmpty()) {
            current.setGoogleServiceAccountJson(newConfig.getGoogleServiceAccountJson());
        }
        return backupConfigRepository.save(current);
    }

    public List<BackupRecord> getAllBackups() {
        return backupRecordRepository.findAllByOrderByCreatedAtDesc();
    }

    public BackupRecord getBackupById(Long id) {
        return backupRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Backup record not found for ID: " + id));
    }

    /**
     * Creates a full SQL backup to the designated secondary drive,
     * then attempts to sync it to Google Drive.
     */
    public synchronized BackupRecord createBackup(String backupType, String triggeredBy) {
        BackupConfig config = getOrCreateConfig();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = "mobistock_backup_" + timestamp + ".sql";

        // Determine local directory (Target: D:\MobiStock_Backups, Fallback: ./backups)
        Path targetDir = resolveBackupDirectory(config.getLocalBackupDir());
        Path targetFilePath = targetDir.resolve(filename);

        BackupRecord record = BackupRecord.builder()
                .filename(filename)
                .filePath(targetFilePath.toString())
                .backupType(backupType != null ? backupType : "MANUAL")
                .triggeredBy(triggeredBy != null ? triggeredBy : "SYSTEM")
                .status("PENDING")
                .googleDriveStatus("NOT_CONFIGURED")
                .build();

        try {
            // Generate full database SQL dump
            dumpDatabaseToSqlFile(targetFilePath);

            long fileSize = Files.size(targetFilePath);
            record.setFileSizeBytes(fileSize);
            record.setStatus("SUCCESS");

            // Update last backup time
            config.setLastBackupAt(LocalDateTime.now());
            backupConfigRepository.save(config);

            // Attempt Google Drive Upload / Sync
            if (Boolean.TRUE.equals(config.getGoogleDriveEnabled())) {
                boolean uploaded = googleDriveService.uploadBackupFile(targetFilePath.toFile(), record, config);
                if (uploaded) {
                    config.setLastDriveUploadAt(LocalDateTime.now());
                    backupConfigRepository.save(config);
                }
            } else {
                record.setGoogleDriveStatus("NOT_CONFIGURED");
            }

            backupRecordRepository.save(record);
            auditService.log(null, "DATABASE_BACKUP_CREATED", "BACKUP", record.getId(), null, "Created backup: " + filename + " (" + (fileSize / 1024) + " KB)");
            log.info("Database backup created successfully: {}", targetFilePath);
            return record;
        } catch (Exception e) {
            log.error("Failed to generate database backup: {}", e.getMessage(), e);
            record.setStatus("FAILED");
            record.setErrorMessage("Backup creation failed: " + e.getMessage());
            backupRecordRepository.save(record);
            throw new RuntimeException("Database backup failed: " + e.getMessage(), e);
        }
    }

    /**
     * Resolves the backup directory. If D:\ cannot be written or doesn't exist,
     * falls back safely to ./backups in the application directory.
     */
    private Path resolveBackupDirectory(String configuredPath) {
        if (configuredPath != null && !configuredPath.trim().isEmpty()) {
            try {
                Path p = Paths.get(configuredPath.trim());
                if (!Files.exists(p)) {
                    Files.createDirectories(p);
                }
                // Test write permissions
                Path testFile = p.resolve(".test_write");
                Files.writeString(testFile, "test");
                Files.deleteIfExists(testFile);
                return p;
            } catch (Exception e) {
                log.warn("Could not write to configured backup path '{}': {}. Falling back to ./backups", configuredPath, e.getMessage());
            }
        }

        // Fallback: local ./backups folder
        try {
            Path fallback = Paths.get("backups");
            if (!Files.exists(fallback)) {
                Files.createDirectories(fallback);
            }
            return fallback;
        } catch (IOException ex) {
            throw new RuntimeException("Could not create local fallback backups folder: " + ex.getMessage(), ex);
        }
    }

    /**
     * Pure Java JDBC Database Dumper.
     * Safely reads all tables and row data and generates standard SQL dump.
     */
    private void dumpDatabaseToSqlFile(Path outputFile) throws Exception {
        try (Connection conn = dataSource.getConnection();
             BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {

            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();

            writer.write("-- ========================================================\n");
            writer.write("-- MobiStock Database Full Backup\n");
            writer.write("-- Generated at: " + LocalDateTime.now() + "\n");
            writer.write("-- Database: " + catalog + "\n");
            writer.write("-- ========================================================\n\n");
            writer.write("SET FOREIGN_KEY_CHECKS=0;\n");
            writer.write("SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';\n\n");

            // List of tables in proper dependency order
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = metaData.getTables(catalog, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    tables.add(tableName);
                }
            }

            for (String table : tables) {
                if ("backup_config".equalsIgnoreCase(table)) {
                    continue; // Skip backup_config so database restore operations don't wipe active system configuration
                }
                writer.write("-- --------------------------------------------------------\n");
                writer.write("-- Table structure for table `" + table + "`\n");
                writer.write("-- --------------------------------------------------------\n");
                writer.write("DROP TABLE IF EXISTS `" + table + "`;\n");

                // Get CREATE TABLE statement
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE `" + table + "`")) {
                    if (rs.next()) {
                        String createSql = rs.getString(2);
                        writer.write(createSql + ";\n\n");
                    }
                }

                // Dump data rows
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM `" + table + "`")) {
                    ResultSetMetaData rsmd = rs.getMetaData();
                    int colCount = rsmd.getColumnCount();

                    int rowCount = 0;
                    StringBuilder insertHeader = new StringBuilder();
                    insertHeader.append("INSERT INTO `").append(table).append("` (");
                    for (int i = 1; i <= colCount; i++) {
                        insertHeader.append("`").append(rsmd.getColumnName(i)).append("`");
                        if (i < colCount) insertHeader.append(", ");
                    }
                    insertHeader.append(") VALUES\n");

                    boolean firstRow = true;
                    while (rs.next()) {
                        if (firstRow) {
                            writer.write(insertHeader.toString());
                            firstRow = false;
                        } else {
                            writer.write(",\n");
                        }

                        writer.write("(");
                        for (int i = 1; i <= colCount; i++) {
                            Object val = rs.getObject(i);
                            if (val == null) {
                                writer.write("NULL");
                            } else if (val instanceof Number || val instanceof Boolean) {
                                writer.write(val.toString());
                            } else {
                                String strVal = rs.getString(i);
                                writer.write("'" + escapeSql(strVal) + "'");
                            }
                            if (i < colCount) writer.write(", ");
                        }
                        writer.write(")");
                        rowCount++;
                    }

                    if (rowCount > 0) {
                        writer.write(";\n\n");
                    }
                }
            }

            writer.write("SET FOREIGN_KEY_CHECKS=1;\n");
            writer.write("-- Backup completed successfully\n");
        }
    }

    private String escapeSql(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("'", "\\'")
                  .replace("\r", "\\r")
                  .replace("\n", "\\n");
    }

    /**
     * Restores database from a given SQL file.
     */
    public synchronized int restoreBackup(File sqlFile, String triggeredBy) throws Exception {
        if (!sqlFile.exists() || sqlFile.length() == 0) {
            throw new IllegalArgumentException("Backup file does not exist or is empty.");
        }

        log.info("Starting database restore from file: {}", sqlFile.getAbsolutePath());
        int executedCount = 0;

        try (Connection conn = dataSource.getConnection();
             BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(sqlFile), StandardCharsets.UTF_8));
             Statement statement = conn.createStatement()) {

            conn.setAutoCommit(false);
            StringBuilder sqlBatch = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.startsWith("/*") || trimmed.isEmpty()) {
                    continue;
                }
                sqlBatch.append(line).append("\n");

                if (trimmed.endsWith(";")) {
                    String sql = sqlBatch.toString().trim();
                    if (!sql.isEmpty()) {
                        try {
                            statement.execute(sql);
                            executedCount++;
                        } catch (SQLException ex) {
                            log.warn("SQL statement warning during restore: {} - SQL: {}", ex.getMessage(), sql.length() > 100 ? sql.substring(0, 100) : sql);
                        }
                    }
                    sqlBatch.setLength(0);
                }
            }

            conn.commit();
            log.info("Database restore completed successfully. Executed {} SQL statements.", executedCount);
            auditService.log(null, "DATABASE_RESTORED", "DATABASE", null, null, "Database restored from: " + sqlFile.getName() + " by " + triggeredBy);
            return executedCount;
        } catch (Exception e) {
            log.error("Failed to restore database from {}: {}", sqlFile.getName(), e.getMessage(), e);
            throw new RuntimeException("Database restoration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Uploads any backup that is currently in PENDING state to Google Drive.
     */
    public synchronized int syncPendingBackups() {
        BackupConfig config = getOrCreateConfig();
        if (!Boolean.TRUE.equals(config.getGoogleDriveEnabled())) {
            return 0;
        }

        List<BackupRecord> pendingRecords = backupRecordRepository.findByGoogleDriveStatus("PENDING");
        int count = 0;
        for (BackupRecord record : pendingRecords) {
            File f = new File(record.getFilePath());
            if (f.exists()) {
                boolean ok = googleDriveService.uploadBackupFile(f, record, config);
                if (ok) {
                    record.setGoogleDriveStatus("UPLOADED");
                    backupRecordRepository.save(record);
                    count++;
                }
            }
        }
        return count;
    }
}
