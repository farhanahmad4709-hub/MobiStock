package com.mobistock.controller;

import com.mobistock.model.BackupConfig;
import com.mobistock.model.BackupRecord;
import com.mobistock.model.User;
import com.mobistock.service.DatabaseBackupService;
import com.mobistock.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Controller
@RequestMapping("/backup")
@RequiredArgsConstructor
@Slf4j
public class BackupController {

    private final DatabaseBackupService databaseBackupService;
    private final UserService userService;

    @GetMapping
    public String backupDashboard(Model model) {
        BackupConfig config = databaseBackupService.getOrCreateConfig();
        List<BackupRecord> backups = databaseBackupService.getAllBackups();

        model.addAttribute("config", config);
        model.addAttribute("backups", backups);
        model.addAttribute("currentUser", userService.getCurrentUser());
        model.addAttribute("pageTitle", "Backup & Recovery");
        return "backup/index";
    }

    @PostMapping("/create")
    public String createManualBackup(RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getCurrentUser();
            String username = user != null ? user.getUsername() : "User";
            BackupRecord record = databaseBackupService.createBackup("MANUAL", username);

            String msg = "Backup created successfully! Saved to: " + record.getFilePath();
            if ("UPLOADED".equals(record.getGoogleDriveStatus())) {
                msg += " and synced to Google Drive.";
            } else if ("PENDING".equals(record.getGoogleDriveStatus())) {
                msg += " (Google Drive upload queued/offline).";
            }
            redirectAttributes.addFlashAttribute("success", msg);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating backup: " + e.getMessage());
        }
        return "redirect:/backup";
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadBackupFile(@PathVariable Long id) {
        BackupRecord record = databaseBackupService.getBackupById(id);
        File file = new File(record.getFilePath());

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + record.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .body(resource);
    }

    @PostMapping("/sync-drive")
    public String syncGoogleDrive(RedirectAttributes redirectAttributes) {
        try {
            int synced = databaseBackupService.syncPendingBackups();
            if (synced > 0) {
                redirectAttributes.addFlashAttribute("success", "Successfully uploaded " + synced + " backup(s) to Google Drive.");
            } else {
                redirectAttributes.addFlashAttribute("info", "All backups are already synced or Google Drive is not configured.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error syncing to Google Drive: " + e.getMessage());
        }
        return "redirect:/backup";
    }

    @PostMapping("/restore/{id}")
    public String restoreFromBackupRecord(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getCurrentUser();
            BackupRecord record = databaseBackupService.getBackupById(id);
            File file = new File(record.getFilePath());

            if (!file.exists()) {
                redirectAttributes.addFlashAttribute("error", "Backup file not found on disk: " + record.getFilePath());
                return "redirect:/backup";
            }

            int count = databaseBackupService.restoreBackup(file, user != null ? user.getUsername() : "Admin");
            redirectAttributes.addFlashAttribute("success", "Database successfully restored from " + record.getFilename() + "! (" + count + " statements executed).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Restore failed: " + e.getMessage());
        }
        return "redirect:/backup";
    }

    @PostMapping("/restore-upload")
    public String restoreFromUploadedFile(@RequestParam("backupFile") MultipartFile multipartFile,
                                         RedirectAttributes redirectAttributes) {
        if (multipartFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a valid .sql backup file to upload.");
            return "redirect:/backup";
        }

        Path tempFile = null;
        try {
            User user = userService.getCurrentUser();
            tempFile = Files.createTempFile("mobistock_restore_", ".sql");
            try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
                fos.write(multipartFile.getBytes());
            }

            int count = databaseBackupService.restoreBackup(tempFile.toFile(), user != null ? user.getUsername() : "Admin");
            redirectAttributes.addFlashAttribute("success", "Database successfully restored from uploaded file: " 
                    + multipartFile.getOriginalFilename() + " (" + count + " statements executed).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed restoring from uploaded file: " + e.getMessage());
        } finally {
            if (tempFile != null) {
                try { Files.deleteIfExists(tempFile); } catch (Exception ignored) {}
            }
        }
        return "redirect:/backup";
    }

    @PostMapping("/settings")
    public String saveBackupSettings(
            @RequestParam(value = "localBackupDir", required = false) String localBackupDir,
            @RequestParam(value = "autoBackupEnabled", defaultValue = "false") boolean autoBackupEnabled,
            @RequestParam(value = "backupSchedule", defaultValue = "DAILY_NIGHT") String backupSchedule,
            @RequestParam(value = "googleDriveEnabled", defaultValue = "false") boolean googleDriveEnabled,
            @RequestParam(value = "googleDriveMode", defaultValue = "DESKTOP_SYNC") String googleDriveMode,
            @RequestParam(value = "googleDriveDesktopPath", required = false) String googleDriveDesktopPath,
            @RequestParam(value = "googleDriveFolderId", required = false) String googleDriveFolderId,
            RedirectAttributes redirectAttributes) {
        try {
            databaseBackupService.updateConfigValues(
                    localBackupDir,
                    autoBackupEnabled,
                    backupSchedule,
                    googleDriveEnabled,
                    googleDriveMode,
                    googleDriveDesktopPath,
                    googleDriveFolderId
            );
            redirectAttributes.addFlashAttribute("success", "Backup & Google Drive configuration saved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed saving settings: " + e.getMessage());
        }
        return "redirect:/backup";
    }
}
