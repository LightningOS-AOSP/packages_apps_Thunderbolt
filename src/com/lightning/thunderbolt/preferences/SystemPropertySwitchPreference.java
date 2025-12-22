/*
 * Copyright (C) 2022 crDroid Android Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lightning.thunderbolt.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.util.Log;

import com.android.settingslib.development.SystemPropPoker;

import lineageos.preference.SelfRemovingSwitchPreference;

import com.android.settings.R;

public class SystemPropertySwitchPreference extends SelfRemovingSwitchPreference {

    private static final String TAG = "SysPropSwitchPref";

    private String mPropKey = null;

    private String mPropType = "boolean";

    private String mPropDefault = "false";


    public SystemPropertySwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttrs(attrs);
    }

    public SystemPropertySwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs);
    }

    public SystemPropertySwitchPreference(Context context) {
        super(context);
        }

    private void initAttrs(AttributeSet attrs) {
        if (attrs == null) return;

        TypedArray a = null;
        try {
            a = getContext().obtainStyledAttributes(attrs, R.styleable.SystemPropertySwitchPreference);

            String pk = a.getString(R.styleable.SystemPropertySwitchPreference_propKey);
            if (pk != null && !pk.isEmpty()) mPropKey = pk;

            int propTypeEnum = a.getInt(R.styleable.SystemPropertySwitchPreference_propType, -1);
            if (propTypeEnum >= 0) {
                switch (propTypeEnum) {
                    case 0: // type_boolean
                        mPropType = "boolean";
                        break;
                    case 1: // type_string
                        mPropType = "string";
                        break;
                    case 2: // type_int
                        mPropType = "int";
                        break;
                    default:
                        mPropType = "boolean";
                        break;
                }
            } else {
                String t = a.getString(R.styleable.SystemPropertySwitchPreference_propType);
                if (t != null && !t.isEmpty()) mPropType = t;
            }

            String def = a.getString(R.styleable.SystemPropertySwitchPreference_propDefault);
            if (def != null) mPropDefault = def;
        } catch (Throwable t) {
            Log.w(TAG, "Failed to obtain attrs for SystemPropertySwitchPreference", t);
        } finally {
            if (a != null) a.recycle();
        }
    }

    private String effectivePropKey() {
        if (mPropKey != null && !mPropKey.isEmpty()) return mPropKey;
        return getKey();
    }

    @Override
    protected boolean isPersisted() {
        String prop = effectivePropKey();
        String val = SystemProperties.get(prop, "");
        return !val.isEmpty();
    }

    @Override
    protected void putBoolean(String key, boolean value) {
        String prop = effectivePropKey();
        try {
            SystemProperties.set(prop, Boolean.toString(value));
            SystemPropPoker.getInstance().poke();
        } catch (Throwable t) {
            Log.w(TAG, "Failed to set system property " + prop + " to " + value, t);
        }
    }

    @Override
    protected boolean getBoolean(String key, boolean defaultValue) {
        String prop = effectivePropKey();
        try {
            return SystemProperties.getBoolean(prop, defaultValue);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to read system property " + prop + " (getBoolean)", t);
            return defaultValue;
        }
    }
}
