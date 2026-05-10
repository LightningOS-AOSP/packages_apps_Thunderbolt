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

package com.lightning.thunderbolt.fragments.lockscreen

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import androidx.preference.Preference
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable
import com.android.internal.util.lightning.VibrationUtils

import com.lightning.thunderbolt.preferences.SystemSettingSwitchPreference

@SearchIndexable
class MistHubSettings : SettingsPreferenceFragment(),
    Preference.OnPreferenceChangeListener {

    private var musicVisualizerPref: SystemSettingSwitchPreference? = null
    private var springAnimPref: SystemSettingSwitchPreference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.lightning_hub_settings)

        musicVisualizerPref = findPreference("lightning_hub_music_visualizer")
        springAnimPref = findPreference("lightning_hub_spring_animation")

        musicVisualizerPref?.onPreferenceChangeListener = this
        springAnimPref?.onPreferenceChangeListener = this
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key != null) {
            VibrationUtils.triggerVibration(context, 3)
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        return true
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.LIGHTNING
    }

    companion object {
        const val TAG = "MistHubSettings"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.lightning_hub_settings) {
            override fun getNonIndexableKeys(context: Context): List<String> {
                val keys = super.getNonIndexableKeys(context).toMutableList()
                val isHubEnabled = Settings.System.getInt(
                    context.contentResolver,
                    "lightning_hub_enabled",
                    0
                ) == 1
                if (!isHubEnabled) {
                    keys.addAll(listOf(
                        "lightning_hub_corner_radius",
                        "lightning_hub_edge_glow",
                        "lightning_hub_vertical_offset",
                        "lightning_hub_font",
                        "lightning_hub_animation_speed",
                        "lightning_hub_spring_animation",
                        "lightning_hub_pulse_notifications",
                        "lightning_hub_battery_status",
                        "lightning_hub_music_visualizer",
                        "lightning_hub_per_app"
                    ))
                }
                return keys
            }
        }
    }
}
