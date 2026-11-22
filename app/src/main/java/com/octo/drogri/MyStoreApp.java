package com.octo.drogri;

import android.app.Application;

import com.octo.drogri.utils.SettingsManager;

/**
 * Application class for MyStore ("متجري").
 * Initializes application-level configurations such as theme mode.
 */
public class MyStoreApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SettingsManager settings = SettingsManager.getInstance(this);
        SettingsManager.applyTheme(settings.getThemeMode());
    }
}
