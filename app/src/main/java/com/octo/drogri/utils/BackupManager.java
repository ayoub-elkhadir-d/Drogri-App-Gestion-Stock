package com.octo.drogri.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.octo.drogri.database.DatabaseHelper;
import com.octo.drogri.models.BackupRecord;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Central coordinator for both Local Automatic and Google Drive Cloud backups.
 * Manages backup trigger conditions, product threshold counting, and background thread dispatch.
 */
public class BackupManager {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface BackupCallback {
        void onSuccess(File backupFile);
        void onError(String message);
    }

    public interface ManualBackupCallback {
        void onStarted();
        void onLocalSuccess(File backupFile);
        void onCloudUploading();
        void onCloudSuccess(File backupFile, String driveFileId);
        void onCloudFailed(File backupFile, String errorMessage);
        void onError(String message);
    }

    /**
     * Triggered whenever a new product is added.
     * Evaluates local backup and the 10-product Google Drive threshold.
     */
    public static void onProductAdded(Context context) {
        SettingsManager settings = SettingsManager.getInstance(context);

        // 1. Local backup & persistent SAF
        if (settings.isAutoLocalBackupEnabled()) {
            executor.execute(() -> LocalBackupManager.createBackupZip(context, "PRODUCT_ADDED"));
        }

        // 2. Count new products and dispatch cloud backup
        settings.incrementProductsAddedSinceBackup();
        if (settings.isGoogleDriveBackupEnabled()) {
            settings.setCloudBackupPending(true);
            int count = settings.getProductsAddedSinceBackup();
            int threshold = settings.getNewProductsThreshold();
            if (count >= threshold) {
                BackupWorker.scheduleCloudBackup(context);
            }
        }
    }

    /**
     * Triggered whenever an existing product is edited.
     */
    public static void onProductEdited(Context context) {
        SettingsManager settings = SettingsManager.getInstance(context);

        if (settings.isAutoLocalBackupEnabled()) {
            executor.execute(() -> LocalBackupManager.createBackupZip(context, "PRODUCT_EDITED"));
        }

        if (settings.isGoogleDriveBackupEnabled()) {
            settings.setCloudBackupPending(true);
            if (settings.isBackupOnEdit()) {
                BackupWorker.scheduleCloudBackup(context);
            }
        }
    }

    /**
     * Triggered whenever a product is deleted.
     */
    public static void onProductDeleted(Context context) {
        SettingsManager settings = SettingsManager.getInstance(context);

        if (settings.isAutoLocalBackupEnabled()) {
            executor.execute(() -> LocalBackupManager.createBackupZip(context, "PRODUCT_DELETED"));
        }

        if (settings.isGoogleDriveBackupEnabled()) {
            settings.setCloudBackupPending(true);
            if (settings.isBackupOnDelete()) {
                BackupWorker.scheduleCloudBackup(context);
            }
        }
    }

    /**
     * Triggered whenever multiple products are bulk deleted in a single action.
     * Triggers a single local backup and a single cloud backup dispatch.
     */
    public static void onProductsBulkDeleted(Context context, int count) {
        SettingsManager settings = SettingsManager.getInstance(context);

        if (settings.isAutoLocalBackupEnabled()) {
            executor.execute(() -> LocalBackupManager.createBackupZip(context, "BULK_DELETE_" + count + "_PRODUCTS"));
        }

        if (settings.isGoogleDriveBackupEnabled()) {
            settings.setCloudBackupPending(true);
            if (settings.isBackupOnDelete()) {
                BackupWorker.scheduleCloudBackup(context);
            }
        }
    }

    /**
     * Triggered whenever a category is added, edited or deleted.
     */
    public static void onCategoryChanged(Context context) {
        SettingsManager settings = SettingsManager.getInstance(context);

        if (settings.isAutoLocalBackupEnabled()) {
            executor.execute(() -> LocalBackupManager.createBackupZip(context, "CATEGORY_CHANGED"));
        }

        if (settings.isGoogleDriveBackupEnabled()) {
            settings.setCloudBackupPending(true);
            BackupWorker.scheduleCloudBackup(context);
        }
    }

    /**
     * Performs an immediate manual backup ("نسخ احتياطي الآن") with full progress and error callbacks.
     */
    public static void performManualBackup(Context context, ManualBackupCallback callback) {
        if (callback != null) {
            mainHandler.post(callback::onStarted);
        }

        executor.execute(() -> {
            File zip = LocalBackupManager.createBackupZip(context, "MANUAL");
            if (zip == null) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("فشل إنشاء النسخة الاحتياطية المحلية"));
                }
                return;
            }

            if (callback != null) {
                mainHandler.post(() -> callback.onLocalSuccess(zip));
            }

            SettingsManager settings = SettingsManager.getInstance(context);
            if (settings.isGoogleDriveBackupEnabled() && GoogleDriveManager.isAccountConnected(context)) {
                if (callback != null) {
                    mainHandler.post(callback::onCloudUploading);
                }

                GoogleDriveManager.UploadResult uploadResult = GoogleDriveManager.uploadBackup(context, zip, "Manual Google Drive Backup");
                if (uploadResult.success && uploadResult.fileId != null) {
                    DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
                    BackupRecord record = new BackupRecord();
                    record.setBackupName(GoogleDriveManager.CLOUD_BACKUP_FILENAME);
                    record.setBackupType("GOOGLE_DRIVE");
                    record.setFilePath(zip.getAbsolutePath());
                    record.setFileSize(zip.length());
                    record.setProductCount(dbHelper.getTotalProductCount());
                    record.setCategoryCount(dbHelper.getTotalCategoryCount());
                    record.setDriveFileId(uploadResult.fileId);
                    record.setCloud(true);
                    dbHelper.insertBackupRecord(record);
                    dbHelper.markAllChangesSynced();

                    settings.setCloudBackupPending(false);
                    settings.setLastCloudError(null);
                    settings.resetProductsAddedSinceBackup();

                    if (callback != null) {
                        mainHandler.post(() -> callback.onCloudSuccess(zip, uploadResult.fileId));
                    }
                } else {
                    settings.setCloudBackupPending(true);
                    settings.setLastCloudError(uploadResult.userFriendlyMessage);

                    if (callback != null) {
                        mainHandler.post(() -> callback.onCloudFailed(zip, uploadResult.userFriendlyMessage));
                    }
                }
            }
        });
    }

    public static void performManualBackup(Context context, BackupCallback callback) {
        performManualBackup(context, new ManualBackupCallback() {
            @Override
            public void onStarted() {}

            @Override
            public void onLocalSuccess(File backupFile) {
                if (callback != null) callback.onSuccess(backupFile);
            }

            @Override
            public void onCloudUploading() {}

            @Override
            public void onCloudSuccess(File backupFile, String driveFileId) {}

            @Override
            public void onCloudFailed(File backupFile, String errorMessage) {}

            @Override
            public void onError(String message) {
                if (callback != null) callback.onError(message);
            }
        });
    }
}
