package com.octo.drogri.utils;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.octo.drogri.R;
import com.octo.drogri.dialogs.PasswordDialog;

import java.util.concurrent.Executor;

/**
 * Centralized authentication helper supporting AndroidX BiometricPrompt
 * (Fingerprint / Face recognition) with seamless Password fallback.
 */
public class AuthHelper {

    public interface AuthCallback {
        void onSuccess();
        default void onFailed() {}
        default void onCancelled() {}
    }

    /**
     * Authenticates the user for sensitive operations (deletion, settings, etc.)
     * using biometric authentication if enabled, or falling back to PasswordDialog.
     */
    public static void authenticate(@NonNull FragmentActivity activity, String reasonMessage, @NonNull AuthCallback callback) {
        SecurityManager securityManager = SecurityManager.getInstance(activity);

        // If protection is not enabled or password never set, proceed directly
        if (!securityManager.isProtectSensitiveOperations() || !securityManager.isPasswordSet()) {
            callback.onSuccess();
            return;
        }

        // If biometric is enabled and supported on this device
        if (securityManager.isBiometricAuthEnabled(activity)) {
            showBiometricPrompt(activity, reasonMessage, new AuthCallback() {
                @Override
                public void onSuccess() {
                    callback.onSuccess();
                }

                @Override
                public void onFailed() {
                    // Fall back to password dialog on biometric negative button or fallback
                    showPasswordFallback(activity, reasonMessage, callback);
                }

                @Override
                public void onCancelled() {
                    callback.onCancelled();
                }
            });
        } else {
            // Password dialog
            showPasswordFallback(activity, reasonMessage, callback);
        }
    }

    private static void showPasswordFallback(@NonNull FragmentActivity activity, String reasonMessage, @NonNull AuthCallback callback) {
        PasswordDialog.show(activity, reasonMessage, new PasswordDialog.OnPasswordVerifiedListener() {
            @Override
            public void onVerified() {
                callback.onSuccess();
            }

            @Override
            public void onCancelled() {
                callback.onCancelled();
            }
        });
    }

    /**
     * Displays system BiometricPrompt.
     */
    public static void showBiometricPrompt(@NonNull FragmentActivity activity, String subtitle, @NonNull AuthCallback callback) {
        Executor executor = ContextCompat.getMainExecutor(activity);

        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    callback.onFailed(); // User tapped "استخدام كلمة المرور"
                } else if (errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                    callback.onCancelled();
                } else {
                    // Lockout or other error: fallback to password
                    callback.onFailed();
                }
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                callback.onSuccess();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });

        String promptSubtitle = (subtitle != null && !subtitle.trim().isEmpty())
                ? subtitle
                : activity.getString(R.string.biometric_prompt_subtitle);

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.biometric_prompt_title))
                .setSubtitle(promptSubtitle)
                .setNegativeButtonText(activity.getString(R.string.biometric_prompt_negative))
                .build();

        biometricPrompt.authenticate(promptInfo);
    }
}
