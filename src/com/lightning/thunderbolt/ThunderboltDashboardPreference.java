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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

/**
 * Dashboard row preference rendered in the storm/voltage themed control center.
 * Supports tinting the accent bar, icon chip, and icon per accent group.
 */
public class ThunderboltDashboardPreference extends Preference {

    public interface Accent {
        int AMBER = 0;
        int VIOLET = 1;
        int CYAN = 2;
        int GRADIENT = 3;
    }

    private int mAccent = Accent.AMBER;
    private int mIconRes = R.drawable.ic_category_boltbar;
    private boolean mSpecial = false;

    public ThunderboltDashboardPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    public ThunderboltDashboardPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ThunderboltDashboardPreference(Context context) {
        super(context);
        init(null);
    }

    private void init(@Nullable AttributeSet attrs) {
        setLayoutResource(R.layout.preference_thunderbolt_row);
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(
                    attrs, R.styleable.ThunderboltDashboardPreference);
            mAccent = a.getInt(R.styleable.ThunderboltDashboardPreference_tbAccent, Accent.AMBER);
            mIconRes = a.getResourceId(
                    R.styleable.ThunderboltDashboardPreference_tbIcon, R.drawable.ic_category_boltbar);
            mSpecial = a.getBoolean(R.styleable.ThunderboltDashboardPreference_tbSpecial, false);
            a.recycle();
        }
    }

    public void setAccent(int accent) {
        mAccent = accent;
        notifyChanged();
    }

    public void setIconRes(@DrawableRes int iconRes) {
        mIconRes = iconRes;
        notifyChanged();
    }

    public void setSpecial(boolean special) {
        mSpecial = special;
        notifyChanged();
    }

    private int accentColor() {
        switch (mAccent) {
            case Accent.VIOLET: return R.color.thunderbolt_violet;
            case Accent.CYAN: return R.color.thunderbolt_cyan;
            default: return R.color.thunderbolt_amber;
        }
    }

    private int accentBarRes() {
        switch (mAccent) {
            case Accent.VIOLET: return R.drawable.bg_thunderbolt_accent_violet;
            case Accent.CYAN: return R.drawable.bg_thunderbolt_accent_cyan;
            case Accent.GRADIENT: return R.drawable.bg_thunderbolt_accent_gradient;
            default: return R.drawable.bg_thunderbolt_accent_amber;
        }
    }

    private int chipRes() {
        switch (mAccent) {
            case Accent.VIOLET: return R.drawable.bg_thunderbolt_chip_violet;
            case Accent.CYAN: return R.drawable.bg_thunderbolt_chip_cyan;
            default: return R.drawable.bg_thunderbolt_chip_amber;
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        View row = holder.itemView;

        View accentBar = holder.findViewById(R.id.tb_accent_bar);
        if (accentBar != null) accentBar.setBackgroundResource(accentBarRes());

        View chip = holder.findViewById(R.id.tb_icon_chip);
        if (chip != null) chip.setBackgroundResource(chipRes());

        ImageView icon = (ImageView) holder.findViewById(R.id.tb_icon);
        if (icon != null) {
            icon.setImageResource(mIconRes);
            icon.setColorFilter(getContext().getColor(accentColor()));
        }

        if (mSpecial) {
            row.setBackgroundResource(R.drawable.bg_thunderbolt_row_special);
        }
    }
}
