/*
 * Copyright (C) 2019-2024 LightningOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lightning.thunderbolt.fragments.extras;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import com.lightning.thunderbolt.preferences.SystemSettingSwitchPreference;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.util.android.VibrationUtils;

import java.util.List;
import com.android.settings.R;

import com.lightning.thunderbolt.utils.SystemPropertiesHelper;

@SearchIndexable
public class Extras extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "Extras";

    private static final String PREF_KEY_CUSTOM_LOCKSCREEN_TOGGLE =
            "pref_custom_lockscreen_enable";
    private static final String PREF_KEY_CUSTOM_LOCKSCREEN_OPEN =
            "pref_custom_lockscreen_open";

    private static final String CUSTOM_LOCKSCREEN_PROP =
            "persist.mist.customlockscreen.enable";

    private SwitchPreferenceCompat mCustomLockscreenToggle;
    private Preference mCustomLockscreenOpen;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.lightning_settings_extras);

        final Context context = getContext();
        final ContentResolver resolver = context.getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();
        final Resources resources = context.getResources();

        PreferenceCategory customCategory = (PreferenceCategory) findPreference("custom_lockscreen_category");
        if (customCategory == null) {
            customCategory = new PreferenceCategory(getContext());
            customCategory.setKey("custom_lockscreen_category_runtime");
            try {
                customCategory.setTitle(resources.getString(R.string.custom_lockscreen_title));
            } catch (Exception e) {
                customCategory.setTitle("Custom Lockscreen");
            }
            if (prefScreen != null) prefScreen.addPreference(customCategory);
        }

        SwitchPreferenceCompat customToggle = (SwitchPreferenceCompat) findPreference(PREF_KEY_CUSTOM_LOCKSCREEN_TOGGLE);
        if (customToggle == null) {
            customToggle = new SwitchPreferenceCompat(getContext());
            customToggle.setKey(PREF_KEY_CUSTOM_LOCKSCREEN_TOGGLE);
            try {
                customToggle.setTitle(resources.getString(R.string.custom_lockscreen_title));
                customToggle.setSummary(resources.getString(R.string.custom_lockscreen_summary));
            } catch (Exception e) {
                customToggle.setTitle("Custom Lockscreen");
                customToggle.setSummary("Enable or disable the custom lockscreen.");
            }
            customToggle.setDefaultValue(false);
            if (customCategory != null) customCategory.addPreference(customToggle);
        } else {
            if (customToggle.getParent() == null && customCategory != null) customCategory.addPreference(customToggle);
        }

        Preference customOpen = findPreference(PREF_KEY_CUSTOM_LOCKSCREEN_OPEN);
        if (customOpen == null) {
            customOpen = new Preference(getContext());
            customOpen.setKey(PREF_KEY_CUSTOM_LOCKSCREEN_OPEN);
            try {
                customOpen.setTitle(resources.getString(R.string.custom_lockscreen_open));
                customOpen.setSummary(resources.getString(R.string.custom_lockscreen_open_summary));
            } catch (Exception e) {
                customOpen.setTitle("Open Lockscreen Editor");
                customOpen.setSummary("Tap to configure the custom lockscreen UI.");
            }
            if (customCategory != null) customCategory.addPreference(customOpen);
        } else {
            if (customOpen.getParent() == null && customCategory != null) customCategory.addPreference(customOpen);
        }

        if (customToggle != null) {
            customToggle.setPersistent(false);
            boolean enabled = getSystemPropertyBoolean(CUSTOM_LOCKSCREEN_PROP, false);
            customToggle.setChecked(enabled);
            customToggle.setOnPreferenceChangeListener(this);
        } else {
            Log.w(TAG, "Custom lockscreen toggle not available");
        }

        if (customOpen != null) {
            customOpen.setOnPreferenceClickListener(pref -> {
                try {
                    Intent i = new Intent();
                    i.setClassName("org.avium.lockscreenedit",
                            "org.avium.lockscreenedit.MainActivity");
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                } catch (Exception e) {
                    try {
                        PackageManager pm = requireContext().getPackageManager();
                        Intent launch = pm.getLaunchIntentForPackage("org.avium.lockscreenedit");
                        if (launch != null) {
                            launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(launch);
                        } else {
                            Log.e(TAG, "Custom lockscreen app not installed: org.avium.lockscreenedit");
                        }
                    } catch (Exception ex) {
                        Log.w(TAG, "Failed to launch custom lockscreen editor", ex);
                    }
                }
                return true;
            });
        } else {
            Log.w(TAG, "Custom lockscreen open preference not available");
       }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final Context context = getContext();
        final ContentResolver resolver = context.getContentResolver();

        if (preference != null && PREF_KEY_CUSTOM_LOCKSCREEN_TOGGLE.equals(preference.getKey())) {
            if (!(newValue instanceof Boolean)) return false;
            final boolean enabled = (Boolean) newValue;

            new Thread(() -> {
                try {
                    setSystemProperty(CUSTOM_LOCKSCREEN_PROP, enabled ? "true" : "false");
                } catch (Exception e) {
                    Log.w(TAG, "Failed to set system property " + CUSTOM_LOCKSCREEN_PROP, e);
                }

                try {
                    Intent intent = new Intent("org.avium.systemui.lockscreen.SETTINGS_CHANGED");
                    intent.setFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
                    Context ctx = getContext();
                    if (ctx != null) ctx.sendBroadcast(intent);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to broadcast lockscreen settings change", e);
                }
            }).start();

            return true;
        }

        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.LIGHTNING;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference != null && preference.getKey() != null) {
            VibrationUtils.triggerVibration(getContext(), 3);
        }
        return super.onPreferenceTreeClick(preference);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider(R.xml.lightning_settings_extras) {

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                List<String> keys = super.getNonIndexableKeys(context);
                final Resources resources = context.getResources();
                return keys;
            }
        };

    private boolean getSystemPropertyBoolean(String key, boolean def) {
        try {
            return SystemPropertiesHelper.INSTANCE.getBoolean(key, def);
        } catch (Throwable t) {
            Log.w(TAG, "SystemPropertiesHelper.getBoolean failed for key=" + key, t);
            return def;
        }
    }

    private void setSystemProperty(String key, String value) {
        try {
            SystemPropertiesHelper.INSTANCE.set(key, value);
        } catch (Throwable t) {
            Log.w(TAG, "SystemPropertiesHelper.set failed for key=" + key + " value=" + value, t);
        }
    }
}
