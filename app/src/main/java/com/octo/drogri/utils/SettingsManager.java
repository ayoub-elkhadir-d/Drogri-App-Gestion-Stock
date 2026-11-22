package com.octo.drogri.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Manages all dynamic user-configurable application preferences.
 */
public class SettingsManager {

    private static final String PREF_NAME = "mystore_settings";
    private static SettingsManager instance;
    private final SharedPreferences prefs;

    // Keys
    public static final String KEY_THEME_MODE = "theme_mode"; // 0: Light, 1: Dark, 2: System
    public static final String KEY_FONT_SIZE = "font_size"; // "small", "medium", "large"
    public static final String KEY_CARD_COMPACT = "card_compact";

    // Card Display Keys
    public static final String KEY_SHOW_IMAGE = "show_card_image";
    public static final String KEY_SHOW_NAME = "show_card_name";
    public static final String KEY_SHOW_SELLING_PRICE = "show_card_selling_price";
    public static final String KEY_SHOW_PURCHASE_PRICE = "show_card_purchase_price";
    public static final String KEY_SHOW_CATEGORY = "show_card_category";
    public static final String KEY_SHOW_COLOR = "show_card_color";
    public static final String KEY_SHOW_SIZE = "show_card_size";

    // Currency
    public static final String KEY_CURRENCY_SYMBOL = "currency_symbol";

    // Backup Keys
    public static final String KEY_AUTO_LOCAL_BACKUP = "auto_local_backup";
    public static final String KEY_GOOGLE_DRIVE_BACKUP = "google_drive_backup";
    public static final String KEY_GOOGLE_EMAIL = "google_account_email";
    public static final String KEY_NEW_PRODUCTS_THRESHOLD = "new_products_threshold";
    public static final String KEY_BACKUP_ON_EDIT = "backup_on_edit";
    public static final String KEY_BACKUP_ON_DELETE = "backup_on_delete";
    public static final String KEY_MAX_RETAINED_BACKUPS = "max_retained_backups";
    public static final String KEY_BACKUP_NOTIFICATIONS = "backup_notifications";
    public static final String KEY_LAST_LOCAL_BACKUP = "last_local_backup_time";
    public static final String KEY_LAST_LOCAL_NAME = "last_local_backup_name";
    public static final String KEY_LAST_LOCAL_SIZE = "last_local_backup_size";
    public static final String KEY_LAST_CLOUD_BACKUP = "last_cloud_backup_time";
    public static final String KEY_LAST_CLOUD_NAME = "last_cloud_backup_name";
    public static final String KEY_LAST_CLOUD_ID = "last_cloud_backup_id";
    public static final String KEY_LAST_CLOUD_SIZE = "last_cloud_backup_size";
    public static final String KEY_LAST_CLOUD_ERROR = "last_cloud_backup_error";
    public static final String KEY_PRODUCTS_SINCE_BACKUP = "products_added_since_backup";
    public static final String KEY_SAF_BACKUP_TREE_URI = "saf_backup_tree_uri";
    public static final String KEY_SAF_BACKUP_DISPLAY_NAME = "saf_backup_display_name";
    public static final String KEY_CLOUD_BACKUP_PENDING = "cloud_backup_pending";
    public static final String KEY_SHOW_BACKUP_WARNING_BANNER = "show_backup_warning_banner";
    public static final String KEY_BACKUP_WIFI_ONLY = "backup_wifi_only";

    // General
    public static final String KEY_CONFIRM_DELETE = "confirm_on_delete";
    public static final String KEY_DEFAULT_SORT = "default_sort"; // "newest", "price_asc", "price_desc", "name_asc"

