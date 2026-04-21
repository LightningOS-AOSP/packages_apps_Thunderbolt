/*
 * Copyright (C) 2023 the RisingOS Android Project
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

package com.lightning.thunderbolt

import android.app.settings.SettingsEnums
import android.content.Context
import android.view.View
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.SubSettingLauncher
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.widget.LayoutPreference

class LightningController(context: Context) : AbstractPreferenceController(context) {

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        screen.findPreference<LayoutPreference>(KEY_MIST)?.let { mistPref ->
            setupClickListeners(mistPref)
        }
    }

    private fun setupClickListeners(preference: LayoutPreference) {

        val clickMap = mapOf(
            R.id.card_monet      to "com.lightning.thunderbolt.fragments.themes.ColorsSettingsFragment",
            R.id.card_qs         to "com.lightning.thunderbolt.fragments.quicksettings.QuickSettings",
            R.id.card_statusbar  to "com.lightning.thunderbolt.fragments.statusbar.StatusBar",
            R.id.card_lockscreen to "com.lightning.thunderbolt.fragments.lockscreen.LockScreen",
            R.id.card_themes     to "com.lightning.thunderbolt.fragments.themes.Themes",
            R.id.card_spoofing   to "com.lightning.thunderbolt.fragments.miscellaneous.Spoofing",
            R.id.card_misc       to "com.lightning.thunderbolt.fragments.miscellaneous.Miscellaneous",
            R.id.card_sound      to "com.lightning.thunderbolt.fragments.sound.Sound",
            R.id.card_buttons    to "com.lightning.thunderbolt.fragments.buttons.Buttons",
            R.id.card_extras     to "com.lightning.thunderbolt.fragments.extras.Extras",
            R.id.card_about      to "com.lightning.thunderbolt.fragments.about.About",
            R.id.card_notifications to "com.lightning.thunderbolt.fragments.notifications.Notifications"
        )

        clickMap.forEach { (viewId, fragmentClass) ->
            val view = preference.findViewById<View>(viewId)
            view?.setOnClickListener {
                try {
                    SubSettingLauncher(mContext)
                        .setDestination(fragmentClass)
                        .setSourceMetricsCategory(SettingsEnums.DASHBOARD_SUMMARY)
                        .setTitleRes(-1)
                        .launch()
                } catch (e: Exception) {
                    android.util.Log.w(
                        "LightningController",
                        "Failed to launch fragment: $fragmentClass",
                        e
                    )
                }
            }
        }
    }

    override fun isAvailable(): Boolean = true

    override fun getPreferenceKey(): String = KEY_MIST

    companion object {
        private const val KEY_MIST = "lightning_card_dashboard"
    }
}
