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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.SwitchPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.android.internal.logging.nano.MetricsProto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SearchIndexable
public class Boltbar extends ThunderboltSubSettingsFragment {

    @Override
    protected int getAccentThemeOverlay() {
        return R.style.ThemeOverlay_ThunderboltSub;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.category_boltbar);

        initSwitchLayout();
    }

    private void initSwitchLayout() {
        ListPreference switchLayout = findPreference("boltbar_switch_layout");
        if (switchLayout == null) {
            return;
        }
        boolean lightning = ThunderboltTheme.isLightning(getContext());
        switchLayout.setValue(lightning ? "1" : "0");
        switchLayout.setSummary(getContext().getString(
                lightning ? R.string.boltbar_layout_lightning : R.string.boltbar_layout_aosp));
        switchLayout.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enable = "1".equals(String.valueOf(newValue));
            ThunderboltTheme.setLightning(getContext(), enable);
            preference.setSummary(getContext().getString(
                    enable ? R.string.boltbar_layout_lightning : R.string.boltbar_layout_aosp));
            // Relaunch Settings from its home page so the style applies instantly, without the
            // user having to force-close and reopen Settings.
            restartSettings();
            return true;
        });
    }

    private void restartSettings() {
        final Context context = getContext();
        if (context == null) {
            return;
        }
        final Intent intent =
                new Intent(context, com.android.settings.homepage.SettingsHomepageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.LIGHTNING;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.category_boltbar;
                    return Arrays.asList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
                }
            };
}
