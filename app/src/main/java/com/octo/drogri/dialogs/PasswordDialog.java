package com.octo.drogri.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.octo.drogri.R;
import com.octo.drogri.utils.SecurityManager;

/**
 * Reusable password confirmation dialog for sensitive operations and privacy verification.
 */
public class PasswordDialog {

    public interface OnPasswordVerifiedListener {
        void onVerified();
        default void onCancelled() {}
    }

    public static void show(Context context, OnPasswordVerifiedListener listener) {
        show(context, null, listener);
    }

    public static void show(Context context, String customMessage, OnPasswordVerifiedListener listener) {
        SecurityManager securityManager = SecurityManager.getInstance(context);

        // If password has never been set, allow operation directly
        if (!securityManager.isPasswordSet()) {
            if (listener != null) {
                listener.onVerified();
            }
            return;
        }

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_password_confirm, null);
        TextView tvMessage = view.findViewById(R.id.tv_dialog_message);
        TextInputLayout tilPassword = view.findViewById(R.id.til_dialog_password);
        TextInputEditText etPassword = view.findViewById(R.id.et_dialog_password);

        if (customMessage != null && !customMessage.trim().isEmpty()) {
            tvMessage.setText(customMessage);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.confirm_password_dialog_title)
                .setView(view)
                .setPositiveButton(R.string.btn_confirm, null) // Set null to override and prevent auto-dismiss on error
                .setNegativeButton(R.string.cancel, (d, which) -> {
                    if (listener != null) {
                        listener.onCancelled();
                    }
                })
                .setOnCancelListener(d -> {
                    if (listener != null) {
                        listener.onCancelled();
                    }
                })
                .create();

        dialog.show();

        Button btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnPositive.setOnClickListener(v -> {
            tilPassword.setError(null);
            String entered = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            if (entered.isEmpty()) {
                tilPassword.setError(context.getString(R.string.password_empty_error));
                return;
            }

            boolean isValid = securityManager.verifyPassword(entered);
            if (isValid) {
                dialog.dismiss();
                if (listener != null) {
                    listener.onVerified();
                }
            } else {
                tilPassword.setError(context.getString(R.string.password_incorrect_error));
            }
        });
    }
}
