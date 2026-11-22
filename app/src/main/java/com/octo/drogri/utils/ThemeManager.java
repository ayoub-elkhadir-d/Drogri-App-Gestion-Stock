package com.octo.drogri.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.StyleRes;

import com.octo.drogri.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages the 10 customizable application themes with seamless Light/Dark mode integration.
 */
public class ThemeManager {

    public static class ThemeItem {
        public final int id;
        public final String key;
        public final int nameResId;
        public final String primaryHex;
        public final String secondaryHex;
        public final String backgroundHex;
        @StyleRes
        public final int styleResId;

        public ThemeItem(int id, String key, int nameResId,
                         String primaryHex, String secondaryHex, String backgroundHex,
                         @StyleRes int styleResId) {
            this.id = id;
            this.key = key;
            this.nameResId = nameResId;
            this.primaryHex = primaryHex;
            this.secondaryHex = secondaryHex;
            this.backgroundHex = backgroundHex;
            this.styleResId = styleResId;
        }
    }

    private static final List<ThemeItem> THEMES;

    static {
        List<ThemeItem> list = new ArrayList<>();
        list.add(new ThemeItem(0, "ocean_blue", R.string.theme_ocean_blue, "#2563EB", "#60A5FA", "#F8FAFC", R.style.Theme_Drogri_OceanBlue));
        list.add(new ThemeItem(1, "forest_green", R.string.theme_forest_green, "#15803D", "#4ADE80", "#F7FAF7", R.style.Theme_Drogri_ForestGreen));
        list.add(new ThemeItem(2, "royal_purple", R.string.theme_royal_purple, "#7C3AED", "#A78BFA", "#FAF8FF", R.style.Theme_Drogri_RoyalPurple));
        list.add(new ThemeItem(3, "sunset_orange", R.string.theme_sunset_orange, "#EA580C", "#FB923C", "#FFFAF5", R.style.Theme_Drogri_SunsetOrange));
        list.add(new ThemeItem(4, "golden_yellow", R.string.theme_golden_yellow, "#CA8A04", "#FACC15", "#FFFEF5", R.style.Theme_Drogri_GoldenYellow));
        list.add(new ThemeItem(5, "rose_pink", R.string.theme_rose_pink, "#DB2777", "#F472B6", "#FFF7FA", R.style.Theme_Drogri_RosePink));
        list.add(new ThemeItem(6, "teal", R.string.theme_teal, "#0F766E", "#2DD4BF", "#F5FAF9", R.style.Theme_Drogri_Teal));
        list.add(new ThemeItem(7, "midnight", R.string.theme_midnight, "#334155", "#64748B", "#F8FAFC", R.style.Theme_Drogri_Midnight));
        list.add(new ThemeItem(8, "burgundy", R.string.theme_burgundy, "#9F1239", "#FB7185", "#FFF7F8", R.style.Theme_Drogri_Burgundy));
        list.add(new ThemeItem(9, "emerald", R.string.theme_emerald, "#047857", "#34D399", "#F5FBF8", R.style.Theme_Drogri_Emerald));
        THEMES = Collections.unmodifiableList(list);
    }

    public static List<ThemeItem> getThemes() {
        return THEMES;
    }

    public static ThemeItem getTheme(int index) {
        if (index >= 0 && index < THEMES.size()) {
            return THEMES.get(index);
        }
        return THEMES.get(0); // Default Ocean Blue
    }

    public static void applyTheme(Activity activity) {
        int selectedIndex = SettingsManager.getInstance(activity).getSelectedThemeIndex();
        ThemeItem theme = getTheme(selectedIndex);
        activity.setTheme(theme.styleResId);
    }

    @ColorInt
    public static int getThemeColor(Context context, @AttrRes int attrRes) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = context.obtainStyledAttributes(typedValue.data, new int[]{attrRes});
        int color = a.getColor(0, 0);
        a.recycle();
        return color;
    }
}