    private SettingsManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SettingsManager getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsManager(context);
        }
        return instance;
    }

    // Appearance
    public static final String KEY_SELECTED_THEME_INDEX = "selected_theme_index"; // 0-9

    public int getThemeMode() {
        return prefs.getInt(KEY_THEME_MODE, 0); // Default 0 (Light)
    }

    public void setThemeMode(int mode) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
        applyTheme(mode);
    }

    public int getSelectedThemeIndex() {
        return prefs.getInt(KEY_SELECTED_THEME_INDEX, 0);
    }

    public void setSelectedThemeIndex(int index) {
        prefs.edit().putInt(KEY_SELECTED_THEME_INDEX, index).apply();
    }

    public static void applyTheme(int mode) {
        if (mode == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (mode == 2) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    public String getFontSize() {
        return prefs.getString(KEY_FONT_SIZE, "medium");
    }

    public void setFontSize(String size) {
        prefs.edit().putString(KEY_FONT_SIZE, size).apply();
    }

    public boolean isCardCompact() {
        return prefs.getBoolean(KEY_CARD_COMPACT, false);
    }

    public void setCardCompact(boolean compact) {
        prefs.edit().putBoolean(KEY_CARD_COMPACT, compact).apply();
    }

    // Card Display Toggles
    public boolean showCardImage() {
        return prefs.getBoolean(KEY_SHOW_IMAGE, true);
    }

    public void setShowCardImage(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_IMAGE, show).apply();
    }

    public boolean showCardSellingPrice() {
        return prefs.getBoolean(KEY_SHOW_SELLING_PRICE, true);
    }

    public void setShowCardSellingPrice(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_SELLING_PRICE, show).apply();
    }

    public boolean showCardPurchasePrice() {
        return prefs.getBoolean(KEY_SHOW_PURCHASE_PRICE, false);
    }

    public void setShowCardPurchasePrice(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_PURCHASE_PRICE, show).apply();
    }

    public boolean showCardCategory() {
        return prefs.getBoolean(KEY_SHOW_CATEGORY, true);
    }

    public void setShowCardCategory(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_CATEGORY, show).apply();
    }

    public boolean showCardColor() {
        return prefs.getBoolean(KEY_SHOW_COLOR, true);
    }

    public void setShowCardColor(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_COLOR, show).apply();
    }

    public boolean showCardSize() {
        return prefs.getBoolean(KEY_SHOW_SIZE, false);
    }

    public void setShowCardSize(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_SIZE, show).apply();
    }

    // Currency
    public String getCurrencySymbol() {
        return prefs.getString(KEY_CURRENCY_SYMBOL, "د.م");
    }

    public void setCurrencySymbol(String symbol) {
        prefs.edit().putString(KEY_CURRENCY_SYMBOL, symbol != null ? symbol.trim() : "د.م").apply();
    }

    // Backup Settings
    public boolean isAutoLocalBackupEnabled() {
        return prefs.getBoolean(KEY_AUTO_LOCAL_BACKUP, true);
    }

    public void setAutoLocalBackupEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_LOCAL_BACKUP, enabled).apply();
    }

    public boolean isGoogleDriveBackupEnabled() {
        return prefs.getBoolean(KEY_GOOGLE_DRIVE_BACKUP, false);
    }

    public void setGoogleDriveBackupEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_GOOGLE_DRIVE_BACKUP, enabled).apply();
    }

    public String getGoogleAccountEmail() {
        return prefs.getString(KEY_GOOGLE_EMAIL, "");
    }

    public void setGoogleAccountEmail(String email) {
        prefs.edit().putString(KEY_GOOGLE_EMAIL, email != null ? email.trim() : "").apply();
    }

    public int getNewProductsThreshold() {
        return prefs.getInt(KEY_NEW_PRODUCTS_THRESHOLD, 10);
    }

    public void setNewProductsThreshold(int threshold) {
        prefs.edit().putInt(KEY_NEW_PRODUCTS_THRESHOLD, Math.max(1, threshold)).apply();
    }

    public boolean isBackupOnEdit() {
        return prefs.getBoolean(KEY_BACKUP_ON_EDIT, true);
    }

    public void setBackupOnEdit(boolean enabled) {
        prefs.edit().putBoolean(KEY_BACKUP_ON_EDIT, enabled).apply();
    }

    public boolean isBackupOnDelete() {
        return prefs.getBoolean(KEY_BACKUP_ON_DELETE, true);
    }

    public void setBackupOnDelete(boolean enabled) {
        prefs.edit().putBoolean(KEY_BACKUP_ON_DELETE, enabled).apply();
    }

    public int getMaxRetainedBackups() {
        return prefs.getInt(KEY_MAX_RETAINED_BACKUPS, 5);
    }

    public void setMaxRetainedBackups(int max) {
        prefs.edit().putInt(KEY_MAX_RETAINED_BACKUPS, Math.max(1, max)).apply();
    }

    public boolean isBackupNotificationsEnabled() {
        return prefs.getBoolean(KEY_BACKUP_NOTIFICATIONS, true);
    }

    public void setBackupNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BACKUP_NOTIFICATIONS, enabled).apply();
    }

    public long getLastLocalBackupTime() {
        return prefs.getLong(KEY_LAST_LOCAL_BACKUP, 0);
    }

    public void setLastLocalBackupTime(long time) {
        prefs.edit().putLong(KEY_LAST_LOCAL_BACKUP, time).apply();
    }

    public String getLastLocalBackupName() {
        return prefs.getString(KEY_LAST_LOCAL_NAME, "");
    }

    public void setLastLocalBackupName(String name) {
        prefs.edit().putString(KEY_LAST_LOCAL_NAME, name).apply();
    }

    public long getLastLocalBackupSize() {
        return prefs.getLong(KEY_LAST_LOCAL_SIZE, 0);
    }

    public void setLastLocalBackupSize(long size) {
        prefs.edit().putLong(KEY_LAST_LOCAL_SIZE, size).apply();
    }

    public long getLastCloudBackupTime() {
        return prefs.getLong(KEY_LAST_CLOUD_BACKUP, 0);
    }

    public void setLastCloudBackupTime(long time) {
        prefs.edit().putLong(KEY_LAST_CLOUD_BACKUP, time).apply();
    }

    public String getLastCloudBackupName() {
        return prefs.getString(KEY_LAST_CLOUD_NAME, "");
    }

    public void setLastCloudBackupName(String name) {
        prefs.edit().putString(KEY_LAST_CLOUD_NAME, name).apply();
    }

    public String getLastCloudBackupId() {
        return prefs.getString(KEY_LAST_CLOUD_ID, "");
    }

    public void setLastCloudBackupId(String id) {
        prefs.edit().putString(KEY_LAST_CLOUD_ID, id).apply();
    }

    public long getLastCloudBackupSize() {
        return prefs.getLong(KEY_LAST_CLOUD_SIZE, 0);
    }

    public void setLastCloudBackupSize(long size) {
        prefs.edit().putLong(KEY_LAST_CLOUD_SIZE, size).apply();
    }

    public String getLastCloudError() {
        return prefs.getString(KEY_LAST_CLOUD_ERROR, null);
    }

    public void setLastCloudError(String error) {
        prefs.edit().putString(KEY_LAST_CLOUD_ERROR, error).apply();
    }

    public int getProductsAddedSinceBackup() {
        return prefs.getInt(KEY_PRODUCTS_SINCE_BACKUP, 0);
    }

    public void incrementProductsAddedSinceBackup() {
        int count = getProductsAddedSinceBackup() + 1;
        prefs.edit().putInt(KEY_PRODUCTS_SINCE_BACKUP, count).apply();
    }

    public void resetProductsAddedSinceBackup() {
        prefs.edit().putInt(KEY_PRODUCTS_SINCE_BACKUP, 0).apply();
    }

    public String getSafBackupTreeUri() {
        return prefs.getString(KEY_SAF_BACKUP_TREE_URI, null);
    }

    public void setSafBackupTreeUri(String uri) {
        prefs.edit().putString(KEY_SAF_BACKUP_TREE_URI, uri).apply();
    }

    public String getSafBackupDisplayName() {
        return prefs.getString(KEY_SAF_BACKUP_DISPLAY_NAME, null);
    }

    public void setSafBackupDisplayName(String name) {
        prefs.edit().putString(KEY_SAF_BACKUP_DISPLAY_NAME, name).apply();
    }

    public boolean isCloudBackupPending() {
        return prefs.getBoolean(KEY_CLOUD_BACKUP_PENDING, false);
    }

    public void setCloudBackupPending(boolean pending) {
        prefs.edit().putBoolean(KEY_CLOUD_BACKUP_PENDING, pending).apply();
    }

    public boolean isShowBackupWarningBanner() {
        return prefs.getBoolean(KEY_SHOW_BACKUP_WARNING_BANNER, true);
    }

    public void setShowBackupWarningBanner(boolean show) {
        prefs.edit().putBoolean(KEY_SHOW_BACKUP_WARNING_BANNER, show).apply();
    }

    public boolean isBackupWifiOnly() {
        return prefs.getBoolean(KEY_BACKUP_WIFI_ONLY, false);
    }

    public void setBackupWifiOnly(boolean wifiOnly) {
        prefs.edit().putBoolean(KEY_BACKUP_WIFI_ONLY, wifiOnly).apply();
    }

    // General
    public boolean isConfirmOnDelete() {
        return prefs.getBoolean(KEY_CONFIRM_DELETE, true);
    }

    public void setConfirmOnDelete(boolean confirm) {
        prefs.edit().putBoolean(KEY_CONFIRM_DELETE, confirm).apply();
    }

    public String getDefaultSort() {
        return prefs.getString(KEY_DEFAULT_SORT, "newest");
    }

    public void setDefaultSort(String sort) {
        prefs.edit().putString(KEY_DEFAULT_SORT, sort).apply();
    }
}
