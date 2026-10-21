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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.widget.Toast;

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
public class Collectibles extends SettingsPreferenceFragment {

    private int tapCount = 0;
    private SharedPreferences prefs;
    private Preference genesisPref;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.category_collectibles);
        
        prefs = getActivity().getSharedPreferences("ThunderboltCollectibles", Context.MODE_PRIVATE);
        
        genesisPref = findPreference("badge_genesis");
        updatePreferencesState();

        Preference secretEggPref = findPreference("unlock_secret_egg");
        if (secretEggPref != null) {
            secretEggPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    tapCount++;
                    if (tapCount >= 5) {
                        prefs.edit().putBoolean("genesis_unlocked", true).apply();
                        Toast.makeText(getActivity(), " Easter Egg Found! Genesis Badge Unlocked!", Toast.LENGTH_LONG).show();
                        updatePreferencesState();
                        tapCount = 0;
                    } else {
                        Toast.makeText(getActivity(), "Tap " + (5 - tapCount) + " more times...", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            });
        }
    }

    private void updatePreferencesState() {
        if (genesisPref != null) {
            boolean genesisUnlocked = prefs.getBoolean("genesis_unlocked", false);
            if (genesisUnlocked) {
                genesisPref.setSummary("Status: UNLOCKED  (Active in OS)");
            } else {
                genesisPref.setSummary("Status: Locked");
            }
        }
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
                    sir.xmlResId = R.xml.category_collectibles;
                    return Arrays.asList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
                }
            };
}
