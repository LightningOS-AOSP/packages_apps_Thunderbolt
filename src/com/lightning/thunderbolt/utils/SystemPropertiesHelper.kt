package com.lightning.thunderbolt.utils

import android.os.SystemProperties
import android.provider.Settings
import android.provider.Settings.Secure
import android.content.ContentResolver
import android.os.UserHandle

object SystemPropertiesHelper {

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return SystemProperties.getBoolean(key, defaultValue)
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return SystemProperties.getInt(key, defaultValue)
    }

    fun get(key: String, def: String): String {
        return SystemProperties.get(key, def)
    }

    fun getSystemBoolean(resolver: ContentResolver, key: String, defaultValue: Boolean): Boolean {
        return Settings.System.getIntForUser(
            resolver, 
            key, 
            if (defaultValue) 1 else 0,
            UserHandle.USER_CURRENT
        ) == 1
    }

    fun setSystemBoolean(resolver: ContentResolver, key: String, value: Boolean) {
        Settings.System.putIntForUser(
            resolver, 
            key, 
            if (value) 1 else 0,
            UserHandle.USER_CURRENT
        )
    }

    fun getSystemInt(resolver: ContentResolver, key: String, defaultValue: Int): Int {
        return Settings.System.getIntForUser(
            resolver, 
            key, 
            defaultValue,
            UserHandle.USER_CURRENT
        )
    }

    fun setSystemInt(resolver: ContentResolver, key: String, value: Int) {
        Settings.System.putIntForUser(
            resolver, 
            key, 
            value,
            UserHandle.USER_CURRENT
        )
    }

    fun getSystemString(resolver: ContentResolver, key: String, defaultValue: String): String {
        return Settings.System.getStringForUser(
            resolver, 
            key,
            UserHandle.USER_CURRENT
        ) ?: defaultValue
    }

    fun setSystemString(resolver: ContentResolver, key: String, value: String) {
        Settings.System.putStringForUser(
            resolver, 
            key, 
            value,
            UserHandle.USER_CURRENT
        )
    }

    fun getSecureBoolean(resolver: ContentResolver, key: String, defaultValue: Boolean): Boolean {
        return Secure.getIntForUser(
            resolver, 
            key, 
            if (defaultValue) 1 else 0,
            UserHandle.USER_CURRENT
        ) == 1
    }

    fun setSecureBoolean(resolver: ContentResolver, key: String, value: Boolean) {
        Secure.putIntForUser(
            resolver, 
            key, 
            if (value) 1 else 0,
            UserHandle.USER_CURRENT
        )
    }

    fun getSecureInt(resolver: ContentResolver, key: String, defaultValue: Int): Int {
        return Secure.getIntForUser(
            resolver, 
            key, 
            defaultValue,
            UserHandle.USER_CURRENT
        )
    }

    fun setSecureInt(resolver: ContentResolver, key: String, value: Int) {
        Secure.putIntForUser(
            resolver, 
            key, 
            value,
            UserHandle.USER_CURRENT
        )
    }

    fun getSecureString(
        resolver: ContentResolver,
        key: String,
        defaultValue: String
    ): String {
        return Secure.getStringForUser(
            resolver, 
            key,
            UserHandle.USER_CURRENT
        ) ?: defaultValue
    }

    fun setSecureString(
        resolver: ContentResolver,
        key: String,
        value: String
    ) {
        Secure.putStringForUser(
            resolver, 
            key, 
            value,
            UserHandle.USER_CURRENT
        )
    }

}
