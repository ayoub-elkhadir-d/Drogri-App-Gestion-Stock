package com.octo.drogri.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import androidx.biometric.BiometricManager;

/**
 * Manages application security, cryptographic password hashing with random salt,
 * session unlock state, biometric authentication support, and sensitive operation policies.
 */
public class SecurityManager {

    public static final int AUTH_METHOD_PASSWORD = 1;
    public static final int AUTH_METHOD_BIOMETRIC = 2;

    private static final String PREF_NAME = "mystore_security";
    private static final String KEY_PASSWORD_HASH = "sec_pwd_hash";
    private static final String KEY_PASSWORD_SALT = "sec_pwd_salt";
    private static final String KEY_IS_SETUP = "sec_is_setup";
    private static final String KEY_AUTH_METHOD = "sec_auth_method";
    private static final String KEY_PROTECT_SENSITIVE = "sec_protect_sensitive";
    private static final String KEY_MASK_PURCHASE_PRICE = "sec_mask_purchase_price";

    private static final int SALT_BYTES = 16;
    private static final int HASH_ITERATIONS = 5000;

    private static SecurityManager instance;
    private final SharedPreferences prefs;

    // In-memory unlock state for the current user session
    private static boolean isAppUnlocked = false;

    private SecurityManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SecurityManager getInstance(Context context) {
        if (instance == null) {
            instance = new SecurityManager(context);
        }
        return instance;
    }

    public static boolean isAppUnlocked() {
        return isAppUnlocked;
    }

    public static void setAppUnlocked(boolean unlocked) {
        isAppUnlocked = unlocked;
    }

    public static void lockApp() {
        isAppUnlocked = false;
    }

    /**
     * Checks whether the device hardware supports biometric authentication (Fingerprint, Face, etc.)
     * and has biometrics enrolled.
     */
    public static boolean isBiometricSupported(Context context) {
        if (context == null) return false;
        try {
            BiometricManager biometricManager = BiometricManager.from(context);
            int canAuth = biometricManager.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK
            );
            return canAuth == BiometricManager.BIOMETRIC_SUCCESS;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Selected Authentication Method: AUTH_METHOD_PASSWORD or AUTH_METHOD_BIOMETRIC.
     */
    public int getAuthMethod() {
        return prefs.getInt(KEY_AUTH_METHOD, AUTH_METHOD_PASSWORD);
    }

    public void setAuthMethod(int method) {
        prefs.edit().putInt(KEY_AUTH_METHOD, method).apply();
    }

    public boolean isBiometricAuthEnabled(Context context) {
        return getAuthMethod() == AUTH_METHOD_BIOMETRIC && isBiometricSupported(context);
    }

    /**
     * Checks whether a password has been created on the device.
     */
    public boolean isPasswordSet() {
        return prefs.getBoolean(KEY_IS_SETUP, false) && prefs.contains(KEY_PASSWORD_HASH);
    }

    /**
     * Creates and stores a salted hash of the password.
     */
    public boolean setPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }

        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);

        byte[] hash = hashPassword(password.trim(), salt);
        if (hash == null) {
            return false;
        }

        String saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP);
        String hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP);

        return prefs.edit()
                .putString(KEY_PASSWORD_SALT, saltBase64)
                .putString(KEY_PASSWORD_HASH, hashBase64)
                .putBoolean(KEY_IS_SETUP, true)
                .commit();
    }

    /**
     * Verifies an entered password against the stored salted hash using constant-time comparison.
     */
    public boolean verifyPassword(String enteredPassword) {
        if (enteredPassword == null || !isPasswordSet()) {
            return false;
        }

        String saltBase64 = prefs.getString(KEY_PASSWORD_SALT, null);
        String storedHashBase64 = prefs.getString(KEY_PASSWORD_HASH, null);

        if (saltBase64 == null || storedHashBase64 == null) {
            return false;
        }

        try {
            byte[] salt = Base64.decode(saltBase64, Base64.NO_WRAP);
            byte[] storedHash = Base64.decode(storedHashBase64, Base64.NO_WRAP);
            byte[] computedHash = hashPassword(enteredPassword.trim(), salt);

            if (computedHash == null) {
                return false;
            }

            return MessageDigest.isEqual(storedHash, computedHash);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Changes password after verifying the old password.
     */
    public boolean changePassword(String currentPassword, String newPassword) {
        if (!verifyPassword(currentPassword)) {
            return false;
        }
        return setPassword(newPassword);
    }

    /**
     * Sensitive operation protection toggle (Default: true)
     */
    public boolean isProtectSensitiveOperations() {
        return prefs.getBoolean(KEY_PROTECT_SENSITIVE, true);
    }

    public void setProtectSensitiveOperations(boolean protect) {
        prefs.edit().putBoolean(KEY_PROTECT_SENSITIVE, protect).apply();
    }

    /**
     * Mask purchase price by default toggle (Default: true)
     */
    public boolean isMaskPurchasePrice() {
        return prefs.getBoolean(KEY_MASK_PURCHASE_PRICE, true);
    }

    public void setMaskPurchasePrice(boolean mask) {
        prefs.edit().putBoolean(KEY_MASK_PURCHASE_PRICE, mask).apply();
    }

    /**
     * Computes a multi-iteration salted SHA-256 hash.
     */
    private byte[] hashPassword(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] current = md.digest(password.getBytes(StandardCharsets.UTF_8));
            for (int i = 1; i < HASH_ITERATIONS; i++) {
                md.reset();
                md.update(current);
                current = md.digest(salt);
            }
            return current;
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
