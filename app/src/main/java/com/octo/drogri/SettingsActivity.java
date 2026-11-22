package com.octo.drogri;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.octo.drogri.adapters.ThemeAdapter;
import com.octo.drogri.database.DatabaseHelper;
import com.octo.drogri.utils.BackupManager;
import com.octo.drogri.utils.SecurityManager;
import com.octo.drogri.utils.SettingsManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Activity for configuring user preferences: security & privacy,
 * 10 custom themes and light/dark modes, product card display,
 * currency symbol, and automatic backup settings.
 */
public class SettingsActivity extends BaseActivity {

    private SettingsManager settingsManager;
    private SecurityManager securityManager;

    // Security & Privacy
    private MaterialButton btnChangePassword;
    private SwitchMaterial switchProtectSensitive;
    private SwitchMaterial switchMaskPurchasePrice;
    private SwitchMaterial switchBiometricAuth;
    private TextView tvBiometricDesc;

    // Appearance & Themes
    private RecyclerView rvThemes;
    private ThemeAdapter themeAdapter;
    private RadioGroup rgTheme;
    private RadioButton rbLight, rbDark, rbSystem;
    private SwitchMaterial switchCompactCard;

    // Card Display Checkboxes
    private CheckBox cbShowImage, cbShowSellingPrice, cbShowPurchasePrice;
    private CheckBox cbShowCategory, cbShowColor, cbShowSize;

    // Currency
    private EditText etCurrencySymbol;

    // Backup Settings
    private SwitchMaterial switchAutoLocalBackup;
    private SwitchMaterial switchGoogleDriveBackup;
    private SwitchMaterial switchBackupWifiOnly;
    private SwitchMaterial switchShowBackupWarning;
    private EditText etThreshold;
    private SwitchMaterial switchBackupOnEdit;
    private SwitchMaterial switchBackupOnDelete;
    private EditText etMaxRetention;

    // Persistent SAF Backup Folder (Feature 5 & 8)
    private TextView tvBackupFolderPath;
    private MaterialButton btnChooseBackupFolder;
    private TextView tvBackupStatusSummary;
    private MaterialButton btnManualBackup;

    // General
    private SwitchMaterial switchConfirmDelete;

    private final ActivityResultLauncher<Uri> safFolderLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
                if (uri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    } catch (Exception ignored) {
                    }
                    settingsManager.setSafBackupTreeUri(uri.toString());
                    DocumentFile doc = DocumentFile.fromTreeUri(this, uri);
                    String name = doc != null && doc.getName() != null ? doc.getName() : uri.getLastPathSegment();
                    settingsManager.setSafBackupDisplayName(name);
                    updateBackupFolderDisplay();
                    Toast.makeText(this, R.string.backup_folder_selected, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settingsManager = SettingsManager.getInstance(this);
        securityManager = SecurityManager.getInstance(this);

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Security
        btnChangePassword = findViewById(R.id.btn_change_password);
        switchProtectSensitive = findViewById(R.id.switch_protect_sensitive);
        switchMaskPurchasePrice = findViewById(R.id.switch_mask_purchase_price);
        switchBiometricAuth = findViewById(R.id.switch_biometric_auth);
        tvBiometricDesc = findViewById(R.id.tv_biometric_desc);

        // Themes & Appearance
        rvThemes = findViewById(R.id.rv_themes);
        rvThemes.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rgTheme = findViewById(R.id.rg_theme);
        rbLight = findViewById(R.id.rb_theme_light);
        rbDark = findViewById(R.id.rb_theme_dark);
        rbSystem = findViewById(R.id.rb_theme_system);
        switchCompactCard = findViewById(R.id.switch_compact_card);

        // Product Card Display
        cbShowImage = findViewById(R.id.cb_show_image);
        cbShowSellingPrice = findViewById(R.id.cb_show_selling_price);
        cbShowPurchasePrice = findViewById(R.id.cb_show_purchase_price);
        cbShowCategory = findViewById(R.id.cb_show_category);
        cbShowColor = findViewById(R.id.cb_show_color);
        cbShowSize = findViewById(R.id.cb_show_size);

        // Currency
        etCurrencySymbol = findViewById(R.id.et_currency_symbol);

        // Backups
        switchAutoLocalBackup = findViewById(R.id.switch_auto_local_backup);
        switchGoogleDriveBackup = findViewById(R.id.switch_google_drive_backup);
        switchBackupWifiOnly = findViewById(R.id.switch_backup_wifi_only);
        switchShowBackupWarning = findViewById(R.id.switch_show_backup_warning);
        etThreshold = findViewById(R.id.et_new_products_threshold);
        switchBackupOnEdit = findViewById(R.id.switch_backup_on_edit);
        switchBackupOnDelete = findViewById(R.id.switch_backup_on_delete);
        etMaxRetention = findViewById(R.id.et_max_retention);

        tvBackupFolderPath = findViewById(R.id.tv_backup_folder_path);
        btnChooseBackupFolder = findViewById(R.id.btn_choose_backup_folder);
        tvBackupStatusSummary = findViewById(R.id.tv_backup_status_summary);
        btnManualBackup = findViewById(R.id.btn_manual_backup);

        // General
        switchConfirmDelete = findViewById(R.id.switch_confirm_delete);
    }

