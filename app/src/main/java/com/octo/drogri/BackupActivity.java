package com.octo.drogri;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.octo.drogri.database.DatabaseHelper;
import com.octo.drogri.dialogs.PasswordDialog;
import com.octo.drogri.models.BackupRecord;
import com.octo.drogri.utils.AuthHelper;
import com.octo.drogri.utils.BackupManager;
import com.octo.drogri.utils.GoogleDriveManager;
import com.octo.drogri.utils.LocalBackupManager;
import com.octo.drogri.utils.SecurityManager;
import com.octo.drogri.utils.SettingsManager;
import com.octo.drogri.utils.ThemeManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Activity for managing local and Google Drive cloud backups,
 * connecting Google account via OAuth 2.0, triggering immediate backups, and restoring backups.
 */
public class BackupActivity extends BaseActivity {

    private DatabaseHelper databaseHelper;
    private SettingsManager settingsManager;

    // Local Backup UI
    private TextView tvLocalStatus;
    private TextView tvLastLocalBackup;
    private TextView tvLocalDetails;
    private TextView tvUnsyncedCounter;

    // Cloud Backup UI
    private TextView tvGoogleAccountStatus;
    private MaterialButton btnGoogleAuth;
    private TextView tvCloudStatus;
    private TextView tvLastCloudBackup;
    private TextView tvCloudFileInfo;
    private TextView tvCloudFileId;
    private TextView tvProductsCounter;
    private TextView tvCloudError;

