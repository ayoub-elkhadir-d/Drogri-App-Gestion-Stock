package com.octo.drogri;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.octo.drogri.utils.ThemeManager;

/**
 * Base Activity providing centralized dynamic theme management across all application screens.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
    }
}
