/*
 * Copyright (C) 2026 LightningOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lightning.thunderbolt;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SELinux;
import android.os.SystemProperties;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.settingslib.utils.StringUtil;

import java.net.Inet4Address;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Redesigned "About phone" screen for the Thunderbolt Control Center. The
 * page follows the regular system background for light/dark mode; the hero
 * card shows processor / storage / screen, and a two-column grid of
 * category-tinted, tappable info cards. The original device-info behaviors
 * are preserved: the Android version card opens the stock firmware/Android
 * version screen, and the Build number card keeps the classic 7-tap
 * developer-mode easter egg. No color is hardcoded: everything resolves
 * through the {@code ThemeOverlay_ThunderboltAboutPhone} overlay applied to
 * the fragment's theme context.
 */
/** Callbacks for cards that trigger device-info actions instead of a dialog. */
interface AboutCardCallback {
    void openFirmwareVersionPage();

    void onBuildNumberTap();
}

public class AboutPhoneFragment extends Fragment implements AboutCardCallback {

    private enum Category {
        HARDWARE, SOFTWARE, SECURITY
    }

    private static final class InfoItem {
        final int labelRes;
        final String value;
        final int iconRes;
        final Category category;

        InfoItem(int labelRes, String value, int iconRes, Category category) {
            this.labelRes = labelRes;
            this.value = value;
            this.iconRes = iconRes;
            this.category = category;
        }
    }

    private Context mThemedContext;
    private View mRoot;

    private final List<InfoItem> mItems = new ArrayList<>();
    private InfoCardAdapter mAdapter;