    private void loadSettings() {
        // Security
        switchProtectSensitive.setChecked(securityManager.isProtectSensitiveOperations());
        switchMaskPurchasePrice.setChecked(securityManager.isMaskPurchasePrice());

        // Biometrics
        if (!SecurityManager.isBiometricSupported(this)) {
            switchBiometricAuth.setEnabled(false);
            switchBiometricAuth.setChecked(false);
            if (tvBiometricDesc != null) {
                tvBiometricDesc.setText(R.string.biometric_not_available);
            }
        } else {
            switchBiometricAuth.setEnabled(true);
            switchBiometricAuth.setChecked(securityManager.isBiometricAuthEnabled(this));
        }

        // Themes (10 Custom Themes)
        int selectedTheme = settingsManager.getSelectedThemeIndex();
        themeAdapter = new ThemeAdapter(this, selectedTheme, themeIndex -> {
            settingsManager.setSelectedThemeIndex(themeIndex);
            recreate();
        });
        rvThemes.setAdapter(themeAdapter);
        rvThemes.scrollToPosition(selectedTheme);

        // Theme mode (Light / Dark / System)
        int themeMode = settingsManager.getThemeMode();
        if (themeMode == 1) {
            rbDark.setChecked(true);
        } else if (themeMode == 2) {
            rbSystem.setChecked(true);
        } else {
            rbLight.setChecked(true);
        }

        switchCompactCard.setChecked(settingsManager.isCardCompact());

        // Card Display
        cbShowImage.setChecked(settingsManager.showCardImage());
        cbShowSellingPrice.setChecked(settingsManager.showCardSellingPrice());
        cbShowPurchasePrice.setChecked(settingsManager.showCardPurchasePrice());
        cbShowCategory.setChecked(settingsManager.showCardCategory());
        cbShowColor.setChecked(settingsManager.showCardColor());
        cbShowSize.setChecked(settingsManager.showCardSize());

        // Currency
        etCurrencySymbol.setText(settingsManager.getCurrencySymbol());

        // Backups
        switchAutoLocalBackup.setChecked(settingsManager.isAutoLocalBackupEnabled());
        switchGoogleDriveBackup.setChecked(settingsManager.isGoogleDriveBackupEnabled());
        switchBackupWifiOnly.setChecked(settingsManager.isBackupWifiOnly());
        switchShowBackupWarning.setChecked(settingsManager.isShowBackupWarningBanner());
        etThreshold.setText(String.valueOf(settingsManager.getNewProductsThreshold()));
        switchBackupOnEdit.setChecked(settingsManager.isBackupOnEdit());
        switchBackupOnDelete.setChecked(settingsManager.isBackupOnDelete());
        etMaxRetention.setText(String.valueOf(settingsManager.getMaxRetainedBackups()));

        updateBackupFolderDisplay();
        updateBackupStatusSummary();

        // General
        switchConfirmDelete.setChecked(settingsManager.isConfirmOnDelete());
    }

