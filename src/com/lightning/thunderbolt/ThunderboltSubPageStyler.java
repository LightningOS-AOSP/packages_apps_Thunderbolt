/*
 * Copyright (C) 2026 LightningOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lightning.thunderbolt;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.LayoutPreference;

public final class ThunderboltSubPageStyler {

    private static final int[] DOT_IDS = {R.id.tv_dot_1, R.id.tv_dot_2, R.id.tv_dot_3,
            R.id.tv_dot_4};

    private static final java.util.HashMap<Integer, android.graphics.drawable.Drawable.ConstantState>
            STATES = new java.util.HashMap<>();

    private ThunderboltSubPageStyler() {
    }

    public static void applyBackground(View view, int resId) {
        final android.graphics.drawable.Drawable current = view.getBackground();
        if (resId == 0) {
            if (current != null) {
                view.setBackground(null);
            }
            return;
        }
        android.graphics.drawable.Drawable.ConstantState state = STATES.get(resId);
        if (state == null) {
            final android.graphics.drawable.Drawable drawable =
                    view.getContext().getDrawable(resId);
            if (drawable == null) {
                return;
            }
            state = drawable.getConstantState();
            STATES.put(resId, state);
        }
        if (current != null && current.getConstantState() != null
                && current.getConstantState().equals(state)) {
            return;
        }
        view.setBackgroundResource(resId);
    }

    public static int windowBackground(Context context) {
        final android.util.TypedValue value = new android.util.TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.colorBackground, value, true)
                && value.type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT
                && value.type <= android.util.TypedValue.TYPE_LAST_COLOR_INT) {
            return value.data;
        }
        return context.getColor(R.color.tv_surface);
    }

    public static void applyPadding(View view, int start, int top, int end, int bottom) {
        if (view.getPaddingLeft() == start && view.getPaddingTop() == top
                && view.getPaddingRight() == end && view.getPaddingBottom() == bottom) {
            return;
        }
        view.setPaddingRelative(start, top, end, bottom);
    }

    public static void applyOverlay(Context context) {
        context.getTheme().applyStyle(R.style.ThemeOverlay_Thunderbolt_SubPage, true);
    }

    public static void applyOverlay(Context context, String fragmentClass) {
        applyOverlay(context);
    }

    public static void attach(PreferenceFragmentCompat fragment) {
        final RecyclerView list = fragment.getListView();
        if (list == null) {
            return;
        }
        final Context context = list.getContext();
        final int side = context.getResources().getDimensionPixelSize(R.dimen.tv_list_pad_side);
        final int rowGap = context.getResources().getDimensionPixelSize(R.dimen.tv_row_gap);
        final int groupGap = context.getResources().getDimensionPixelSize(R.dimen.tv_group_gap);
        final int padTop = context.getResources().getDimensionPixelSize(R.dimen.tv_row_pad_v);
        final int padStart = context.getResources().getDimensionPixelSize(
                R.dimen.tv_row_pad_start);
        final int padEnd = context.getResources().getDimensionPixelSize(R.dimen.tv_row_pad_end);

        list.setPadding(side, list.getPaddingTop(), side, list.getPaddingBottom());
        list.setClipToPadding(false);
        list.setItemAnimator(null);
        list.setBackground(null);
        final View root = fragment.getView();
        if (root != null) {
            root.setBackgroundColor(windowBackground(root.getContext()));
        }

        addHeader(fragment);

        list.addItemDecoration(new RecyclerView.ItemDecoration() {
            private PreferenceGroupAdapter cachedAdapter;
            private int[] topOffsets;

            @Override
            public void getItemOffsets(Rect out, View view, RecyclerView parent,
                    RecyclerView.State state) {
                final PreferenceGroupAdapter adapter = adapterOf(parent);
                if (adapter == null) {
                    return;
                }
                final int position = parent.getChildAdapterPosition(view);
                if (position < 0) {
                    return;
                }
                if (adapter != cachedAdapter || topOffsets == null
                        || topOffsets.length != adapter.getItemCount()) {
                    cachedAdapter = adapter;
                    topOffsets = offsetsFor(adapter, groupGap, rowGap);
                }
                if (position < topOffsets.length) {
                    out.top = topOffsets[position];
                }
            }
        });

        list.addOnChildAttachStateChangeListener(
                new RecyclerView.OnChildAttachStateChangeListener() {
                    @Override
                    public void onChildViewAttachedToWindow(View view) {
                        style(list, view, padTop, padStart, padEnd);
                    }

                    @Override
                    public void onChildViewDetachedFromWindow(View view) {
                    }
                });

        list.post(() -> list.scrollToPosition(0));
        list.getViewTreeObserver().addOnPreDrawListener(() -> {
            for (int i = 0; i < list.getChildCount(); i++) {
                style(list, list.getChildAt(i), padTop, padStart, padEnd);
            }
            return true;
        });
    }

    private static void addHeader(PreferenceFragmentCompat fragment) {
        final PreferenceScreen screen = fragment.getPreferenceScreen();
        if (screen == null || fragment.getListView() == null) {
            return;
        }
        if (screen.findPreference(HeaderInfo.KEY) != null) {
            return;
        }
        final Context context = fragment.getListView().getContext();
        final HeaderInfo info = HeaderInfo.forFragment(context, fragment.getClass().getName());
        final LayoutPreference header = new LayoutPreference(context, R.layout.tv_sub_header);
        header.setKey(HeaderInfo.KEY);
        header.setSelectable(false);
        header.setPersistent(false);
        header.getExtras().putCharSequence(HeaderInfo.EXTRA_TITLE, info.title);
        header.getExtras().putCharSequence(HeaderInfo.EXTRA_SUMMARY, info.summary);
        header.getExtras().putInt(HeaderInfo.EXTRA_ICON, info.icon);
        header.setOrder(-1000);
        screen.addPreference(header);
        applyCategoryLayouts(screen);
    }

    private static void applyCategoryLayouts(PreferenceGroup group) {
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            final Preference preference = group.getPreference(i);
            if (preference instanceof PreferenceCategory) {
                preference.setLayoutResource(R.layout.tv_category);
                applyCategoryLayouts((PreferenceCategory) preference);
            } else if (preference instanceof PreferenceScreen) {
                applyCategoryLayouts((PreferenceScreen) preference);
            }
        }
    }

    private static void bindHeader(Preference preference, View view) {
        if (Boolean.TRUE.equals(view.getTag(R.id.tv_styled))) {
            return;
        }
        final android.os.Bundle extras = preference.getExtras();
        final TextView title = view.findViewById(R.id.tv_sub_header_title);
        if (title != null) {
            title.setText(extras.getCharSequence(HeaderInfo.EXTRA_TITLE));
        }
        final TextView summary = view.findViewById(R.id.tv_sub_header_summary);
        if (summary != null) {
            summary.setText(extras.getCharSequence(HeaderInfo.EXTRA_SUMMARY));
        }
        final View icon = view.findViewById(R.id.tv_sub_header_icon);
        if (icon instanceof android.widget.ImageView) {
            ((android.widget.ImageView) icon).setImageResource(
                    extras.getInt(HeaderInfo.EXTRA_ICON));
        }
        view.setTag(R.id.tv_styled, Boolean.TRUE);
    }

    private static void style(RecyclerView list, View view, int padTop, int padStart,
            int padEnd) {
        final Preference preference = preferenceAt(list, view);
        if (preference == null) {
            return;
        }
        final int position = list.getChildAdapterPosition(view);
        final PreferenceGroupAdapter adapter = adapterOf(list);
        final Preference previous = adapter == null || position <= 0
                ? null : adapter.getItem(position - 1);
        final Preference next = adapter == null || position < 0
                || position + 1 >= adapter.getItemCount() ? null : adapter.getItem(position + 1);
        if (preference instanceof PreferenceCategory) {
            final CharSequence title = preference.getTitle();
            final boolean empty = title == null || title.length() == 0;
            final int wanted = empty ? View.GONE : View.VISIBLE;
            if (view.getVisibility() != wanted) {
                view.setVisibility(wanted);
            }
            return;
        }
        if (HeaderInfo.KEY.equals(preference.getKey())) {
            bindHeader(preference, view);
            return;
        }
        if (preference instanceof FooterPreference) {
            return;
        }
        styleValuePill(view);
        styleSwatch(preference, view);
    }

    private static void styleValuePill(View view) {
        final int valueId = view.getResources().getIdentifier("seekbar_value", "id",
                view.getContext().getPackageName());
        if (valueId == 0) {
            return;
        }
        final View value = view.findViewById(valueId);
        if (!(value instanceof TextView)) {
            return;
        }
        final TextView pill = (TextView) value;
        if (!Boolean.TRUE.equals(pill.getTag(R.id.tv_styled))) {
            final float density = view.getResources().getDisplayMetrics().density;
            pill.setBackgroundResource(R.drawable.tv_value_pill);
            pill.setPaddingRelative(Math.round(12 * density), Math.round(4 * density),
                    Math.round(12 * density), Math.round(4 * density));
            pill.setTextColor(view.getResources().getColor(R.color.tv_on_secondary_container,
                    null));
            pill.setTextSize(13);
            pill.setTag(R.id.tv_styled, Boolean.TRUE);
        }
    }

    private static void styleSwatch(Preference preference, View view) {
        final LinearLayout frame = view.findViewById(android.R.id.widget_frame);
        if (frame == null) {
            return;
        }
        final Integer color = extensionDrawable(preference);
        if (color == null) {
            for (int i = frame.getChildCount() - 1; i >= 0; i--) {
                if (Boolean.TRUE.equals(frame.getChildAt(i).getTag(R.id.tv_styled))) {
                    frame.removeViewAt(i);
                }
            }
            return;
        }
        if (frame.getChildCount() > 0) {
            final View first = frame.getChildAt(frame.getChildCount() - 1);
            if (Boolean.TRUE.equals(first.getTag(R.id.tv_styled))) {
                final GradientDrawable existing = new GradientDrawable();
                existing.setShape(GradientDrawable.OVAL);
                existing.setColor(color);
                existing.setStroke(Math.round(2 * view.getResources().getDisplayMetrics().density),
                        view.getResources().getColor(R.color.tv_on_surface, null));
                first.setBackground(existing);
                return;
            }
            return;
        }
        final float density = view.getResources().getDisplayMetrics().density;
        final View swatch = new View(view.getContext());
        final GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(color);
        shape.setStroke(Math.round(2 * density),
                view.getResources().getColor(R.color.tv_on_surface, null));
        swatch.setBackground(shape);
        swatch.setTag(R.id.tv_styled, Boolean.TRUE);
        frame.addView(swatch, new LinearLayout.LayoutParams(Math.round(32 * density),
                Math.round(32 * density)));
    }

    private static Integer extensionDrawable(Preference preference) {
        final CharSequence summary = preference.getSummary();
        if (summary == null) {
            return null;
        }
        final String text = summary.toString().trim();
        if (text.length() != 7 && text.length() != 9) {
            return null;
        }
        if (text.charAt(0) != '#') {
            return null;
        }
        try {
            return (int) Long.parseLong(text.substring(1), 16) | 0xFF000000;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static PreferenceGroupAdapter adapterOf(RecyclerView list) {
        final RecyclerView.Adapter<?> adapter = list.getAdapter();
        return adapter instanceof PreferenceGroupAdapter ? (PreferenceGroupAdapter) adapter : null;
    }

    private static int[] offsetsFor(PreferenceGroupAdapter adapter, int groupGap, int rowGap) {
        final int count = adapter.getItemCount();
        final int[] tops = new int[count];
        for (int i = 0; i < count; i++) {
            final Preference preference = adapter.getItem(i);
            if (preference == null) {
                continue;
            }
            final Preference previous = i == 0 ? null : adapter.getItem(i - 1);
            if (preference instanceof PreferenceCategory) {
                tops[i] = previous == null ? 0 : groupGap;
            } else if (isFirst(preference, previous)) {
                tops[i] = previous instanceof PreferenceCategory ? 0 : groupGap;
            } else {
                tops[i] = rowGap;
            }
        }
        return tops;
    }

    private static boolean isFirst(Preference preference, Preference previous) {
        return previous == null || previous instanceof PreferenceCategory
                || previous.getParent() != preference.getParent();
    }

    private static boolean isLast(Preference preference, Preference next) {
        return next == null || next instanceof PreferenceCategory
                || next.getParent() != preference.getParent();
    }

    private static Preference preferenceAt(RecyclerView list, View view) {
        return preferenceAt(list, view, 0);
    }

    private static Preference preferenceAt(RecyclerView list, View view, int delta) {
        if (!(list.getAdapter() instanceof PreferenceGroupAdapter)) {
            return null;
        }
        final int position = list.getChildAdapterPosition(view) + delta;
        final PreferenceGroupAdapter adapter = (PreferenceGroupAdapter) list.getAdapter();
        return position < 0 || position >= adapter.getItemCount() ? null : adapter.getItem(position);
    }

    private static final class HeaderInfo {
        static final String KEY = "tv_sub_header";
        static final String EXTRA_TITLE = "tv_header_title";
        static final String EXTRA_SUMMARY = "tv_header_summary";
        static final String EXTRA_ICON = "tv_header_icon";

        final CharSequence title;
        final CharSequence summary;
        final int icon;

        private HeaderInfo(CharSequence title, CharSequence summary, int icon) {
            this.title = title;
            this.summary = summary;
            this.icon = icon;
        }

        static HeaderInfo forFragment(Context context, String className) {
            if (className == null) {
                return fallback(context);
            }
            if (className.contains(".themes.ColorsSettingsFragment")) {
                return of(context, R.string.themes_monet_title, R.string.themes_monet_summary,
                        R.drawable.ic_tv_hub_monet);
            }
            if (className.contains(".themes.")) {
                return of(context, R.string.themes_title, R.string.themes_summary,
                        R.drawable.ic_tv_hub_themes);
            }
            if (className.contains(".lockscreen.")) {
                return of(context, R.string.lock_screen_title, R.string.lock_screen_summary,
                        R.drawable.ic_tv_hub_lock);
            }
            if (className.contains(".statusbar.")) {
                return of(context, R.string.status_bar_title, R.string.status_bar_summary,
                        R.drawable.ic_tv_hub_status);
            }
            if (className.contains(".quicksettings.")) {
                return of(context, R.string.quick_settings_title, R.string.quick_settings_summary,
                        R.drawable.ic_tv_hub_quick);
            }
            if (className.contains(".powermenu.")) {
                return of(context, R.string.power_menu_title, R.string.power_menu_summary,
                        R.drawable.ic_tv_hub_power);
            }
            if (className.contains(".notifications.")) {
                return of(context, R.string.lightning_notifications_title,
                        R.string.notifications_summary, R.drawable.ic_tv_hub_notify);
            }
            if (className.contains(".miscellaneous.")) {
                return of(context, R.string.miscellaneous_title, R.string.miscellaneous_summary,
                        R.drawable.ic_tv_hub_misc);
            }
            if (className.contains(".spoofing.") || className.contains("Spoof")) {
                return of(context, R.string.spoofing_title, R.string.spoofing_summary,
                        R.drawable.ic_tv_hub_spoof);
            }
            return fallback(context);
        }

        private static HeaderInfo fallback(Context context) {
            return of(context, R.string.lightning_dashboard_title, R.string.tv_hub_subtitle,
                    R.drawable.ic_tv_hub_themes);
        }

        private static HeaderInfo of(Context context, int titleRes, int summaryRes, int icon) {
            return new HeaderInfo(context.getText(titleRes), context.getText(summaryRes), icon);
        }
    }
}
