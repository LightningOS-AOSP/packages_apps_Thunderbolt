/*
 * SPDX-FileCopyrightText: LightningOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lightning.thunderbolt.fragments.quicksettings;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;

import lineageos.providers.LineageSettings;

import com.lightning.thunderbolt.preferences.SecureSettingListPreference;
import com.lightning.thunderbolt.utils.DeviceUtils;
import com.lightning.thunderbolt.utils.SystemUtils;

@SearchIndexable
public class PanelCustomisations extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final String TAG = "PanelCustomisations";

    private static final String QS_BRIGHTNESS_CATEGORY = "ax_qs_sliders_category";

    private static final String KEY_AX_QS_PANEL_MODE = "ax_qs_panel_mode";
    private static final String KEY_AX_QS_QUICK_PANEL_ON_LEFT = "ax_qs_quick_panel_on_left";
    private static final String KEY_AX_QS_BRIGHTNESS_SLIDER_STYLE = "ax_qs_brightness_vertical_slider_style";
    private static final String KEY_AX_QS_VOLUME_SLIDER_STYLE = "ax_qs_volume_vertical_slider_style";
    private static final String KEY_QS_RESET_LAYOUT = "qs_reset_layout";

    private static final String KEY_BRIGHTNESS_SLIDER_HAPTIC = "qs_brightness_slider_haptic";
    private static final String KEY_BRIGHTNESS_SLIDER_POSITION = "qs_brightness_slider_position";
    private static final String KEY_SHOW_AUTO_BRIGHTNESS = "qs_show_auto_brightness";
    private static final String KEY_SHOW_BRIGHTNESS_SLIDER = "qs_show_brightness_slider";

    private SecureSettingListPreference mQsPanelMode;
    private SecureSettingListPreference mQsQuickPanelOnLeft;
    private SecureSettingListPreference mQsBrightnessSliderStyle;
    private SecureSettingListPreference mQsVolumeSliderStyle;
    private Preference mQsResetLayout;

    private ListPreference mBrightnessSliderPosition;
    private ListPreference mShowBrightnessSlider;
    private SwitchPreferenceCompat mBrightnessSliderHaptic;
    private SwitchPreferenceCompat mShowAutoBrightness;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.lightning_settings_panel_customisations);

        final Context context = getContext();
        final ContentResolver resolver = context.getContentResolver();

        final PreferenceCategory brightnessCategory = findPreference(QS_BRIGHTNESS_CATEGORY);

        mQsPanelMode = findPreference(KEY_AX_QS_PANEL_MODE);
        if (mQsPanelMode != null) {
            mQsPanelMode.setOnPreferenceChangeListener(this);
        }

        mQsQuickPanelOnLeft = findPreference(KEY_AX_QS_QUICK_PANEL_ON_LEFT);
        updateQuickPanelOnLeftState();

        mQsBrightnessSliderStyle = findPreference(KEY_AX_QS_BRIGHTNESS_SLIDER_STYLE);
        if (mQsBrightnessSliderStyle != null) {
            mQsBrightnessSliderStyle.setOnPreferenceChangeListener(this);
        }

        mQsVolumeSliderStyle = findPreference(KEY_AX_QS_VOLUME_SLIDER_STYLE);
        if (mQsVolumeSliderStyle != null) {
            mQsVolumeSliderStyle.setOnPreferenceChangeListener(this);
        }

        mQsResetLayout = findPreference(KEY_QS_RESET_LAYOUT);
        if (mQsResetLayout != null) {
            mQsResetLayout.setOnPreferenceClickListener(this);
        }

        mShowBrightnessSlider = findPreference(KEY_SHOW_BRIGHTNESS_SLIDER);
        if (mShowBrightnessSlider != null) {
            mShowBrightnessSlider.setOnPreferenceChangeListener(this);
            boolean showSlider = LineageSettings.Secure.getIntForUser(resolver,
                    LineageSettings.Secure.QS_SHOW_BRIGHTNESS_SLIDER, 1, UserHandle.USER_CURRENT) > 0;

            mBrightnessSliderPosition = findPreference(KEY_BRIGHTNESS_SLIDER_POSITION);
            if (mBrightnessSliderPosition != null) {
                mBrightnessSliderPosition.setEnabled(showSlider);
            }

            mBrightnessSliderHaptic = findPreference(KEY_BRIGHTNESS_SLIDER_HAPTIC);
            if (mBrightnessSliderHaptic != null) {
                if (DeviceUtils.hasVibrator(context)) {
                    mBrightnessSliderHaptic.setOnPreferenceChangeListener(this);
                    mBrightnessSliderHaptic.setEnabled(showSlider);
                } else if (brightnessCategory != null) {
                    brightnessCategory.removePreference(mBrightnessSliderHaptic);
                }
            }

            mShowAutoBrightness = findPreference(KEY_SHOW_AUTO_BRIGHTNESS);
            if (mShowAutoBrightness != null) {
                if (context.getResources().getBoolean(
                        com.android.internal.R.bool.config_automatic_brightness_available)) {
                    mShowAutoBrightness.setEnabled(showSlider);
                } else if (brightnessCategory != null) {
                    brightnessCategory.removePreference(mShowAutoBrightness);
                }
            }
        }
    }

    private void updateQuickPanelOnLeftState() {
        if (mQsQuickPanelOnLeft != null) {
            int panelMode = Settings.Secure.getIntForUser(
                    getContext().getContentResolver(),
                    "ax_qs_panel_mode", 0, UserHandle.USER_CURRENT);
            mQsQuickPanelOnLeft.setEnabled(panelMode == 1);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mShowBrightnessSlider) {
            int value = Integer.parseInt((String) newValue);
            if (mBrightnessSliderPosition != null)
                mBrightnessSliderPosition.setEnabled(value > 0);
            if (mBrightnessSliderHaptic != null)
                mBrightnessSliderHaptic.setEnabled(value > 0);
            if (mShowAutoBrightness != null)
                mShowAutoBrightness.setEnabled(value > 0);
            return true;
        } else if (preference == mQsPanelMode) {
            int mode = Integer.parseInt((String) newValue);
            if (mQsQuickPanelOnLeft != null) {
                mQsQuickPanelOnLeft.setEnabled(mode == 1);
            }
            return true;
        } else if (preference == mQsBrightnessSliderStyle) {
            int val = Integer.parseInt((String) newValue);
            Settings.Secure.putIntForUser(getContext().getContentResolver(),
                    "ax_qs_brightness_vertical_slider_style", val, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mQsVolumeSliderStyle) {
            int val = Integer.parseInt((String) newValue);
            Settings.Secure.putIntForUser(getContext().getContentResolver(),
                    "ax_qs_volume_vertical_slider_style", val, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mBrightnessSliderHaptic) {
            SystemUtils.showSystemUiRestartDialog(getActivity());
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mQsResetLayout) {
            final ContentResolver resolver = getContext().getContentResolver();
            final int userId = UserHandle.USER_CURRENT;
            Settings.Secure.putStringForUser(resolver, "ax_qqs_tile_order", "", userId);
            Settings.Secure.putStringForUser(resolver, "ax_qs_tile_order", "", userId);
            Settings.Secure.putStringForUser(resolver, "ax_qqs_control_order", "", userId);
            Settings.Secure.putStringForUser(resolver, "ax_qs_control_order", "", userId);
            Settings.Secure.putStringForUser(resolver, "ax_qqs_spans", "", userId);
            Settings.Secure.putStringForUser(resolver, "ax_qs_spans", "", userId);
            Settings.Secure.putStringForUser(resolver, "ax_qqs_control_positions", "", userId);
            Settings.Secure.putStringForUser(resolver, "ax_qs_control_positions", "", userId);
            Toast.makeText(getContext(), R.string.ax_qs_reset_layout_done, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.LIGHTNING;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider(
            R.xml.lightning_settings_panel_customisations) {

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            List<String> keys = super.getNonIndexableKeys(context);

            if (!context.getResources().getBoolean(
                    com.android.internal.R.bool.config_automatic_brightness_available)) {
                keys.add(KEY_SHOW_AUTO_BRIGHTNESS);
            }

            if (!DeviceUtils.hasVibrator(context)) {
                keys.add(KEY_BRIGHTNESS_SLIDER_HAPTIC);
            }

            return keys;
        }
    };
}