    private void updateBackupFolderDisplay() {
        String displayName = settingsManager.getSafBackupDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            tvBackupFolderPath.setText(getString(R.string.local_backup_location, displayName));
            btnChooseBackupFolder.setText(R.string.btn_change_backup_folder);
        } else {
            tvBackupFolderPath.setText(R.string.local_backup_location_default);
            btnChooseBackupFolder.setText(R.string.choose_backup_folder);
        }
    }

    private void updateBackupStatusSummary() {
        if (tvBackupStatusSummary == null) return;
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());

        long localTime = settingsManager.getLastLocalBackupTime();
        if (localTime > 0) {
            sb.append("آخر نسخة محلية: ").append(sdf.format(new Date(localTime)))
              .append(" (").append(settingsManager.getLastLocalBackupSize() / 1024).append(" KB)\n");
        } else {
            sb.append("آخر نسخة محلية: لا توجد\n");
        }

        long cloudTime = settingsManager.getLastCloudBackupTime();
        if (cloudTime > 0) {
            sb.append("آخر نسخة سحابية: ").append(sdf.format(new Date(cloudTime)))
              .append(" (").append(settingsManager.getLastCloudBackupSize() / 1024).append(" KB)\n");
        } else {
            sb.append("آخر نسخة سحابية: لا توجد\n");
        }

        int unsynced = DatabaseHelper.getInstance(this).getUnsyncedChangesCount();
        sb.append(getString(R.string.pending_changes_count, unsynced));

        tvBackupStatusSummary.setText(sb.toString());
    }

    private void handleManualBackup() {
        if (btnManualBackup == null) return;
        btnManualBackup.setEnabled(false);
        btnManualBackup.setText(R.string.backup_in_progress);

        BackupManager.performManualBackup(this, new BackupManager.ManualBackupCallback() {
            @Override
            public void onStarted() {
            }

            @Override
            public void onLocalSuccess(File backupFile) {
            }

            @Override
            public void onCloudUploading() {
            }

            @Override
            public void onCloudSuccess(File backupFile, String driveFileId) {
                runOnUiThread(() -> {
                    btnManualBackup.setEnabled(true);
                    btnManualBackup.setText(R.string.backup_now);
                    updateBackupStatusSummary();
                    Toast.makeText(SettingsActivity.this, R.string.backup_synced_success, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onCloudFailed(File backupFile, String errorMessage) {
                runOnUiThread(() -> {
                    btnManualBackup.setEnabled(true);
                    btnManualBackup.setText(R.string.backup_now);
                    updateBackupStatusSummary();
                    Toast.makeText(SettingsActivity.this, errorMessage != null ? errorMessage : "فشل النسخ السحابي", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    btnManualBackup.setEnabled(true);
                    btnManualBackup.setText(R.string.backup_now);
                    updateBackupStatusSummary();
                    Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setupListeners() {
        // Security listeners
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        switchProtectSensitive.setOnCheckedChangeListener((buttonView, isChecked) ->
                securityManager.setProtectSensitiveOperations(isChecked));

        switchMaskPurchasePrice.setOnCheckedChangeListener((buttonView, isChecked) ->
                securityManager.setMaskPurchasePrice(isChecked));

        switchBiometricAuth.setOnCheckedChangeListener((buttonView, isChecked) ->
                securityManager.setAuthMethod(isChecked ? SecurityManager.AUTH_METHOD_BIOMETRIC : SecurityManager.AUTH_METHOD_PASSWORD));

        // Light/Dark Theme change
        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int mode = 0;
            if (checkedId == R.id.rb_theme_dark) {
                mode = 1;
            } else if (checkedId == R.id.rb_theme_system) {
                mode = 2;
            }
            settingsManager.setThemeMode(mode);
        });

        switchCompactCard.setOnCheckedChangeListener((buttonView, isChecked) ->
                settingsManager.setCardCompact(isChecked));

        // Card Display Checkboxes
        cbShowImage.setOnCheckedChangeListener((buttonView, isChecked) ->
                settingsManager.setShowCardImage(isChecked));

        cbShowSellingPrice.setOnCheckedChangeListener((buttonView, isChecked) ->
                settingsManager.setShowCardSellingPrice(isChecked));

        cbShowPurchasePrice.setOnCheckedChangeListener((buttonView, isChecked) ->
                settingsManager.setShowCardPurchasePrice(isChecked));

        cbShowCategory.setOnCheckedChangeListener((buttonView, isChecked) ->
                settingsManager.setShowCardCategory(isChecked));

        cbShowColor.setOnCheckedChangeListener((buttonView, isChecked) ->
                settingsManager.setShowCardColor(isChecked));

        cbShowSize.setOnCheckedChangeListener((buttonView, isChecked) ->
                settingsManager.setShowCardSize(isChecked));

        // Currency text watcher
        etCurrencySymbol.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && s.length() > 0) {
                    settingsManager.setCurrencySymbol(s.toString().trim());
                }
            }
        });

        // Backup toggles
        switchAutoLocalBackup.setOnCheckedChangeListener((buttonView, isChecked) ->
                settingsManager.setAutoLocalBackupEnabled(isChecked));

        switchGoogleDriveBackup.setOnCheckedChangeListener((buttonView, isChecked) ->
                settingsManager.setGoogleDriveBackupEnabled(isChecked));

        switchBackupWifiOnly.setOnCheckedChangeListener((buttonView, isChecked) ->
                settingsManager.setBackupWifiOnly(isChecked));

        switchShowBackupWarning.setOnCheckedChangeListener((buttonView, isChecked) ->
                settingsManager.setShowBackupWarningBanner(isChecked));

        btnChooseBackupFolder.setOnClickListener(v -> safFolderLauncher.launch(null));

        btnManualBackup.setOnClickListener(v -> handleManualBackup());

        etThreshold.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    int val = Integer.parseInt(s.toString().trim());
                    settingsManager.setNewProductsThreshold(val);
                } catch (Exception ignored) {
                }
            }
        });

        switchBackupOnEdit.setOnCheckedChangeListener((buttonView, isChecked) ->
                settingsManager.setBackupOnEdit(isChecked));

        switchBackupOnDelete.setOnCheckedChangeListener((buttonView, isChecked) ->
                settingsManager.setBackupOnDelete(isChecked));

        etMaxRetention.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    int val = Integer.parseInt(s.toString().trim());
                    settingsManager.setMaxRetainedBackups(val);
                } catch (Exception ignored) {
                }
            }
        });

        // General
        switchConfirmDelete.setOnCheckedChangeListener((buttonView, isChecked) ->
                settingsManager.setConfirmOnDelete(isChecked));
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        TextInputLayout tilCurrent = dialogView.findViewById(R.id.til_current_password);
        TextInputEditText etCurrent = dialogView.findViewById(R.id.et_current_password);
        TextInputLayout tilNew = dialogView.findViewById(R.id.til_new_password);
        TextInputEditText etNew = dialogView.findViewById(R.id.et_new_password);
        TextInputLayout tilConfirmNew = dialogView.findViewById(R.id.til_confirm_new_password);
        TextInputEditText etConfirmNew = dialogView.findViewById(R.id.et_confirm_new_password);

        boolean hasExistingPassword = securityManager.isPasswordSet();
        if (!hasExistingPassword) {
            tilCurrent.setVisibility(View.GONE);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.change_password_title)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_save_password, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.show();

        Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnSave.setOnClickListener(v -> {
            tilCurrent.setError(null);
            tilNew.setError(null);
            tilConfirmNew.setError(null);

            if (hasExistingPassword) {
                String current = etCurrent.getText() != null ? etCurrent.getText().toString().trim() : "";
                if (current.isEmpty()) {
                    tilCurrent.setError(getString(R.string.password_empty_error));
                    return;
                }
                if (!securityManager.verifyPassword(current)) {
                    tilCurrent.setError(getString(R.string.password_incorrect_error));
                    return;
                }
            }

            String newPass = etNew.getText() != null ? etNew.getText().toString().trim() : "";
            if (newPass.isEmpty()) {
                tilNew.setError(getString(R.string.password_empty_error));
                return;
            }
            if (newPass.length() < 4) {
                tilNew.setError(getString(R.string.password_too_short_error));
                return;
            }

            String confirmNew = etConfirmNew.getText() != null ? etConfirmNew.getText().toString().trim() : "";
            if (!newPass.equals(confirmNew)) {
                tilConfirmNew.setError(getString(R.string.password_mismatch_error));
                return;
            }

            boolean updated = securityManager.setPassword(newPass);
            if (updated) {
                SecurityManager.setAppUnlocked(true);
                dialog.dismiss();
                Toast.makeText(SettingsActivity.this, R.string.password_changed_success, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SettingsActivity.this, "فشل حفظ كلمة المرور، يرجى المحاولة ثانية", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}
