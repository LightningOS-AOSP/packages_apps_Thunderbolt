/*
 * Copyright (C) 2019-2026 LightningOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lightning.thunderbolt;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroupAdapter;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.LayoutPreference;

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

@SearchIndexable
public class ThunderboltSettings extends DashboardFragment implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TAG = "ThunderboltSettings";
    private static final String KEY_HERO = "tv_active_look";
    private static final String MONET_KEY = "monet_settings";
    private static final String OVERLAY_KEY = "theme_customization_overlay_packages";
    private static final String STYLE_KEY = "android.theme.customization.theme_style";

    private static final String[] CELL_KEYS = {"monet_settings", "themes", "lock_screen",
            "status_bar", "quick_settings", "power_menu", "miscellaneous", "spoofing"};

    private static final String[] CATEGORY_KEYS = {"tv_cat_style", "tv_cat_interface",
            "tv_cat_system"};

    private int[] mDotColors;

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.lightning_settings;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.LIGHTNING;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        for (String key : CELL_KEYS) {
            final Preference preference = findPreference(key);
            if (preference != null) {
                preference.setLayoutResource(R.layout.tv_cell);
            }
        }
        for (String key : CATEGORY_KEYS) {
            final Preference preference = findPreference(key);
            if (preference != null) {
                preference.setLayoutResource(R.layout.tv_category);
            }
        }
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        final RecyclerView list = getListView();
        if (list == null) {
            return root;
        }
        final float density = getResources().getDisplayMetrics().density;
        final int side = Math.round(12 * density);
        list.setClipToPadding(false);
        list.setItemAnimator(null);
        list.setPadding(side, Math.round(8 * density), side, Math.round(24 * density));
        list.setBackground(null);
        root.setBackgroundColor(ThunderboltSubPageStyler.windowBackground(getContext()));
        list.getViewTreeObserver().addOnPreDrawListener(() -> {
            dress(list);
            return true;
        });
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindActiveLook();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindActiveLook();
    }

    @Override
    public RecyclerView.LayoutManager onCreateLayoutManager() {
        final GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                final RecyclerView list = getListView();
                final RecyclerView.Adapter<?> adapter = list == null ? null : list.getAdapter();
                if (!(adapter instanceof PreferenceGroupAdapter)) {
                    return 3;
                }
                final Preference preference =
                        ((PreferenceGroupAdapter) adapter).getItem(position);
                if (preference == null || preference instanceof PreferenceCategory
                        || KEY_HERO.equals(preference.getKey())) {
                    return 3;
                }
                return 1;
            }
        });
        return layoutManager;
    }

    private void dress(RecyclerView list) {
        if (!(list.getAdapter() instanceof PreferenceGroupAdapter)) {
            return;
        }
        final PreferenceGroupAdapter adapter = (PreferenceGroupAdapter) list.getAdapter();
        for (int i = 0; i < list.getChildCount(); i++) {
            final View view = list.getChildAt(i);
            final int position = list.getChildAdapterPosition(view);
            if (position < 0 || position >= adapter.getItemCount()) {
                continue;
            }
            final Preference preference = adapter.getItem(position);
            if (isCell(preference.getKey())) {
                ThunderboltSubPageStyler.applyBackground(view, R.drawable.tv_cell_bg);
                final View summary = view.findViewById(android.R.id.summary);
                if (summary != null && summary.getVisibility() != View.GONE) {
                    summary.setVisibility(View.GONE);
                }
            } else {
                ThunderboltSubPageStyler.applyBackground(view, 0);
            }
            if (isCell(preference.getKey())) {
                final float density = getResources().getDisplayMetrics().density;
                ThunderboltSubPageStyler.applyPadding(view, Math.round(4 * density),
                        Math.round(10 * density), Math.round(4 * density),
                        Math.round(8 * density));
            } else {
                ThunderboltSubPageStyler.applyPadding(view, 0, 0, 0, 0);
            }
        }
    }

    private static boolean isCell(String key) {
        for (String cell : CELL_KEYS) {
            if (cell.equals(key)) {
                return true;
            }
        }
        return false;
    }

    private void bindActiveLook() {
        final Preference preference = findPreference(KEY_HERO);
        if (!(preference instanceof LayoutPreference)) {
            return;
        }
        final LayoutPreference hero = (LayoutPreference) preference;
        final View card = hero.findViewById(R.id.tv_active_look);
        if (card == null) {
            return;
        }
        final TextView title = hero.findViewById(R.id.tv_active_look_title);
        if (title != null) {
            title.setText(themeStyleName());
        }
        bindDots(hero);
        card.setOnClickListener(v -> openMonet());
        final View edit = hero.findViewById(R.id.tv_active_look_edit);
        if (edit != null) {
            edit.setOnClickListener(v -> openMonet());
        }
    }

    private void bindDots(LayoutPreference hero) {
        if (mDotColors == null) {
            mDotColors = new int[] {
                    getResources().getColor(android.R.color.system_accent1_600, null),
                    getResources().getColor(android.R.color.system_accent3_600, null),
                    getResources().getColor(android.R.color.system_accent2_600, null),
                    getResources().getColor(android.R.color.system_accent1_200, null)};
        }
        final int[] ids = {R.id.tv_dot_1, R.id.tv_dot_2, R.id.tv_dot_3, R.id.tv_dot_4};
        for (int i = 0; i < ids.length; i++) {
            final View dot = hero.findViewById(ids[i]);
            if (dot == null) {
                continue;
            }
            final GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(mDotColors[i]);
            shape.setStroke(Math.round(getResources().getDisplayMetrics().density * 2),
                    getResources().getColor(R.color.tv_secondary_container, null));
            dot.setBackground(shape);
        }
    }

    private String themeStyleName() {
        final String fallback = getString(R.string.tv_hero_fallback);
        final ContentResolver resolver = requireContext().getContentResolver();
        String style = null;
        try {
            final String raw = Settings.Secure.getString(resolver, OVERLAY_KEY);
            if (raw != null && !raw.isEmpty()) {
                style = new JSONObject(raw).optString(STYLE_KEY, null);
            }
        } catch (Exception e) {
            style = null;
        }
        if (style == null || style.isEmpty()) {
            return fallback;
        }
        final String[] values = getResources().getStringArray(R.array.monet_engine_style_values);
        final String[] entries = getResources().getStringArray(R.array.monet_engine_style_entries);
        final String wanted = style.toUpperCase(Locale.ROOT);
        for (int i = 0; i < values.length && i < entries.length; i++) {
            if (values[i].toUpperCase(Locale.ROOT).equals(wanted)) {
                return entries[i];
            }
        }
        return fallback;
    }

    private void openMonet() {
        final Preference monet = findPreference(MONET_KEY);
        if (monet == null || monet.getFragment() == null) {
            return;
        }
        new SubSettingLauncher(getActivity())
                .setDestination(monet.getFragment())
                .setArguments(monet.getExtras())
                .setSourceMetricsCategory(getMetricsCategory())
                .setTitleRes(-1)
                .launch();
    }

    @Override
    public Fragment getCallbackFragment() {
        return this;
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        new SubSettingLauncher(getActivity())
                .setDestination(pref.getFragment())
                .setArguments(pref.getExtras())
                .setSourceMetricsCategory(caller instanceof Instrumentable
                        ? ((Instrumentable) caller).getMetricsCategory()
                        : Instrumentable.METRICS_CATEGORY_UNKNOWN)
                .setTitleRes(-1)
                .launch();
        return true;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.lightning_settings) {
                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    keys.add(KEY_HERO);
                    return keys;
                }
            };
}
