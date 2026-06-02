/*
 * Copyright (C) 2026 LightningOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lightning.thunderbolt.fragments.themes;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.lightning.thunderbolt.preferences.SecureSettingSwitchPreference;
import com.lightning.thunderbolt.preferences.SystemSettingSeekBarPreference;

import java.util.List;

@SearchIndexable
public class WallpaperDepth extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    public static final String TAG = "WallpaperDepth";

    private static final String KEY_ENABLED  = "ax_depth_clock_enabled";
    private static final String KEY_OPACITY  = "lock_screen_depth_wallpaper_opacity";
    private static final String KEY_OFFSET_X = "lock_screen_depth_wallpaper_offset_x";
    private static final String KEY_OFFSET_Y = "lock_screen_depth_wallpaper_offset_y";

    private SecureSettingSwitchPreference mEnabled;
    private SystemSettingSeekBarPreference mOpacity;
    private SystemSettingSeekBarPreference mOffsetX;
    private SystemSettingSeekBarPreference mOffsetY;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.depth_wallpaper_settings);

        mEnabled  = findPreference(KEY_ENABLED);
        mOpacity  = findPreference(KEY_OPACITY);
        mOffsetX  = findPreference(KEY_OFFSET_X);
        mOffsetY  = findPreference(KEY_OFFSET_Y);

        if (mEnabled != null) mEnabled.setOnPreferenceChangeListener(this);

        updateDependencies();
        showInfoDialog();
    }

    private void showInfoDialog() {
        Context context = getContext();
        if (context == null) return;

        new AlertDialog.Builder(context)
                .setTitle(R.string.depthwall_info_dialog_title)
                .setMessage(R.string.depthwall_info_dialog_message)
                .setCancelable(false)
                .setPositiveButton(R.string.depthwall_info_dialog_button, null)
                .show();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mEnabled) {
            requireActivity().runOnUiThread(this::updateDependencies);
        }
        return true;
    }

    private void updateDependencies() {
        boolean enabled = Settings.Secure.getIntForUser(
                requireActivity().getContentResolver(),
                KEY_ENABLED, 0, UserHandle.USER_CURRENT) == 1;

        if (mOpacity  != null) mOpacity.setEnabled(enabled);
        if (mOffsetX  != null) mOffsetX.setEnabled(enabled);
        if (mOffsetY  != null) mOffsetY.setEnabled(enabled);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.LIGHTNING;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.depth_wallpaper_settings) {
                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    return super.getNonIndexableKeys(context);
                }
            };
}
