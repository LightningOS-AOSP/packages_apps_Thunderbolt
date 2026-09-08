/*
 * Copyright (C) 2026 LightningOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lightning.thunderbolt;

import android.content.Context;
import android.provider.Settings;

import androidx.annotation.StyleRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.widget.SettingsMainSwitchPreference;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.widget.BannerMessagePreference;
import com.android.settingslib.widget.ButtonPreference;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.LayoutPreference;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;
import com.android.settingslib.widget.SpacePreference;
import com.android.settingslib.widget.TopIntroPreference;

import com.lightning.thunderbolt.preferences.CustomSeekBarPreference;
import com.lightning.thunderbolt.preferences.colorpicker.ColorPickerPreference;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared Thunderbolt theming helpers.
 *
 * <p>Controls whether the whole Settings app follows the "Lightning" layout style (a
 * {@link Settings.Secure} switch toggled from Bolt Bar) and applies the Thunderbolt row/category
 * layouts to any preference screen.
 */
public final class ThunderboltTheme {

    /** {@link Settings.Secure} key: 0 = AOSP style, 1 = Lightning style. */
    public static final String SETTING_KEY = "lightning_layout_style";

    /** Cache of classes known to declare their own {@link Preference#onBindViewHolder}. */
    private static final Map<Class<?>, Boolean> CUSTOM_BINDER_CACHE = new HashMap<>();

    private ThunderboltTheme() {
    }

