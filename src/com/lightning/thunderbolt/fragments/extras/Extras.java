/*
 * Copyright (C) 2019-2024 LightningOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lightning.thunderbolt.fragments.extras;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.content.Intent;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import com.android.internal.util.android.VibrationUtils;

import com.lightning.thunderbolt.preferences.SystemSettingSwitchPreference;

import java.util.List;

@SearchIndexable
public class Extras extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "Extras";
    private static final String CUSTOM_LOCKSCREEN_KEY = "custom_lockscreen_enable";

    private static final String CUSTOM_LOCKSCREEN_EDITOR_KEY = "custom_lockscreen_editor";

    private SystemSettingSwitchPreference mCustomLockscreen;
    private Preference mCustomLockscreenEditor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.lightning_settings_extras);

        final Context context = getContext();

        mCustomLockscreen = (SystemSettingSwitchPreference) findPreference(CUSTOM_LOCKSCREEN_KEY);
        if (mCustomLockscreen != null) {
            mCustomLockscreen.setOnPreferenceChangeListener(this);
        }

        mCustomLockscreenEditor = findPreference(CUSTOM_LOCKSCREEN_EDITOR_KEY);
        if (mCustomLockscreenEditor != null) {
            mCustomLockscreenEditor.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    launchCustomLockscreenApp(getContext());
                    return true;
                }
            });
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mCustomLockscreen) {
            sendCustomLockscreenBroadcast(getContext());
            return true;
        }
        return false;
    }

    private void sendCustomLockscreenBroadcast(Context context) {
        try {
            Intent intent = new Intent("org.lightning.systemui.lockscreen.SETTINGS_CHANGED");
            intent.setFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
            context.sendBroadcast(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void launchCustomLockscreenApp(Context context) {
        try {
            Intent intent = new Intent();
            intent.setClassName("org.avium.lockscreenedit", "org.avium.lockscreenedit.MainActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            android.widget.Toast.makeText(context, 
                "Custom Lockscreen Editor app not found", 
                android.widget.Toast.LENGTH_SHORT).show();
        }
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
                return keys;
            }
        };
}
