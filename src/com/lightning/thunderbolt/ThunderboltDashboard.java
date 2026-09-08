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
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import com.android.internal.logging.nano.MetricsProto;

import java.util.Arrays;
import java.util.List;

@SearchIndexable
public class ThunderboltDashboard extends SettingsPreferenceFragment {

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.thunderbolt_dashboard);

        PreferenceScreen screen = getPreferenceScreen();

        ThunderboltDashboardPreference boltbar =
                (ThunderboltDashboardPreference) screen.findPreference("boltbar");
        boltbar.setAccent(ThunderboltDashboardPreference.Accent.AMBER);
        boltbar.setIconRes(R.drawable.ic_category_boltbar);

        ThunderboltDashboardPreference flashPanel =
                (ThunderboltDashboardPreference) screen.findPreference("flash_panel");
        flashPanel.setAccent(ThunderboltDashboardPreference.Accent.AMBER);
        flashPanel.setIconRes(R.drawable.ic_category_flashpanel);

        ThunderboltDashboardPreference motionNav =
                (ThunderboltDashboardPreference) screen.findPreference("motionnav_settings");
        motionNav.setAccent(ThunderboltDashboardPreference.Accent.VIOLET);
        motionNav.setIconRes(R.drawable.ic_category_motionnav);

        ThunderboltDashboardPreference staticField =
                (ThunderboltDashboardPreference) screen.findPreference("staticfield_settings");
        staticField.setAccent(ThunderboltDashboardPreference.Accent.CYAN);
        staticField.setIconRes(R.drawable.ic_category_staticfield);

        ThunderboltDashboardPreference stormUi =
                (ThunderboltDashboardPreference) screen.findPreference("stormui_settings");
        stormUi.setAccent(ThunderboltDashboardPreference.Accent.VIOLET);
        stormUi.setIconRes(R.drawable.ic_category_stormui);

        ThunderboltDashboardPreference lightningPerks =
                (ThunderboltDashboardPreference) screen.findPreference("lightningperks_settings");
        lightningPerks.setAccent(ThunderboltDashboardPreference.Accent.AMBER);
        lightningPerks.setIconRes(R.drawable.ic_category_lightningperks);

        ThunderboltDashboardPreference romCollectibles =
                (ThunderboltDashboardPreference) screen.findPreference("romcollectibles_settings");
        romCollectibles.setAccent(ThunderboltDashboardPreference.Accent.GRADIENT);
        romCollectibles.setIconRes(R.drawable.ic_category_romcollectibles);
        romCollectibles.setSpecial(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // No dividers: rows sit flush so sections are told apart by accent color only.
        setDivider(null);
        setDividerHeight(0);
    }

    @Override
    protected boolean isPreferenceSpacingEnabled() {
        // The expressive settingslib theme adds a bottom margin to every row. Our dashboard
        // keeps rows flush, so disable it here.
        return false;
    }

    /**
     * The dashboard rows carry their own Thunderbolt styling; the generic Lightning style
     * hook must not restyle them (and must not override {@link ThunderboltDashboardPreference}).
     */
    @Override
    protected boolean isThunderboltStyledScreen() {
        return true;
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
                    sir.xmlResId = R.xml.thunderbolt_dashboard;
                    return Arrays.asList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
                }
            };
}
