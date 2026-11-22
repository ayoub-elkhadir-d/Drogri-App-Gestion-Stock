package com.octo.drogri;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.octo.drogri.utils.AuthHelper;
import com.octo.drogri.utils.SecurityManager;

/**
 * Activity for password setup on first launch, biometric selection,
 * and secure unlocking on application startup.
 */
public class PasswordActivity extends BaseActivity {

    public static final String EXTRA_MODE = "extra_mode";
    public static final int MODE_SETUP = 1;
    public static final int MODE_UNLOCK = 2;

    private SecurityManager securityManager;
    private int currentMode = MODE_UNLOCK;
    private boolean biometricTriggeredOnLaunch = false;

    private TextView tvTitle;
    private TextView tvDesc;
    private TextInputLayout tilPassword;
    private TextInputEditText etPassword;
    private TextInputLayout tilConfirmPassword;
    private TextInputEditText etConfirmPassword;
    private LinearLayout layoutAuthMethod;
    private RadioGroup rgAuthMethod;
    private RadioButton rbAuthPassword;
    private RadioButton rbAuthBiometric;
    private MaterialButton btnAction;
    private MaterialButton btnBiometricUnlock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);

        securityManager = SecurityManager.getInstance(this);

        if (getIntent().hasExtra(EXTRA_MODE)) {
            currentMode = getIntent().getIntExtra(EXTRA_MODE, MODE_UNLOCK);
        } else {
            currentMode = securityManager.isPasswordSet() ? MODE_UNLOCK : MODE_SETUP;
        }

        initViews();
        setupMode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentMode == MODE_UNLOCK && !biometricTriggeredOnLaunch) {
            biometricTriggeredOnLaunch = true;
            if (securityManager.isBiometricAuthEnabled(this)) {
                triggerBiometricUnlock();
            }
        }
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_password_title);
        tvDesc = findViewById(R.id.tv_password_desc);
        tilPassword = findViewById(R.id.til_password);
        etPassword = findViewById(R.id.et_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        layoutAuthMethod = findViewById(R.id.layout_auth_method);
        rgAuthMethod = findViewById(R.id.rg_auth_method);
        rbAuthPassword = findViewById(R.id.rb_auth_password);
        rbAuthBiometric = findViewById(R.id.rb_auth_biometric);
        btnAction = findViewById(R.id.btn_password_action);
        btnBiometricUnlock = findViewById(R.id.btn_biometric_unlock);

        btnAction.setOnClickListener(v -> handleSubmit());
        btnBiometricUnlock.setOnClickListener(v -> triggerBiometricUnlock());
    }

    private void setupMode() {
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        if (currentMode == MODE_SETUP) {
            tvTitle.setText(R.string.create_password_title);
            tvDesc.setText(R.string.create_password_desc);
            tilConfirmPassword.setVisibility(View.VISIBLE);
            layoutAuthMethod.setVisibility(View.VISIBLE);
            btnBiometricUnlock.setVisibility(View.GONE);
            btnAction.setText(R.string.btn_save_password);

            // Check if biometric is supported on device
            boolean biometricSupported = SecurityManager.isBiometricSupported(this);
            rbAuthBiometric.setEnabled(biometricSupported);
            if (!biometricSupported) {
                rbAuthBiometric.setText(getString(R.string.auth_method_biometric) + " (غير مدعوم على الجهاز)");
            }
        } else {
            tvTitle.setText(R.string.unlock_title);
            tvDesc.setText(R.string.unlock_desc);
            tilConfirmPassword.setVisibility(View.GONE);
            layoutAuthMethod.setVisibility(View.GONE);
            btnAction.setText(R.string.btn_unlock);

            if (securityManager.isBiometricAuthEnabled(this)) {
                btnBiometricUnlock.setVisibility(View.VISIBLE);
            } else {
                btnBiometricUnlock.setVisibility(View.GONE);
            }
        }
    }

    private void triggerBiometricUnlock() {
        AuthHelper.showBiometricPrompt(this, getString(R.string.unlock_desc), new AuthHelper.AuthCallback() {
            @Override
            public void onSuccess() {
                SecurityManager.setAppUnlocked(true);
                openMainActivity();
            }

            @Override
            public void onFailed() {
                // User cancelled or chose password fallback - remain on password screen
            }

            @Override
            public void onCancelled() {
                // Cancelled - stay on password screen
            }
        });
    }

    private void handleSubmit() {
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.password_empty_error));
            return;
        }

        if (password.length() < 4) {
            tilPassword.setError(getString(R.string.password_too_short_error));
            return;
        }

        if (currentMode == MODE_SETUP) {
            String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";
            if (!password.equals(confirmPassword)) {
                tilConfirmPassword.setError(getString(R.string.password_mismatch_error));
                return;
            }

            // Save auth method choice
            boolean isBiometricChosen = rbAuthBiometric.isChecked();
            if (isBiometricChosen) {
                if (SecurityManager.isBiometricSupported(this)) {
                    securityManager.setAuthMethod(SecurityManager.AUTH_METHOD_BIOMETRIC);
                } else {
                    securityManager.setAuthMethod(SecurityManager.AUTH_METHOD_PASSWORD);
                    Toast.makeText(this, R.string.biometric_not_available, Toast.LENGTH_LONG).show();
                }
            } else {
                securityManager.setAuthMethod(SecurityManager.AUTH_METHOD_PASSWORD);
            }

            boolean saved = securityManager.setPassword(password);
            if (saved) {
                SecurityManager.setAppUnlocked(true);
                Toast.makeText(this, R.string.password_created_success, Toast.LENGTH_SHORT).show();
                openMainActivity();
            } else {
                Toast.makeText(this, "فشل حفظ كلمة المرور، يرجى المحاولة ثانية", Toast.LENGTH_SHORT).show();
            }
        } else {
            boolean valid = securityManager.verifyPassword(password);
            if (valid) {
                SecurityManager.setAppUnlocked(true);
                openMainActivity();
            } else {
                tilPassword.setError(getString(R.string.password_incorrect_error));
            }
        }
    }

    private void openMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Prevent bypassing lock screen
        finishAffinity();
    }
}