    /** Whether the whole Settings app currently follows the Lightning layout style. */
    public static boolean isLightning(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), SETTING_KEY, 0) == 1;
    }

    /** Persists the Lightning layout style selection. */
    public static void setLightning(Context context, boolean enabled) {
        Settings.Secure.putInt(context.getContentResolver(), SETTING_KEY, enabled ? 1 : 0);
    }

    /** Default accent overlay used for generic (non-Thunderbolt) screens in Lightning style. */
    @StyleRes
    public static int getDefaultAccentOverlay() {
        return R.style.ThemeOverlay_ThunderboltSub;
    }

    /**
     * Preference classes that ship their own layout and must keep it: switches, footers,
     * layout/space prefs, seekbars, color pickers and the settingslib widget rows.
     */
    public static boolean isCustomLayoutPreference(Preference pref) {
        return pref instanceof PrimarySwitchPreference
                || pref instanceof MainSwitchPreference
                || pref instanceof SettingsMainSwitchPreference
                || pref instanceof FooterPreference
                || pref instanceof LayoutPreference
                || pref instanceof SpacePreference
                || pref instanceof androidx.preference.SeekBarPreference
                || pref instanceof CustomSeekBarPreference
                || pref instanceof ColorPickerPreference
                || pref instanceof ButtonPreference
                || pref instanceof TopIntroPreference
                || pref instanceof BannerMessagePreference
                || pref instanceof SelectorWithWidgetPreference;
    }

    /**
     * Whether this preference's class binds its own view hierarchy in
     * {@link Preference#onBindViewHolder}. Such rows need their own layouts and must NEVER be
     * replaced with the Thunderbolt row layout (they resolve ids that our layout does not
     * provide), otherwise they crash when bound by the RecyclerView adapter.
     */
    public static boolean declaresCustomViewHolder(Preference pref) {
        final Class<?> clazz = pref.getClass();
        Boolean cached = CUSTOM_BINDER_CACHE.get(clazz);
        if (cached == null) {
            boolean custom = false;
            try {
                final Method binder =
                        clazz.getMethod("onBindViewHolder", PreferenceViewHolder.class);
                custom = binder.getDeclaringClass() != Preference.class;
            } catch (NoSuchMethodException e) {
                custom = false;
            }
            cached = custom;
            CUSTOM_BINDER_CACHE.put(clazz, cached);
        }
        return cached;
    }

    /**
     * Applies the Thunderbolt row/category layouts to a preference screen.
     *
     * @return the number of preferences whose layout actually changed.
     */
    public static int applySubScreenTheme(PreferenceScreen screen) {
        if (screen == null) {
            return 0;
        }
        int changed = 0;
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            changed += applyRowTheme(screen.getPreference(i)) ? 1 : 0;
        }
        return changed;
    }

    /** Preferences that must keep their own layout for any reason. */
    private static boolean customLayout(Preference pref) {
        return isCustomLayoutPreference(pref) || declaresCustomViewHolder(pref);
    }

    /**
     * Applies the Thunderbolt row/category layout to a single preference. Safe to call on
     * preferences added at runtime (before they are inserted into the group, so the adapter
     * captures the layout resource).
     *
     * @return true if the layout resource was changed.
     */
    public static boolean applyRowTheme(Preference pref) {
        if (pref == null) {
            return false;
        }
        boolean changed = false;
        if (pref instanceof PreferenceCategory && !declaresCustomViewHolder(pref)) {
            // Slim accent label header — do NOT hide, or its children vanish with it.
            changed |= setLayout(pref, R.layout.preference_thunderbolt_category);
        } else if (customLayout(pref)) {
            // Keep the built-in layout for prefs that manage their own widgets.
            return false;
        } else {
            changed |= setLayout(pref, R.layout.preference_thunderbolt_sub);
        }
        // Recurse into group children (categories, nested screens).
        if (pref instanceof PreferenceGroup) {
            PreferenceGroup group = (PreferenceGroup) pref;
            for (int i = 0; i < group.getPreferenceCount(); i++) {
                changed |= applyRowTheme(group.getPreference(i));
            }
        }
        return changed;
    }

    private static boolean setLayout(Preference pref, int layoutRes) {
        if (pref.getLayoutResource() != layoutRes) {
            pref.setLayoutResource(layoutRes);
            return true;
        }
        return false;
    }

    /**
     * Homepage tile icon for a given tile key, or {@code 0} when the tile has no custom
     * Thunderbolt icon (keep the default one).
     */
    public static int getHomeIconRes(String key) {
        if (key == null) {
            return 0;
        }
        switch (key) {
            case "top_level_network":
                return R.drawable.ic_tb_home_network;
            case "top_level_connected_devices":
                return R.drawable.ic_tb_home_connected;
            case "top_level_apps":
                return R.drawable.ic_tb_home_apps;
            case "top_level_notifications":
                return R.drawable.ic_tb_home_notifications;
            case "top_level_sound":
                return R.drawable.ic_tb_home_sound;
            case "top_level_priority_modes":
                return R.drawable.ic_tb_home_modes;
            case "top_level_display":
                return R.drawable.ic_tb_home_display;
            case "top_level_wallpaper":
                return R.drawable.ic_tb_home_wallpaper;
            case "top_level_storage":
                return R.drawable.ic_tb_home_storage;
            case "top_level_battery":
                return R.drawable.ic_tb_home_battery;
            case "top_level_system":
                return R.drawable.ic_tb_home_system;
            case "top_level_about_device":
                return R.drawable.ic_tb_home_about;
            case "top_level_thunderbolt":
                return R.drawable.ic_tb_home_thunderbolt;
            case "top_level_security":
            case "top_level_safety_center":
                // "Security & privacy" overview tile -> shield.
                return R.drawable.ic_tb_home_security;
            case "top_level_location":
                return R.drawable.ic_tb_home_location;
            case "top_level_privacy":
                return R.drawable.ic_tb_home_passwords;
            case "top_level_accounts":
            case "top_level_accounts_and_backup":
                return R.drawable.ic_tb_home_accounts;
            case "top_level_emergency":
                return R.drawable.ic_tb_home_emergency;
            case "top_level_accessibility":
                return R.drawable.ic_tb_home_accessibility;
            default:
                return 0;
        }
    }

    /** Homepage tile icon fallback resolved from the tile's target fragment class name. */
    public static int getHomeIconResByFragment(String fragment) {
        if (fragment == null) {
            return 0;
        }
        if (fragment.contains("NetworkDashboardFragment")) {
            return R.drawable.ic_tb_home_network;
        }
        if (fragment.contains("ConnectedDeviceDashboardFragment")) {
            return R.drawable.ic_tb_home_connected;
        }
        if (fragment.contains("AppDashboardFragment")) {
            return R.drawable.ic_tb_home_apps;
        }
        if (fragment.contains("ConfigureNotificationSettings")) {
            return R.drawable.ic_tb_home_notifications;
        }
        if (fragment.equals("com.android.settings.notification.SoundSettings")) {
            return R.drawable.ic_tb_home_sound;
        }
        if (fragment.contains("ZenModesListFragment")) {
            return R.drawable.ic_tb_home_modes;
        }
        if (fragment.equals("com.android.settings.DisplaySettings")) {
            return R.drawable.ic_tb_home_display;
        }
        if (fragment.contains("StorageDashboardFragment")) {
            return R.drawable.ic_tb_home_storage;
        }
        if (fragment.contains("PowerUsageSummary")) {
            return R.drawable.ic_tb_home_battery;
        }
        if (fragment.contains("SystemDashboardFragment")) {
            return R.drawable.ic_tb_home_system;
        }
        if (fragment.contains("MyDeviceInfoFragment")) {
            return R.drawable.ic_tb_home_about;
        }
        if (fragment.contains("ThunderboltDashboard")) {
            return R.drawable.ic_tb_home_thunderbolt;
        }
        if (fragment.contains("SecuritySettings")) {
            return R.drawable.ic_tb_home_security;
        }
        if (fragment.contains("SafetyCenter")) {
            return R.drawable.ic_tb_home_security;
        }
        if (fragment.contains("PrivacyDashboardFragment")) {
            return R.drawable.ic_tb_home_passwords;
        }
        if (fragment.contains("LocationSettings")) {
            return R.drawable.ic_tb_home_location;
        }
        if (fragment.contains("AccountDashboardFragment")
                || fragment.contains("AccountsAndBackupDashboardFragment")) {
            return R.drawable.ic_tb_home_accounts;
        }
        if (fragment.contains("EmergencyDashboardFragment")) {
            return R.drawable.ic_tb_home_emergency;
        }
        if (fragment.contains("AccessibilitySettings")) {
            return R.drawable.ic_tb_home_accessibility;
        }
        return 0;
    }
}