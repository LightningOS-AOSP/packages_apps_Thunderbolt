/*
 * Copyright (C) 2019-2024 LightningOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lightning.thunderbolt.fragments.about;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import com.android.internal.util.lightning.VibrationUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

@SearchIndexable
public class About extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "About";
    private static final String KEY_CHANGELOG = "changelog";

    private static final String CHANGELOG_BASE_URL =
            "https://api.github.com/repos/LightningOS-Devices/official_devices/contents/changelogs/";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.lightning_settings_about);

        final Context context = getContext();
        final ContentResolver resolver = context.getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();
        final Resources resources = context.getResources();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final Context context = getContext();
        final ContentResolver resolver = context.getContentResolver();
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.LIGHTNING;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference != null && preference.getKey() != null) {
            VibrationUtils.triggerVibration(getContext(), 3);

            if (KEY_CHANGELOG.equals(preference.getKey())) {
                showChangelogDialog();
                return true;
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    private void showChangelogDialog() {
        final Context context = getContext();
        if (context == null) return;

        final String deviceCodename = SystemProperties.get("ro.lightning.device", "unknown");
        final String changelogUrl = CHANGELOG_BASE_URL + deviceCodename + ".txt";
        Log.d(TAG, "Fetching changelog from: " + changelogUrl);

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }

        final int dp4  = dp(context,  4);
        final int dp8  = dp(context,  8);
        final int dp16 = dp(context, 16);
        final int dp18 = dp(context, 18);
        final int dp20 = dp(context, 20);
        final int dp24 = dp(context, 24);
        final int dp44 = dp(context, 44);
        final int dp48 = dp(context, 48);

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp20, dp20, dp20, dp20);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dp24);
        TypedValue tvBg = new TypedValue();
        context.getTheme().resolveAttribute(
                android.R.attr.colorBackgroundFloating, tvBg, true);
        cardBg.setColor(tvBg.data);
        card.setBackground(cardBg);

        TextView title = new TextView(context);
        title.setText(context.getString(R.string.mistos_changelog_title));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setPadding(0, 0, 0, dp16);
        title.setGravity(android.view.Gravity.CENTER);
        TypedValue tvPrimary = new TypedValue();
        context.getTheme().resolveAttribute(
                android.R.attr.textColorPrimary, tvPrimary, true);
        title.setTextColor(tvPrimary.data);

        View divider = new View(context);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 1));
        dividerParams.bottomMargin = dp16;
        divider.setLayoutParams(dividerParams);
        TypedValue tvDiv = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.listDivider, tvDiv, true);
        divider.setBackgroundResource(
                tvDiv.resourceId != 0 ? tvDiv.resourceId : android.R.color.darker_gray);

        final TextView changelogText = new TextView(context);
        changelogText.setPadding(dp4, 0, dp4, dp8);
        changelogText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        changelogText.setLineSpacing(dp4, 1.2f);
        TypedValue tvTextColor = new TypedValue();
        boolean resolved = context.getTheme().resolveAttribute(
                android.R.attr.textColorPrimary, tvTextColor, true);
        if (resolved && tvTextColor.data != 0 && tvTextColor.data != Color.TRANSPARENT) {
            changelogText.setTextColor(tvTextColor.data);
        } else {
            TypedValue tvBg2 = new TypedValue();
            context.getTheme().resolveAttribute(
                    android.R.attr.colorBackgroundFloating, tvBg2, true);
            float[] hsv = new float[3];
            Color.colorToHSV(tvBg2.data, hsv);
            changelogText.setTextColor(hsv[2] > 0.5f ? Color.BLACK : Color.WHITE);
        }
        changelogText.setVisibility(View.GONE);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        scrollView.setVerticalScrollBarEnabled(true);
        scrollView.post(() -> {
            int maxHeight = (int) (context.getResources()
                    .getDisplayMetrics().heightPixels * 0.55f);
            ViewGroup.LayoutParams lp = scrollView.getLayoutParams();
            lp.height = maxHeight;
            scrollView.setLayoutParams(lp);
        });
        scrollView.addView(changelogText);

        final ProgressBar progressBar = new ProgressBar(context);
        LinearLayout.LayoutParams pbParams =
                new LinearLayout.LayoutParams(dp48, dp48);
        pbParams.gravity = Gravity.CENTER_HORIZONTAL;
        pbParams.topMargin = dp16;
        pbParams.bottomMargin = dp16;
        progressBar.setLayoutParams(pbParams);

        final LinearLayout contentArea = new LinearLayout(context);
        contentArea.setOrientation(LinearLayout.VERTICAL);
        contentArea.addView(progressBar);
        contentArea.addView(scrollView);

        ImageButton closeBtn = new ImageButton(context);
        LinearLayout.LayoutParams closeBtnParams =
                new LinearLayout.LayoutParams(dp44, dp44);
        closeBtnParams.gravity = Gravity.CENTER_HORIZONTAL;
        closeBtnParams.topMargin = dp18;
        closeBtn.setLayoutParams(closeBtnParams);

        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        TypedValue tvError = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorError, tvError, true);
        circleBg.setColor(tvError.data != 0 ? tvError.data : Color.parseColor("#F44336"));
        closeBtn.setBackground(circleBg);
        closeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        closeBtn.setColorFilter(Color.WHITE);
        closeBtn.setPadding(dp8, dp8, dp8, dp8);
        closeBtn.setScaleType(ImageView.ScaleType.FIT_CENTER);
        closeBtn.setOnClickListener(v -> {
            v.animate()
                .rotation(180f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(180)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .withEndAction(() -> {
                    card.animate()
                        .alpha(0f)
                        .translationY(dp(context, 40))
                        .scaleX(0.93f)
                        .scaleY(0.93f)
                        .setDuration(220)
                        .setInterpolator(new DecelerateInterpolator(2f))
                        .withEndAction(dialog::dismiss)
                        .start();
                })
                .start();
        });

        card.addView(title);
        card.addView(divider, dividerParams);
        card.addView(contentArea);
        card.addView(closeBtn);

        LinearLayout wrapper = new LinearLayout(context);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(dp20, 0, dp20, 0);
        wrapper.addView(card);

        dialog.setContentView(wrapper);

        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
            lp.width  = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.windowAnimations = 0;
            dialog.getWindow().setAttributes(lp);
            dialog.getWindow().setDimAmount(0.6f);
        }

        dialog.show();
        card.setAlpha(0f);
        card.setTranslationY(dp(context, 40));
        card.setScaleX(0.95f);
        card.setScaleY(0.95f);
        card.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(350)
            .setInterpolator(new DecelerateInterpolator(2.5f))
            .start();
        final Handler mainHandler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            String result = null;
            String errorMsg = null;

            try {
                URL url = new URL(changelogUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setInstanceFollowRedirects(true);
                conn.setUseCaches(false);
                conn.setRequestProperty("User-Agent", "LightningOS-Settings");
                conn.setRequestProperty("Accept", "application/vnd.github.v3.raw");
                conn.setRequestProperty("Cache-Control", "no-cache");

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Changelog HTTP response: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    reader.close();
                    result = sb.toString().trim();
                    Log.d(TAG, "Changelog fetched, length: " + result.length());
                } else {
                    errorMsg = "HTTP " + responseCode;
                    Log.e(TAG, "Changelog fetch failed: " + errorMsg);
                }
                conn.disconnect();

            } catch (Exception e) {
                errorMsg = e.getMessage();
                Log.e(TAG, "Changelog fetch exception: " + e.getMessage(), e);
            }

            final String finalResult = result;
            final String finalError  = errorMsg;

            mainHandler.post(() -> {
                if (!dialog.isShowing()) return;

                progressBar.setVisibility(View.GONE);

                if (finalResult != null && !finalResult.isEmpty()) {
                    changelogText.setText(finalResult);
                    changelogText.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Changelog text set on UI, chars: " + finalResult.length());
                } else {
                    String msg = context.getString(R.string.mistos_changelog_error)
                            + "\n\nDevice : " + deviceCodename
                            + "\nURL    : " + changelogUrl
                            + (finalError != null ? "\nError  : " + finalError : "");
                    changelogText.setText(msg);
                    changelogText.setVisibility(View.VISIBLE);
                    Toast.makeText(context,
                            context.getString(R.string.mistos_changelog_error),
                            Toast.LENGTH_LONG).show();
                }
                wrapper.requestLayout();
                wrapper.invalidate();
            });
        }).start();
    }

    private static int dp(Context context, int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics()));
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider(R.xml.lightning_settings_about) {
            @Override
            public List<String> getNonIndexableKeys(Context context) {
                List<String> keys = super.getNonIndexableKeys(context);
                final Resources resources = context.getResources();
                return keys;
            }
        };
}
