package com.octo.drogri.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.octo.drogri.database.DatabaseHelper;
import com.octo.drogri.models.BackupRecord;

import java.io.File;

/**
 * Background WorkManager Worker for uploading backups to Google Drive.
 * Enforces network connectivity and debounces concurrent triggers.
 */
public class BackupWorker extends Worker {

    private static final String TAG = "BackupWorker";
    public static final String WORK_NAME = "google_drive_backup_work";

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SettingsManager settings = SettingsManager.getInstance(context);

        if (!settings.isGoogleDriveBackupEnabled() || !GoogleDriveManager.isAccountConnected(context)) {
            Log.d(TAG, "Google Drive backup skipped: not enabled or account not connected");
            return Result.success();
        }

        try {
            Log.d(TAG, "Starting background Google Drive backup...");

            // 1. Create a fresh local backup ZIP
            File backupZip = LocalBackupManager.createBackupZip(context, "GOOGLE_DRIVE");
            if (backupZip == null || !backupZip.exists()) {
                Log.e(TAG, "Failed to create local backup zip for cloud upload");
                return Result.retry();
            }

            // 2. Upload to Google Drive
            GoogleDriveManager.UploadResult uploadResult = GoogleDriveManager.uploadBackup(context, backupZip, "Automatic Google Drive Backup");
            if (uploadResult.success && uploadResult.fileId != null) {
                // 3. Record in database
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
                BackupRecord record = new BackupRecord();
                record.setBackupName(GoogleDriveManager.CLOUD_BACKUP_FILENAME);
                record.setBackupType("GOOGLE_DRIVE");
                record.setFilePath(backupZip.getAbsolutePath());
                record.setFileSize(backupZip.length());
                record.setProductCount(dbHelper.getTotalProductCount());
                record.setCategoryCount(dbHelper.getTotalCategoryCount());
                record.setDriveFileId(uploadResult.fileId);
                record.setCloud(true);
                dbHelper.insertBackupRecord(record);

                // 4. Mark change log as synced
                dbHelper.markAllChangesSynced();
                settings.setCloudBackupPending(false);
                settings.setLastCloudError(null);
                settings.resetProductsAddedSinceBackup();

                Log.d(TAG, "Background backup successfully uploaded to Google Drive. ID: " + uploadResult.fileId);
                return Result.success();
            } else {
                Log.w(TAG, "Drive upload failed, will retry with WorkManager backoff");
                settings.setCloudBackupPending(true);
                settings.setLastCloudError(uploadResult.userFriendlyMessage);
                return Result.retry();
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception during backup worker execution", e);
            settings.setCloudBackupPending(true);
            settings.setLastCloudError(e.getMessage());
            return Result.retry();
        }
    }

    /**
     * Enqueues a background cloud backup with network constraint and debouncing.
     */
    public static void scheduleCloudBackup(Context context) {
        SettingsManager settings = SettingsManager.getInstance(context);
        NetworkType netType = settings.isBackupWifiOnly() ? NetworkType.UNMETERED : NetworkType.CONNECTED;

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(netType)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BackupWorker.class)
                .setConstraints(constraints)
                .build();

        // Use ExistingWorkPolicy.KEEP to debounce repeated rapid calls
        WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
        );
    }

    /**
     * Immediately triggers cloud backup, replacing any pending task.
     */
    public static void triggerImmediateCloudBackup(Context context) {
        SettingsManager settings = SettingsManager.getInstance(context);
        NetworkType netType = settings.isBackupWifiOnly() ? NetworkType.UNMETERED : NetworkType.CONNECTED;

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(netType)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BackupWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
        );
    }
}
