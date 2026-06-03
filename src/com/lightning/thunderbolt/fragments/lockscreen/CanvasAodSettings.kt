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

package com.lightning.thunderbolt.fragments.lockscreen

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import android.widget.Toast

import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

import com.lightning.thunderbolt.preferences.SecureSettingListPreference
import com.lightning.thunderbolt.preferences.SecureSettingSwitchPreference
import com.lightning.thunderbolt.preferences.colorpicker.SecureSettingColorPickerPreference

import android.graphics.BitmapFactory
import android.widget.ImageView
import com.android.settingslib.widget.LayoutPreference
import java.io.File

@SearchIndexable
class CanvasAodSettings : SettingsPreferenceFragment(),
    Preference.OnPreferenceChangeListener {

    companion object {
        const val TAG = "CanvasAodSettings"

        private const val KEY_ENABLED            = "canvas_aod_enabled"
        private const val KEY_STYLE              = "canvas_aod_style"
        private const val KEY_ANIMATION_ENABLED  = "canvas_aod_animation_enabled"
        private const val KEY_ANIMATION_SPEED    = "canvas_aod_animation_speed"
        private const val KEY_WEATHER_EFFECTS    = "canvas_aod_weather_effects"
        private const val KEY_WEATHER_INTENSITY  = "canvas_aod_weather_intensity"
        private const val KEY_THICKNESS          = "canvas_aod_outline_thickness"
        private const val KEY_COLOR_MODE         = "canvas_aod_color_mode"
        private const val KEY_CUSTOM_COLOR       = "canvas_aod_custom_color"
        private const val KEY_CHARGING_ANIM      = "canvas_aod_charging_animation"
        private const val KEY_NOTIF_PULSE        = "canvas_aod_notification_pulse"
        private const val KEY_REGENERATE         = "canvas_aod_regenerate"
        private const val KEY_PREVIEW_CARD       = "canvas_aod_preview_card"
        private const val COLOR_MODE_CUSTOM = "2"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.canvas_aod_settings) {}
    }

    private var mEnabled:           SecureSettingSwitchPreference?      = null
    private var mStyle:             SecureSettingListPreference?         = null
    private var mAnimEnabled:       SecureSettingSwitchPreference?      = null
    private var mAnimSpeed:         SecureSettingListPreference?         = null
    private var mWeatherEffects:    SecureSettingSwitchPreference?      = null
    private var mWeatherIntensity:  SecureSettingListPreference?         = null
    private var mThickness:         SecureSettingListPreference?         = null
    private var mColorMode:         SecureSettingListPreference?         = null
    private var mCustomColor:       SecureSettingColorPickerPreference?  = null
    private var mChargingAnim:      SecureSettingSwitchPreference?      = null
    private var mNotifPulse:        SecureSettingSwitchPreference?      = null
    private var mRegenerate:        Preference?                          = null
    private var mPreviewCard:       LayoutPreference?                    = null
    private var mPreviewImageView:  ImageView?                           = null

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.canvas_aod_settings)

        mEnabled          = findPreference(KEY_ENABLED)
        mStyle            = findPreference(KEY_STYLE)
        mAnimEnabled      = findPreference(KEY_ANIMATION_ENABLED)
        mAnimSpeed        = findPreference(KEY_ANIMATION_SPEED)
        mWeatherEffects   = findPreference(KEY_WEATHER_EFFECTS)
        mWeatherIntensity = findPreference(KEY_WEATHER_INTENSITY)
        mThickness        = findPreference(KEY_THICKNESS)
        mColorMode        = findPreference(KEY_COLOR_MODE)
        mCustomColor      = findPreference(KEY_CUSTOM_COLOR)
        mChargingAnim     = findPreference(KEY_CHARGING_ANIM)
        mNotifPulse       = findPreference(KEY_NOTIF_PULSE)
        mRegenerate       = findPreference(KEY_REGENERATE)
        mPreviewCard      = findPreference(KEY_PREVIEW_CARD)

        mPreviewCard?.let {
            mPreviewImageView = it.findViewById(R.id.canvas_preview_image)
        }

        listOf(mEnabled, mStyle, mAnimEnabled, mAnimSpeed, mWeatherEffects,
            mWeatherIntensity, mThickness, mColorMode, mChargingAnim, mNotifPulse)
            .forEach { it?.onPreferenceChangeListener = this }

        mCustomColor?.onPreferenceChangeListener = this

        mRegenerate?.setOnPreferenceClickListener {
            triggerRegenerate()
            true
        }

        updateDependencies()
        loadLivePreview()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        handler.post {
            updateDependencies()
            triggerRegenerate()
            handler.postDelayed({ loadLivePreview() }, 3000L)
        }
        return true
    }

    private fun updateDependencies() {
        val enabled = isEnabled()

        listOf(mStyle, mAnimEnabled, mAnimSpeed, mWeatherEffects, mWeatherIntensity,
            mThickness, mColorMode, mChargingAnim, mNotifPulse, mRegenerate)
            .forEach { it?.isEnabled = enabled }

        val animEnabled = enabled && (mAnimEnabled?.isChecked == true)
        mAnimSpeed?.isEnabled = animEnabled

        val weatherEnabled = enabled && (mWeatherEffects?.isChecked == true)
        mWeatherIntensity?.isEnabled = weatherEnabled

        val colorModeVal = mColorMode?.value ?: Settings.Secure.getStringForUser(
            requireActivity().contentResolver, KEY_COLOR_MODE, UserHandle.USER_CURRENT
        ) ?: "1"
        val isCustomColor = enabled && (colorModeVal == COLOR_MODE_CUSTOM)
        mCustomColor?.isVisible = isCustomColor
        mCustomColor?.isEnabled = isCustomColor
    }

    private fun isEnabled(): Boolean {
        val cr = requireActivity().contentResolver
        return Settings.Secure.getIntForUser(cr, KEY_ENABLED, 0, UserHandle.USER_CURRENT) == 1
    }

    private fun loadLivePreview() {
        val cr = requireActivity().contentResolver
        val pathOrUri = Settings.Secure.getStringForUser(cr, "canvas_aod_cache_path", UserHandle.USER_CURRENT) ?: return
        try {
            val bitmap = if (pathOrUri.startsWith("content://")) {
                val uri = android.net.Uri.parse(pathOrUri)
                cr.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } else {
                val file = File(pathOrUri)
                if (file.exists() && file.length() > 0L) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } else null
            }
            if (bitmap != null) {
                mPreviewImageView?.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
        }
    }

    private fun triggerRegenerate() {
        try {
            val intent = Intent("com.android.axion.wallpapereffects.CANVAS_AOD_REGENERATE").apply {
                component = ComponentName(
                    "com.android.axion.wallpapereffects",
                    "com.android.axion.wallpapereffects.service.CanvasAodService"
                )
            }
            requireContext().startServiceAsUser(intent, UserHandle.CURRENT)
            Toast.makeText(
                context,
                R.string.canvas_aod_regenerating,
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                R.string.canvas_aod_regenerate_error,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun getMetricsCategory(): Int = MetricsProto.MetricsEvent.LIGHTNING
}