    // Action Buttons
    private MaterialButton btnBackupNow;
    private MaterialButton btnViewHistory;
    private MaterialButton btnRestoreLocal;
    private MaterialButton btnRestoreDrive;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account != null) {
                        settingsManager.setGoogleAccountEmail(account.getEmail());
                        settingsManager.setGoogleDriveBackupEnabled(true);
                        settingsManager.setLastCloudError(null);
                        Toast.makeText(this, "تم ربط حساب Google بنجاح: " + account.getEmail(), Toast.LENGTH_SHORT).show();
                        updateGoogleAccountUI();
                        updateDashboardStats();
                    }
                } catch (ApiException e) {
                    Toast.makeText(this, "فشل تسجيل الدخول بحساب Google (" + e.getStatusCode() + ")", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<Intent> historyLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    updateDashboardStats();
                    setResult(RESULT_OK);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        databaseHelper = DatabaseHelper.getInstance(this);
        settingsManager = SettingsManager.getInstance(this);

        initViews();
        updateGoogleAccountUI();
        updateDashboardStats();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Local views
        tvLocalStatus = findViewById(R.id.tv_local_status);
        tvLastLocalBackup = findViewById(R.id.tv_last_local_backup);
        tvLocalDetails = findViewById(R.id.tv_local_details);
        tvUnsyncedCounter = findViewById(R.id.tv_unsynced_counter);

        // Cloud views
        tvGoogleAccountStatus = findViewById(R.id.tv_google_account_status);
        btnGoogleAuth = findViewById(R.id.btn_google_auth);
        tvCloudStatus = findViewById(R.id.tv_cloud_status);
        tvLastCloudBackup = findViewById(R.id.tv_last_cloud_backup);
        tvCloudFileInfo = findViewById(R.id.tv_cloud_file_info);
        tvCloudFileId = findViewById(R.id.tv_cloud_file_id);
        tvProductsCounter = findViewById(R.id.tv_products_counter);
        tvCloudError = findViewById(R.id.tv_cloud_error);

        // Actions
        btnBackupNow = findViewById(R.id.btn_backup_now);
        btnViewHistory = findViewById(R.id.btn_view_history);
        btnRestoreLocal = findViewById(R.id.btn_restore_local);
        btnRestoreDrive = findViewById(R.id.btn_restore_drive);

        btnGoogleAuth.setOnClickListener(v -> handleGoogleAuthToggle());
        btnBackupNow.setOnClickListener(v -> handleBackupNow());

        btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, BackupHistoryActivity.class);
            historyLauncher.launch(intent);
        });

        btnRestoreLocal.setOnClickListener(v -> showLocalBackupsDialog());
        btnRestoreDrive.setOnClickListener(v -> showDriveBackupsDialog());
    }

    private void updateGoogleAccountUI() {
        if (GoogleDriveManager.isAccountConnected(this)) {
            String email = GoogleDriveManager.getConnectedEmail(this);
            tvGoogleAccountStatus.setText(getString(R.string.connected_as, email != null ? email : ""));
            tvGoogleAccountStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
            btnGoogleAuth.setText(R.string.disconnect_google);
        } else {
            tvGoogleAccountStatus.setText(R.string.not_connected);
            tvGoogleAccountStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            btnGoogleAuth.setText(R.string.connect_google);
        }
    }

    private void updateDashboardStats() {
        // 1. LOCAL BACKUP STATUS
        File backupDir = LocalBackupManager.getBackupDirectory(this);
        File[] files = backupDir.listFiles((dir, name) -> name.endsWith(".zip"));

        long lastLocalTime = settingsManager.getLastLocalBackupTime();
        if (files != null && files.length > 0) {
            Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            File latest = files[0];
            tvLocalStatus.setText(R.string.local_backup_present);
            tvLocalStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_green));

            String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(latest.lastModified()));
            tvLastLocalBackup.setText(getString(R.string.last_local_backup, formattedDate));

            int prodCount = databaseHelper.getTotalProductCount();
            int imgCount = databaseHelper.getAllProductImagesCount();
            String sizeStr = (latest.length() / 1024) + " KB";
            tvLocalDetails.setText(getString(R.string.local_backup_stats, sizeStr, prodCount, imgCount));
        } else if (lastLocalTime > 0) {
            tvLocalStatus.setText(R.string.local_backup_present);
            tvLocalStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
            String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(lastLocalTime));
            tvLastLocalBackup.setText(getString(R.string.last_local_backup, formattedDate));
            tvLocalDetails.setText(getString(R.string.local_backup_stats, (settingsManager.getLastLocalBackupSize() / 1024) + " KB", databaseHelper.getTotalProductCount(), databaseHelper.getAllProductImagesCount()));
        } else {
            tvLocalStatus.setText(R.string.local_backup_absent);
            tvLocalStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvLastLocalBackup.setText(getString(R.string.last_local_backup, "لا توجد"));
            tvLocalDetails.setText("—");
        }

        int unsynced = databaseHelper.getUnsyncedChangesCount();
        tvUnsyncedCounter.setText(getString(R.string.unsynced_changes_counter, unsynced));

        // 2. GOOGLE DRIVE CLOUD STATUS
        boolean connected = GoogleDriveManager.isAccountConnected(this);
        if (!connected) {
            tvCloudStatus.setText(R.string.not_connected);
            tvCloudStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvLastCloudBackup.setText(getString(R.string.last_cloud_backup, "لا توجد"));
            tvCloudFileInfo.setVisibility(View.GONE);
            tvCloudFileId.setVisibility(View.GONE);
            tvCloudError.setVisibility(View.GONE);
            tvProductsCounter.setText(getString(R.string.products_added_counter, settingsManager.getProductsAddedSinceBackup(), settingsManager.getNewProductsThreshold()));
            return;
        }

        long lastCloud = settingsManager.getLastCloudBackupTime();
        String cloudError = settingsManager.getLastCloudError();

        if (cloudError != null && !cloudError.trim().isEmpty()) {
            tvCloudStatus.setText(R.string.cloud_status_failed);
            tvCloudStatus.setTextColor(ContextCompat.getColor(this, R.color.danger));
            tvCloudError.setVisibility(View.VISIBLE);
            tvCloudError.setText(getString(R.string.cloud_error_prefix, cloudError));
        } else if (lastCloud > 0) {
            tvCloudStatus.setText(R.string.cloud_status_success);
            tvCloudStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
            tvCloudError.setVisibility(View.GONE);
        } else {
            tvCloudStatus.setText(R.string.cloud_status_not_synced);
            tvCloudStatus.setTextColor(ThemeManager.getThemeColor(this, com.google.android.material.R.attr.colorPrimary));
            tvCloudError.setVisibility(View.GONE);
        }

        if (lastCloud > 0) {
            String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(lastCloud));
            tvLastCloudBackup.setText(getString(R.string.last_cloud_backup, formattedDate));
        } else {
            tvLastCloudBackup.setText(getString(R.string.last_cloud_backup, "لا توجد"));
        }

        String cloudFileName = settingsManager.getLastCloudBackupName();
        long cloudSize = settingsManager.getLastCloudBackupSize();
        if (cloudFileName != null && !cloudFileName.isEmpty()) {
            tvCloudFileInfo.setVisibility(View.VISIBLE);
            tvCloudFileInfo.setText(getString(R.string.cloud_backup_file_info, cloudFileName, (cloudSize / 1024) + " KB"));
        } else {
            tvCloudFileInfo.setVisibility(View.GONE);
        }

        String cloudFileId = settingsManager.getLastCloudBackupId();
        if (cloudFileId != null && !cloudFileId.isEmpty()) {
            tvCloudFileId.setVisibility(View.VISIBLE);
            tvCloudFileId.setText(getString(R.string.cloud_file_id_info, cloudFileId));
        } else {
            tvCloudFileId.setVisibility(View.GONE);
        }

        int addedSince = settingsManager.getProductsAddedSinceBackup();
        int threshold = settingsManager.getNewProductsThreshold();
        tvProductsCounter.setText(getString(R.string.products_added_counter, addedSince, threshold));
    }

    private void handleGoogleAuthToggle() {
        if (GoogleDriveManager.isAccountConnected(this)) {
            // Disconnect
            Runnable disconnectTask = () -> {
                GoogleDriveManager.getGoogleSignInClient(BackupActivity.this).signOut().addOnCompleteListener(task -> {
                    settingsManager.setGoogleAccountEmail("");
                    settingsManager.setGoogleDriveBackupEnabled(false);
                    settingsManager.setLastCloudError(null);
                    Toast.makeText(BackupActivity.this, "تم فصل حساب Google", Toast.LENGTH_SHORT).show();
                    updateGoogleAccountUI();
                    updateDashboardStats();
                });
            };

            AuthHelper.authenticate(this, getString(R.string.confirm_password_dialog_msg), disconnectTask::run);
        } else {
            // Connect
            Intent signInIntent = GoogleDriveManager.getGoogleSignInClient(this).getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        }
    }

    private void handleBackupNow() {
        btnBackupNow.setEnabled(false);
        int primaryColor = ThemeManager.getThemeColor(this, com.google.android.material.R.attr.colorPrimary);
        tvLocalStatus.setText(R.string.status_in_progress);
        tvLocalStatus.setTextColor(primaryColor);

        BackupManager.performManualBackup(this, new BackupManager.ManualBackupCallback() {
            @Override
            public void onStarted() {
                tvLocalStatus.setText(R.string.status_in_progress);
            }

            @Override
            public void onLocalSuccess(File backupFile) {
                updateDashboardStats();
                Toast.makeText(BackupActivity.this, R.string.success_backup_created, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCloudUploading() {
                tvCloudStatus.setText(R.string.cloud_status_uploading);
                tvCloudStatus.setTextColor(ThemeManager.getThemeColor(BackupActivity.this, com.google.android.material.R.attr.colorPrimary));
            }

            @Override
            public void onCloudSuccess(File backupFile, String driveFileId) {
                btnBackupNow.setEnabled(true);
                updateDashboardStats();
                Toast.makeText(BackupActivity.this, "تم رفع النسخة الاحتياطية بنجاح إلى Google Drive", Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
            }

            @Override
            public void onCloudFailed(File backupFile, String errorMessage) {
                btnBackupNow.setEnabled(true);
                updateDashboardStats();
                new MaterialAlertDialogBuilder(BackupActivity.this)
                        .setTitle("تنبيه بشأن النسخ السحابي")
                        .setMessage("تم إنشاء النسخة المحلية بنجاح.\n\nفشل الرفع إلى Google Drive:\n" + errorMessage)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }

            @Override
            public void onError(String message) {
                btnBackupNow.setEnabled(true);
                updateDashboardStats();
                Toast.makeText(BackupActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLocalBackupsDialog() {
        File backupDir = LocalBackupManager.getBackupDirectory(this);
        File[] files = backupDir.listFiles((dir, name) -> name.endsWith(".zip"));

        if (files == null || files.length == 0) {
            Toast.makeText(this, "لا توجد نسخ احتياطية محلية محفوظة", Toast.LENGTH_SHORT).show();
            return;
        }

        Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            names[i] = files[i].getName() + " (" + (files[i].length() / 1024) + " KB)";
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.restore_local)
                .setItems(names, (dialog, which) -> {
                    File selectedFile = files[which];
                    confirmAndRestore(selectedFile);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showDriveBackupsDialog() {
        if (!GoogleDriveManager.isAccountConnected(this)) {
            Toast.makeText(this, "يرجى ربط حساب Google أولاً", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "جاري جلب النسخ الاحتياطية من Google Drive…", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            List<BackupRecord> cloudBackups = GoogleDriveManager.listDriveBackups(this);
            mainHandler.post(() -> {
                if (cloudBackups.isEmpty()) {
                    Toast.makeText(this, "لم يتم العثور على أي نسخ احتياطية في مجلد MyStore Backups في Google Drive", Toast.LENGTH_LONG).show();
                    return;
                }

                String[] items = new String[cloudBackups.size()];
                for (int i = 0; i < cloudBackups.size(); i++) {
                    BackupRecord r = cloudBackups.get(i);
                    items[i] = r.getBackupName() + " (" + r.getFormattedSize() + ")";
                }

                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.restore_drive)
                        .setItems(items, (dialog, which) -> {
                            BackupRecord selected = cloudBackups.get(which);
                            showDriveBackupOptionsDialog(selected);
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            });
        });
    }

    private void showDriveBackupOptionsDialog(BackupRecord record) {
        String[] options = {"استعادة النسخة للمتجر", "حذف النسخة من Google Drive"};
        new MaterialAlertDialogBuilder(this)
                .setTitle(record.getBackupName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        downloadAndRestoreDriveBackup(record);
                    } else if (which == 1) {
                        confirmAndDeleteDriveBackup(record);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmAndDeleteDriveBackup(BackupRecord record) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_cloud_backup)
                .setMessage(R.string.delete_cloud_backup_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    Toast.makeText(this, "جاري حذف النسخة من Google Drive…", Toast.LENGTH_SHORT).show();
                    Executors.newSingleThreadExecutor().execute(() -> {
                        boolean deleted = GoogleDriveManager.deleteDriveBackup(this, record.getDriveFileId());
                        mainHandler.post(() -> {
                            if (deleted) {
                                Toast.makeText(this, "تم حذف النسخة من Google Drive بنجاح", Toast.LENGTH_SHORT).show();
                                updateDashboardStats();
                            } else {
                                Toast.makeText(this, "فشل حذف النسخة من Google Drive", Toast.LENGTH_LONG).show();
                            }
                        });
                    });
                })
                .show();
    }

    private void downloadAndRestoreDriveBackup(BackupRecord record) {
        Toast.makeText(this, "جاري تنزيل النسخة الاحتياطية من Google Drive…", Toast.LENGTH_SHORT).show();

        File tempDest = new File(getCacheDir(), record.getBackupName());
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean downloaded = GoogleDriveManager.downloadDriveBackup(this, record.getDriveFileId(), tempDest);
            mainHandler.post(() -> {
                if (downloaded) {
                    confirmAndRestore(tempDest);
                } else {
                    Toast.makeText(this, "فشل تنزيل النسخة من Google Drive", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void confirmAndRestore(File zipFile) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.restore_backup_confirm_title)
                .setMessage(R.string.restore_backup_confirm_msg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.restore, (dialog, which) -> {
                    AuthHelper.authenticate(BackupActivity.this, getString(R.string.confirm_password_dialog_msg), () -> performRestoreZip(zipFile));
                })
                .show();
    }

    private void performRestoreZip(File zipFile) {
        boolean success = LocalBackupManager.restoreFromZip(this, zipFile);
        if (success) {
            Toast.makeText(this, R.string.success_restore, Toast.LENGTH_SHORT).show();
            updateDashboardStats();
            setResult(RESULT_OK);
        } else {
            Toast.makeText(this, "فشلت استعادة البيانات من النسخة", Toast.LENGTH_LONG).show();
        }
    }
}