    private int mDevHitCountdown = -1;
    private Toast mDevHitToast;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mThemedContext = new ContextThemeWrapper(inflater.getContext(),
                R.style.ThemeOverlay_ThunderboltAboutPhone);
        mRoot = inflater.cloneInContext(mThemedContext)
                .inflate(R.layout.activity_about_phone, container, false);
        return mRoot;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null && getActivity().getActionBar() != null) {
            getActivity().getActionBar().hide();
        }

        initDeveloperOptions();
        bindToolbar();
        bindHero();
        bindGrid();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRoot = null;
    }

    /**
     * Opens the original "Android version" screen (LightningOS firmware version
     * page with Android version, build number, maintainer and status) so all
     * stock behaviors on that page keep working.
     */
    @Override
    public void openFirmwareVersionPage() {
        final Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.SubSettings");
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT,
                "com.android.settings.deviceinfo.firmwareversion.FirmwareVersionSettings");
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID,
                R.string.firmware_version);
        mThemedContext.startActivity(intent);
    }

    /**
     * Keeps the original 7-tap "Build number" developer-mode easter egg from
     * the stock About device page.
     */
    @Override
    public void onBuildNumberTap() {
        if (mDevHitCountdown > 0) {
            mDevHitCountdown--;
            if (mDevHitCountdown == 0) {
                DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(mThemedContext, true);
                toast(R.string.show_dev_on_cm, Toast.LENGTH_LONG);
            } else if (mDevHitCountdown < 5) {
                toast(StringUtil.getIcuPluralsString(mThemedContext, mDevHitCountdown,
                        R.string.show_dev_countdown_cm), Toast.LENGTH_SHORT);
            }
        } else {
            toast(R.string.show_dev_already_cm, Toast.LENGTH_LONG);
        }
    }

    private void initDeveloperOptions() {
        mDevHitCountdown = DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(mThemedContext)
                ? -1 : 7;
        mDevHitToast = null;
    }

    private void toast(int resId, int duration) {
        if (mDevHitToast != null) {
            mDevHitToast.cancel();
        }
        mDevHitToast = Toast.makeText(mThemedContext, resId, duration);
        mDevHitToast.show();
    }

    private void toast(CharSequence text, int duration) {
        if (mDevHitToast != null) {
            mDevHitToast.cancel();
        }
        mDevHitToast = Toast.makeText(mThemedContext, text, duration);
        mDevHitToast.show();
    }

    // ---------------------------------------------------------------------
    // Toolbar
    // ---------------------------------------------------------------------

    private void bindToolbar() {
        final View back = mRoot.findViewById(R.id.aboutBack);
        back.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        final EditText searchField = mRoot.findViewById(R.id.aboutSearchField);
        final ImageView searchIcon = mRoot.findViewById(R.id.aboutSearch);
        searchIcon.setOnClickListener(v -> toggleSearch(searchField));

        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mAdapter != null) {
                    mAdapter.setFilter(s == null ? null : s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        searchField.setOnEditorActionListener((v, actionId, event) -> {
            dismissSearch(searchField);
            return true;
        });
    }

    private void toggleSearch(EditText searchField) {
        if (searchField.getVisibility() == View.VISIBLE) {
            dismissSearch(searchField);
        } else {
            searchField.setVisibility(View.VISIBLE);
            searchField.requestFocus();
            final InputMethodManager imm = mThemedContext
                    .getSystemService(InputMethodManager.class);
            if (imm != null) {
                imm.showSoftInput(searchField, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    private void dismissSearch(EditText searchField) {
        searchField.setVisibility(View.GONE);
        searchField.clearFocus();
        if (mAdapter != null) {
            mAdapter.setFilter(null);
        }
        final InputMethodManager imm = mThemedContext
                .getSystemService(InputMethodManager.class);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchField.getWindowToken(), 0);
        }
    }

    // ---------------------------------------------------------------------
    // Hero card
    // ---------------------------------------------------------------------

    private void bindHero() {
        final GradientDrawable heroBg = new GradientDrawable();
        heroBg.setColor(color(R.attr.aboutSurfaceContainer));
        heroBg.setCornerRadius(dp(12));
        mRoot.findViewById(R.id.heroCard).setBackground(heroBg);

        final float glyphRadius = dp(14);
        final int glyphStroke = Math.max(1, Math.round(0.5f * getResources()
                .getDisplayMetrics().density));
        final int glyphColor = color(R.attr.aboutSurface);

        final GradientDrawable glyphBg = new GradientDrawable();
        glyphBg.setColor(android.graphics.Color.TRANSPARENT);
        glyphBg.setCornerRadius(glyphRadius);
        glyphBg.setStroke(glyphStroke, color(R.attr.aboutOutline));
        final View glyph = mRoot.findViewById(R.id.heroGlyph);
        glyph.setClipToOutline(true);
        glyph.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), glyphRadius);
            }
        });
        glyph.setBackground(glyphBg);

        final ImageView glyphIcon = mRoot.findViewById(R.id.heroGlyphIcon);
        glyphIcon.setImageDrawable(tintedDrawable(mThemedContext, R.drawable.ic_bolt,
                color(R.attr.colorCategoryHardwareIcon)));

        final ImageView wallpaperView = mRoot.findViewById(R.id.heroWallpaper);
        final View scrim = mRoot.findViewById(R.id.heroGlyphScrim);
        final Drawable wallpaper = loadWallpaperDrawable();
        if (wallpaper != null) {
            wallpaperView.setImageDrawable(wallpaper);
            wallpaperView.setVisibility(View.VISIBLE);
            final GradientDrawable scrimBg = new GradientDrawable();
            scrimBg.setShape(GradientDrawable.OVAL);
            scrimBg.setColor((glyphColor & 0x00FFFFFF) | 0x99000000);
            scrim.setBackground(scrimBg);
            scrim.setVisibility(View.VISIBLE);
        } else {
            glyphBg.setColor(glyphColor);
            wallpaperView.setVisibility(View.GONE);
            scrim.setVisibility(View.GONE);
        }

        ((TextView) mRoot.findViewById(R.id.heroProcessorValue))
                .setText(Build.SOC_MODEL);

        ((TextView) mRoot.findViewById(R.id.heroStorageValue))
                .setText(formatStorage(totalRamBytes()));

        final DisplayMetrics dm = getResources().getDisplayMetrics();
        ((TextView) mRoot.findViewById(R.id.heroScreenValue))
                .setText(dm.widthPixels + " \u00d7 " + dm.heightPixels);
    }

    /** Returns the current phone wallpaper, or the built-in one for live
     * wallpapers. Null if unavailable. */
    private Drawable loadWallpaperDrawable() {
        try {
            final WallpaperManager wm = WallpaperManager.getInstance(mThemedContext);
            final Drawable drawable = wm.getDrawable();
            if (drawable != null) {
                return drawable;
            }
            return wm.getBuiltInDrawable();
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------------
    // Info card grid
    // ---------------------------------------------------------------------

    private void bindGrid() {
        buildItems();

        final RecyclerView grid = mRoot.findViewById(R.id.aboutGrid);
        mAdapter = new InfoCardAdapter(mThemedContext, mItems, this);
        grid.setLayoutManager(new GridLayoutManager(mThemedContext, 2));
        grid.addItemDecoration(new GridSpacingDecoration(2, dp(10)));
        grid.setAdapter(mAdapter);
    }

    private void buildItems() {
        mItems.clear();

        // Hardware
        mItems.add(new InfoItem(R.string.about_field_device_model,
                Build.MODEL, R.drawable.ic_device_mobile, Category.HARDWARE));
        mItems.add(new InfoItem(R.string.about_field_processor,
                Build.SOC_MODEL, R.drawable.ic_cpu, Category.HARDWARE));
        mItems.add(new InfoItem(R.string.about_field_kernel,
                System.getProperty("os.version"), R.drawable.ic_cpu, Category.HARDWARE));
        mItems.add(new InfoItem(R.string.about_field_bootloader,
                SystemProperties.get("ro.boot.bootloader", "\u2014"),
                R.drawable.ic_cpu, Category.HARDWARE));
        mItems.add(new InfoItem(R.string.about_field_baseband,
                SystemProperties.get("gsm.version.baseband", "\u2014"),
                R.drawable.ic_antenna, Category.HARDWARE));
        mItems.add(new InfoItem(R.string.about_field_ram,
                formatStorage(totalRamBytes()), R.drawable.ic_device_mobile,
                Category.HARDWARE));
        mItems.add(new InfoItem(R.string.about_field_battery_level,
                batteryLevel(), R.drawable.ic_bolt, Category.HARDWARE));
        mItems.add(new InfoItem(R.string.about_field_ip_address,
                ipAddress(), R.drawable.ic_antenna, Category.HARDWARE));
        mItems.add(new InfoItem(R.string.about_field_wifi_mac,
                wifiMac(), R.drawable.ic_antenna, Category.HARDWARE));
        mItems.add(new InfoItem(R.string.about_field_bluetooth_address,
                bluetoothAddress(), R.drawable.ic_device_mobile, Category.HARDWARE));
        mItems.add(new InfoItem(R.string.about_field_serial_number,
                SystemProperties.get("ro.serialno", "\u2014"),
                R.drawable.ic_device_mobile, Category.HARDWARE));

        // Software
        mItems.add(new InfoItem(R.string.about_field_android_version,
                Build.VERSION.RELEASE, R.drawable.ic_brand_android, Category.SOFTWARE));
        mItems.add(new InfoItem(R.string.about_field_api_level,
                "API " + Build.VERSION.SDK_INT, R.drawable.ic_brand_android,
                Category.SOFTWARE));
        mItems.add(new InfoItem(R.string.about_field_thunderbolt_version,
                SystemProperties.get("ro.thunderbolt.version", "\u2014"),
                R.drawable.ic_bolt, Category.SOFTWARE));
        mItems.add(new InfoItem(R.string.about_field_build_number,
                Build.DISPLAY, R.drawable.ic_hammer, Category.SOFTWARE));
        mItems.add(new InfoItem(R.string.about_field_build_date,
                DateFormat.getDateInstance().format(new Date(Build.TIME)),
                R.drawable.ic_hammer, Category.SOFTWARE));
        mItems.add(new InfoItem(R.string.about_field_build_fingerprint,
                Build.FINGERPRINT, R.drawable.ic_brand_android, Category.SOFTWARE));
        mItems.add(new InfoItem(R.string.about_field_maintainer,
                SystemProperties.get("ro.lightningos.maintainer", "\u2014"),
                R.drawable.ic_hammer, Category.SOFTWARE));
        mItems.add(new InfoItem(R.string.about_field_build_status,
                SystemProperties.get("ro.lightning.buildtype", "\u2014"),
                R.drawable.ic_shield_check, Category.SOFTWARE));

        // Security
        mItems.add(new InfoItem(R.string.about_field_security_patch,
                Build.VERSION.SECURITY_PATCH, R.drawable.ic_shield_check,
                Category.SECURITY));
        mItems.add(new InfoItem(R.string.about_field_encryption_state,
                encryptionState(), R.drawable.ic_lock, Category.SECURITY));
        mItems.add(new InfoItem(R.string.about_field_verified_boot,
                verifiedBootState(), R.drawable.ic_shield_check, Category.SECURITY));
        mItems.add(new InfoItem(R.string.about_field_selinux_state,
                getString(SELinux.isSELinuxEnforced()
                        ? R.string.about_selinux_enforcing : R.string.about_selinux_permissive),
                R.drawable.ic_lock, Category.SECURITY));
    }

    // ---------------------------------------------------------------------
    // Live-ish device values
    // ---------------------------------------------------------------------

    private long totalRamBytes() {
        try {
            final ActivityManager am = requireActivity()
                    .getSystemService(ActivityManager.class);
            final ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(info);
            return info.totalMem;
        } catch (RuntimeException e) {
            return 0L;
        }
    }

    private String formatStorage(long bytes) {
        if (bytes <= 0L) {
            return "\u2014";
        }
        final float gib = bytes / 1024f / 1024f / 1024f;
        return String.format(Locale.US, "%.1f GB", gib);
    }

    private String batteryLevel() {
        try {
            final Intent battery = mThemedContext.registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery == null) {
                return "\u2014";
            }
            final int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            final int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            if (level < 0 || scale <= 0) {
                return "\u2014";
            }
            return (level * 100 / scale) + "%";
        } catch (RuntimeException e) {
            return "\u2014";
        }
    }

    private String ipAddress() {
        try {
            final ConnectivityManager cm = mThemedContext
                    .getSystemService(ConnectivityManager.class);
            final android.net.Network network = cm.getActiveNetwork();
            final LinkProperties lp = network != null ? cm.getLinkProperties(network) : null;
            if (lp != null) {
                for (android.net.LinkAddress address : lp.getLinkAddresses()) {
                    if (address.getAddress() instanceof Inet4Address) {
                        return address.getAddress().getHostAddress();
                    }
                }
            }
        } catch (RuntimeException ignored) {
        }
        return "\u2014";
    }

    private String wifiMac() {
        try {
            final WifiManager wm = mThemedContext.getSystemService(WifiManager.class);
            final WifiInfo info = wm.getConnectionInfo();
            if (info != null && info.getMacAddress() != null) {
                return info.getMacAddress();
            }
        } catch (RuntimeException ignored) {
        }
        return "\u2014";
    }

    private String bluetoothAddress() {
        try {
            final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                final String address = adapter.getAddress();
                if (address != null && !"02:00:00:00:00:00".equals(address)) {
                    return address;
                }
            }
        } catch (RuntimeException ignored) {
        }
        return "\u2014";
    }

    private String encryptionState() {
        final String state = SystemProperties.get("ro.crypto.state", "").toLowerCase(Locale.US);
        if ("encrypted".equals(state)) {
            return getString(R.string.about_encryption_encrypted);
        }
        if ("unencrypted".equals(state)) {
            return getString(R.string.about_encryption_unencrypted);
        }
        return state.isEmpty() ? "\u2014" : state;
    }

    private String verifiedBootState() {
        final String state = SystemProperties.get("ro.boot.verifiedbootstate", "")
                .toLowerCase(Locale.US);
        final int res;
        switch (state) {
            case "green":
                res = R.string.about_verified_boot_green;
                break;
            case "yellow":
                res = R.string.about_verified_boot_yellow;
                break;
            case "orange":
                res = R.string.about_verified_boot_orange;
                break;
            case "red":
                res = R.string.about_verified_boot_red;
                break;
            default:
                return state.isEmpty() ? "\u2014" : state;
        }
        return getString(res);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private int color(int attrRes) {
        final TypedValue value = new TypedValue();
        if (!mThemedContext.getTheme().resolveAttribute(attrRes, value, true)) {
            return 0xff000000;
        }
        return value.data;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static Drawable tintedDrawable(Context context, int iconRes, int color) {
        final Drawable drawable = context.getDrawable(iconRes);
        final Drawable mutated = drawable != null ? drawable.mutate() : null;
        if (mutated != null) {
            mutated.setTintList(android.content.res.ColorStateList.valueOf(color));
        }
        return mutated;
    }

    // ---------------------------------------------------------------------
    // Info card adapter
    // ---------------------------------------------------------------------

    private static final class InfoCardAdapter extends RecyclerView.Adapter<InfoCardHolder> {

        private final Context mContext;
        private final List<InfoItem> mMaster;
        private final List<InfoItem> mFiltered = new ArrayList<>();
        private final AboutCardCallback mCallback;
        private String mFilter;

        InfoCardAdapter(Context context, List<InfoItem> master,
                AboutCardCallback callback) {
            mContext = context;
            mMaster = master;
            mCallback = callback;
            mFiltered.addAll(master);
        }

        void setFilter(String filter) {
            final String query = filter == null ? "" : filter.trim().toLowerCase(Locale.US);
            mFilter = query.isEmpty() ? null : query;
            mFiltered.clear();
            if (mFilter == null) {
                mFiltered.addAll(mMaster);
            } else {
                for (InfoItem item : mMaster) {
                    final String label = mContext.getString(item.labelRes);
                    if (label.toLowerCase(Locale.US).contains(mFilter)
                            || item.value.toLowerCase(Locale.US).contains(mFilter)) {
                        mFiltered.add(item);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public InfoCardHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(mContext)
                    .inflate(R.layout.item_info_card_about, parent, false);
            return new InfoCardHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull InfoCardHolder holder, int position) {
            final InfoItem item = mFiltered.get(position);

            final float density = mContext.getResources().getDisplayMetrics().density;
            final float radius = 12f * density;

            final GradientDrawable content = new GradientDrawable();
            content.setColor(attrColor(item.category, 0));
            content.setCornerRadius(radius);

            final GradientDrawable mask = new GradientDrawable();
            mask.setColor(0xFFFFFFFF);
            mask.setCornerRadius(radius);

            final RippleDrawable ripple = new RippleDrawable(
                    ColorStateList.valueOf(attrColor(item.category, 4)), content, mask);
            holder.root.setBackground(ripple);

            final GradientDrawable stripe = new GradientDrawable();
            stripe.setColor(attrColor(item.category, 1));
            stripe.setCornerRadius(3f * density);
            holder.stripe.setBackground(stripe);

            holder.icon.setImageDrawable(tintedDrawable(mContext, item.iconRes,
                    attrColor(item.category, 2)));

            holder.label.setText(item.labelRes);
            holder.label.setTextColor(attrColor(item.category, 3));
            holder.value.setText(item.value);
            holder.value.setTextColor(attrColor(item.category, 4));

            holder.root.setOnClickListener(v -> {
                if (mCallback == null) {
                    return;
                }
                if (item.labelRes == R.string.about_field_android_version) {
                    mCallback.openFirmwareVersionPage();
                } else if (item.labelRes == R.string.about_field_build_number) {
                    mCallback.onBuildNumberTap();
                } else {
                    showDetail(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mFiltered.size();
        }

        private void showDetail(InfoItem item) {
            new AlertDialog.Builder(mContext)
                    .setIcon(tintedDrawable(mContext, item.iconRes,
                            attrColor(item.category, 2)))
                    .setTitle(mContext.getString(item.labelRes))
                    .setMessage(item.value)
                    .setPositiveButton(android.R.string.ok, null)
                    .create()
                    .show();
        }

        private int attrColor(Category category, int index) {
            final int[] attrs = category == Category.HARDWARE
                    ? new int[]{
                        R.attr.colorCategoryHardwareFill, R.attr.colorCategoryHardwareBar,
                        R.attr.colorCategoryHardwareIcon, R.attr.colorCategoryHardwareLabel,
                        R.attr.colorCategoryHardwareValue}
                    : category == Category.SOFTWARE
                    ? new int[]{
                        R.attr.colorCategorySoftwareFill, R.attr.colorCategorySoftwareBar,
                        R.attr.colorCategorySoftwareIcon, R.attr.colorCategorySoftwareLabel,
                        R.attr.colorCategorySoftwareValue}
                    : new int[]{
                        R.attr.colorCategorySecurityFill, R.attr.colorCategorySecurityBar,
                        R.attr.colorCategorySecurityIcon, R.attr.colorCategorySecurityLabel,
                        R.attr.colorCategorySecurityValue};

            final TypedValue tv = new TypedValue();
            if (!mContext.getTheme().resolveAttribute(attrs[index], tv, true)) {
                return 0xff000000;
            }
            return tv.data;
        }
    }

    private static final class InfoCardHolder extends RecyclerView.ViewHolder {
        final View root;
        final View stripe;
        final ImageView icon;
        final TextView label;
        final TextView value;

        InfoCardHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.cardRoot);
            stripe = itemView.findViewById(R.id.cardStripe);
            icon = itemView.findViewById(R.id.cardIcon);
            label = itemView.findViewById(R.id.cardLabel);
            value = itemView.findViewById(R.id.cardValue);
        }
    }

    /** Uniform gap between grid cells. */
    private static final class GridSpacingDecoration extends RecyclerView.ItemDecoration {
        private final int mSpanCount;
        private final int mSpacing;

        GridSpacingDecoration(int spanCount, int spacing) {
            mSpanCount = spanCount;
            mSpacing = spacing;
        }

        @Override
        public void getItemOffsets(@NonNull android.graphics.Rect outRect,
                @NonNull View view, @NonNull RecyclerView parent,
                @NonNull RecyclerView.State state) {
            final int position = parent.getChildAdapterPosition(view);
            if (position == RecyclerView.NO_POSITION) {
                return;
            }
            final int half = mSpacing / 2;
            final int column = position % mSpanCount;
            outRect.left = column == 0 ? 0 : half;
            outRect.right = column == mSpanCount - 1 ? 0 : half;
            outRect.top = position < mSpanCount ? 0 : half;
            outRect.bottom = half;
        }
    }
}