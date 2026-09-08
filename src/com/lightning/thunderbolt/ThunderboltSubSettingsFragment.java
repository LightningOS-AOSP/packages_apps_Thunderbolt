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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.annotation.XmlRes;
import androidx.preference.Preference;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.SettingsPreferenceFragment;

import com.android.internal.logging.nano.MetricsProto;

/**
 * Base fragment for all Thunderbolt sub-setting screens.
 *
 * Applies per-screen accent theming via ContextThemeWrapper on the RecyclerView,
 * sets the page background, kills dividers and spacing (flush rows like the dashboard),
 * and converts standard preferences to Thunderbolt-styled card rows while keeping
 * category headers as slim accent labels.
 *
 * The row theming must run right after the preference screen is inflated (i.e. inside
 * {@link #addPreferencesFromResource(int)}) because the RecyclerView adapter captures the
 * layout resource of each preference at {@code onViewCreated} time.
 */
public abstract class ThunderboltSubSettingsFragment extends SettingsPreferenceFragment {

    /**
     * Theme overlay resource applied to the RecyclerView context.
     * Provides per-screen accent colors (colorControlActivated, thunderboltAccent, etc.).
     */
    @StyleRes
    protected abstract int getAccentThemeOverlay();

    @NonNull
    @Override
    public RecyclerView onCreateRecyclerView(@NonNull LayoutInflater inflater,
            @NonNull ViewGroup parent, @Nullable Bundle savedInstanceState) {
        // Wrap the inflater's context so that the RecyclerView (and all rows inflated
        // from it) inherit the accent theme for switches, seekbars, and row tints.
        int overlayRes = getAccentThemeOverlay();
        android.content.Context accentContext = overlayRes != 0
                ? new android.view.ContextThemeWrapper(requireContext(), overlayRes)
                : null;
        LayoutInflater themedInflater = accentContext != null
                ? inflater.cloneInContext(accentContext) : inflater;
        RecyclerView rv = super.onCreateRecyclerView(themedInflater, parent, savedInstanceState);
        if (rv != null) {
            rv.setClipToPadding(false);
        }
        return rv;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Flush rows — no dividers, no extra spacing.
        setDivider(null);
        setDividerHeight(0);
    }

    @Override
    public void addPreferencesFromResource(@XmlRes int preferencesResId) {
        super.addPreferencesFromResource(preferencesResId);
        // Apply the Thunderbolt layout resources to every preference as soon as the
        // screen is inflated. This must happen before the RecyclerView adapter is
        // created in onViewCreated, otherwise the custom layouts are ignored.
        ThunderboltTheme.applySubScreenTheme(getPreferenceScreen());
    }

    /**
     * This screen already applies Thunderbolt theming itself, so the generic Lightning
     * style hook in {@code SettingsPreferenceFragment} must not also restyle it.
     */
    @Override
    protected boolean isThunderboltStyledScreen() {
        return true;
    }

    @Override
    protected boolean isPreferenceSpacingEnabled() {
        // Flush rows like the dashboard — no expressive spacing.
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.LIGHTNING;
    }

    /**
     * Applies the Thunderbolt row/category layout to a single preference. Safe to call on
     * preferences added at runtime (before they are inserted into the group, so the adapter
     * captures the layout resource).
     */
    protected void applyRowTheme(Preference pref) {
        ThunderboltTheme.applyRowTheme(pref);
    }
}