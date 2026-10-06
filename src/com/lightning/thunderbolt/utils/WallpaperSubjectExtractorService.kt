/*
 * Copyright (C) 2024-2026 Lunaris OS
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

package com.lightning.thunderbolt.utils

import android.app.Service
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.Intent
import android.content.res.Configuration
import android.content.Context
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.lightning.thunderbolt.utils.PortraitSegmenter

import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import kotlin.math.roundToInt

private const val TAG = "WallpaperSubjectExtractorService"

private const val SETTING_AUTO_SUBJECT = "depth_wallpaper_auto_subject"
private const val SETTING_SUBJECT_URI = "depth_wallpaper_subject_image_uri"
private const val SETTING_DEPTH_ENABLED = "depth_wallpaper_enabled"

private const val DE_PHOTO_FILE = "wallpaper.jpg"
private const val FILE_PREFIX = "DEPTH_WALLPAPER_SUBJECT"

class WallpaperSubjectExtractorService : Service() {

    companion object {
        @JvmField
        val ACTION_EXTRACT_NOW = "org.lightning.intent.action.EXTRACT_DEPTH_SUBJECT_NOW"
    }

    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var currentGeneration = 0
    private var pendingExtraction: Runnable? = null
    private var lastScreenW = 0
    private var lastScreenH = 0

    private val DEBOUNCE_DELAY_MS = 500L

    private val wallpaperChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (isAutoSubjectEnabled() && isDepthEnabled()) scheduleExtraction()
        }
    }

    private val colorsListener =
        WallpaperManager.OnColorsChangedListener { _, which ->
            if (which and (WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK) != 0) {
                if (isAutoSubjectEnabled()) scheduleExtraction()
            }
        }

    private val depthEnabledObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (isAutoSubjectEnabled() && isDepthEnabled()) scheduleExtraction()
        }
    }

    private val autoSubjectObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (isAutoSubjectEnabled() && isDepthEnabled()) scheduleExtraction()
        }
    }

    override fun onCreate() {
        super.onCreate()
        lastScreenW = resources.displayMetrics.widthPixels
        lastScreenH = resources.displayMetrics.heightPixels
        WallpaperManager.getInstance(this).addOnColorsChangedListener(colorsListener, handler)
        registerReceiver(
            wallpaperChangedReceiver,
            IntentFilter(Intent.ACTION_WALLPAPER_CHANGED),
            Context.RECEIVER_NOT_EXPORTED)
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(SETTING_DEPTH_ENABLED), false, depthEnabledObserver)
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(SETTING_AUTO_SUBJECT), false, autoSubjectObserver)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        lastScreenW = resources.displayMetrics.widthPixels
        lastScreenH = resources.displayMetrics.heightPixels
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_EXTRACT_NOW) {
            scheduleExtraction(force = true)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        WallpaperManager.getInstance(this).removeOnColorsChangedListener(colorsListener)
        unregisterReceiver(wallpaperChangedReceiver)
        contentResolver.unregisterContentObserver(depthEnabledObserver)
        contentResolver.unregisterContentObserver(autoSubjectObserver)
        pendingExtraction?.let { handler.removeCallbacks(it) }
        currentGeneration++
        super.onDestroy()
    }

    private fun isDepthEnabled() =
        Settings.System.getInt(contentResolver, SETTING_DEPTH_ENABLED, 0) != 0

    private fun isAutoSubjectEnabled() =
        Settings.System.getInt(contentResolver, SETTING_AUTO_SUBJECT, 0) != 0

    private fun scheduleExtraction(force: Boolean = false) {
        pendingExtraction?.let { handler.removeCallbacks(it) }
        val gen = ++currentGeneration
        val runnable = Runnable {
            Thread {
                try {
                    runExtraction(force, gen)
                } catch (e: Exception) {
                    Log.e(TAG, "Extraction threw unexpected exception", e)
                }
            }.start()
        }
        pendingExtraction = runnable
        handler.postDelayed(runnable, if (force) 0L else DEBOUNCE_DELAY_MS)
    }

    private fun runExtraction(force: Boolean, gen: Int) {
        if (!force) {
            if (!isAutoSubjectEnabled() || !isDepthEnabled()) return
        }
        if (gen != currentGeneration) return

        val storageState = Environment.getExternalStorageState()
        if (storageState != Environment.MEDIA_MOUNTED) {
            Log.e(TAG, "External storage not mounted: $storageState")
            return
        }

        val wm = WallpaperManager.getInstance(this)
        val isLive = wm.wallpaperInfo != null

        val (wallpaperBitmap, flag) = loadWallpaperBitmap(wm, isLive) ?: run {
            Log.e(TAG, "loadWallpaperBitmap returned null — cannot extract")
            return
        }
        if (gen != currentGeneration) {
            wallpaperBitmap.recycle()
            return
        }

        val cropped = cropToDisplay(wallpaperBitmap, flag)
        if (cropped !== wallpaperBitmap && !wallpaperBitmap.isRecycled) wallpaperBitmap.recycle()
        if (gen != currentGeneration) {
            if (!cropped.isRecycled) cropped.recycle()
            return
        }

        val segmenter = PortraitSegmenter(this)
        segmenter.init()

        if (!segmenter.isReady()) {
            Log.e(TAG, "Segmenter not ready after init() — models likely missing from assets/")
            if (!cropped.isRecycled) cropped.recycle()
            return
        }

        try {
            val foreground = segmenter.segment(cropped)
            if (foreground == null) {
                Log.w(TAG, "segment() returned null — no subject detected in wallpaper")
                return
            }
            if (gen != currentGeneration) {
                foreground.recycle()
                return
            }

            val savedPath = saveForeground(foreground)
            foreground.recycle()

            if (savedPath == null) {
                Log.e(TAG, "saveForeground() failed — check storage permissions and path")
                return
            }
            if (gen != currentGeneration) return

            Settings.System.putStringForUser(
                contentResolver,
                SETTING_SUBJECT_URI,
                savedPath,
                UserHandle.USER_CURRENT,
            )
        } finally {
            segmenter.release()
            if (!cropped.isRecycled) cropped.recycle()
        }
    }

    private fun saveForeground(foreground: Bitmap): String? {
        return try {
            val baseDir = Environment.getExternalStorageDirectory()
            val dir = File(baseDir, "LightningOS/depthwallpaper")

            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.e(TAG, "Failed to create save directory")
                    return null
                }
            }

            dir.listFiles { _, name ->
                name.startsWith(FILE_PREFIX) && name.endsWith(".png")
            }?.forEach { it.delete() }

            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(dir, "${FILE_PREFIX}_${stamp}.png")

            FileOutputStream(file).use { out ->
                if (!foreground.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    Log.e(TAG, "Bitmap.compress() returned false")
                    return null
                }
            }

            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "saveForeground exception", e)
            null
        }
    }

    private fun loadWallpaperBitmap(wm: WallpaperManager, isLive: Boolean): Pair<Bitmap, Int>? {
        if (isLive) {
            loadFromDeStorage()?.let { return Pair(it, WallpaperManager.FLAG_LOCK) }
        }

        for (flag in intArrayOf(WallpaperManager.FLAG_LOCK, WallpaperManager.FLAG_SYSTEM)) {
            try {
                val pfd = wm.getWallpaperFile(flag)
                if (pfd != null) {
                    val bmp = BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor)
                    pfd.close()
                    if (bmp != null) return Pair(bmp, flag)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Wallpaper file load failed for flag $flag", e)
            }
        }

        try {
            val drawable = wm.drawable
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                return Pair(drawable.bitmap.copy(Bitmap.Config.ARGB_8888, false), WallpaperManager.FLAG_SYSTEM)
            }
        } catch (e: Exception) {
            Log.w(TAG, "WM drawable load failed", e)
        }

        if (!isLive) {
            loadFromDeStorage()?.let { return Pair(it, WallpaperManager.FLAG_SYSTEM) }
        }

        Log.e(TAG, "All wallpaper loading strategies failed")
        return null
    }

    private fun loadFromDeStorage(): Bitmap? {
        return try {
            val file = File(createDeviceProtectedStorageContext().filesDir, DE_PHOTO_FILE)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        } catch (e: Exception) {
            Log.w(TAG, "DE storage load failed", e)
            null
        }
    }

    private fun cropToDisplay(bitmap: Bitmap, flag: Int): Bitmap {
        val wm = getSystemService(WindowManager::class.java)
        val maxBounds = wm?.maximumWindowMetrics?.bounds
        val dm = resources.displayMetrics
        val rawW = maxBounds?.width() ?: dm.widthPixels
        val rawH = maxBounds?.height() ?: dm.heightPixels
        // Depth wallpaper is exclusively displayed in portrait; always crop to portrait aspect ratio
        val dstW = minOf(rawW, rawH)
        val dstH = maxOf(rawW, rawH)

        val srcW = bitmap.width
        val srcH = bitmap.height
        if (dstW <= 0 || dstH <= 0 || srcW <= 0 || srcH <= 0) return bitmap

        val wallpaperMgr = WallpaperManager.getInstance(this)
        var visibleCrop: Rect? = null

        try {
            val crops = wallpaperMgr.getBitmapCrops(listOf(Point(dstW, dstH)), flag, /* originalBitmap = */ false)
            val cropsOrig = try {
                wallpaperMgr.getBitmapCrops(listOf(Point(dstW, dstH)), flag, /* originalBitmap = */ true)
            } catch (e: Exception) {
                null
            }

            if (!crops.isNullOrEmpty() && crops[0] != null) {
                val wallpaperFrame = crops[0]
                if (wallpaperFrame.width() > 0 && wallpaperFrame.height() > 0) {
                    val screenRatio = dstW.toFloat() / dstH
                    val frameRatio = wallpaperFrame.width().toFloat() / wallpaperFrame.height()
                    val isRtl = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

                    visibleCrop = if (frameRatio >= screenRatio) {
                        val visibleW = (wallpaperFrame.height() * screenRatio).roundToInt()
                        val visibleLeft = if (isRtl) (wallpaperFrame.right - visibleW) else wallpaperFrame.left
                        Rect(
                            visibleLeft,
                            wallpaperFrame.top,
                            visibleLeft + visibleW,
                            wallpaperFrame.bottom
                        )
                    } else {
                        val visibleH = (wallpaperFrame.width() / screenRatio).roundToInt()
                        val topOffset = (wallpaperFrame.height() - visibleH) / 2
                        Rect(
                            wallpaperFrame.left,
                            wallpaperFrame.top + topOffset,
                            wallpaperFrame.right,
                            wallpaperFrame.top + topOffset + visibleH
                        )
                    }

                    Log.i(TAG, "=== DEPTH WALLPAPER DIAGNOSTIC LOG ===")
                    Log.i(TAG, "original bitmap (getCropFile): width=$srcW, height=$srcH")
                    if (!cropsOrig.isNullOrEmpty() && cropsOrig[0] != null) {
                        val ch = cropsOrig[0]
                        Log.i(TAG, "cropHint (original coords): left=${ch.left}, top=${ch.top}, right=${ch.right}, bottom=${ch.bottom}, width=${ch.width()}, height=${ch.height()}")
                    }
                    Log.i(TAG, "wallpaperFrame (crop coords): left=${wallpaperFrame.left}, top=${wallpaperFrame.top}, right=${wallpaperFrame.right}, bottom=${wallpaperFrame.bottom}, width=${wallpaperFrame.width()}, height=${wallpaperFrame.height()}")
                    Log.i(TAG, "display: width=$dstW, height=$dstH, screenRatio=$screenRatio, frameRatio=$frameRatio, isRtl=$isRtl")
                    Log.i(TAG, "actual wallpaper visible region (WindowManager): left=${visibleCrop.left}, top=${visibleCrop.top}, right=${visibleCrop.right}, bottom=${visibleCrop.bottom}")
                    Log.i(TAG, "========================================")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query system bitmap crops for flag $flag", e)
        }

        val cropRect: Rect = if (visibleCrop != null) {
            val cl = visibleCrop.left.coerceIn(0, srcW - 1)
            val ct = visibleCrop.top.coerceIn(0, srcH - 1)
            val cw = visibleCrop.width().coerceAtMost(srcW - cl)
            val ch = visibleCrop.height().coerceAtMost(srcH - ct)
            Rect(cl, ct, cl + cw, ct + ch)
        } else {
            // Fallback: center-crop to portrait display aspect ratio
            val srcAR = srcW.toFloat() / srcH
            val dstAR = dstW.toFloat() / dstH
            if (srcAR > dstAR) {
                val cropW = (srcH * dstAR).roundToInt().coerceAtMost(srcW)
                val left = (srcW - cropW) / 2
                Rect(left, 0, left + cropW, srcH)
            } else {
                val cropH = (srcW / dstAR).roundToInt().coerceAtMost(srcH)
                val top = (srcH - cropH) / 2
                Rect(0, top, srcW, top + cropH)
            }
        }

        Log.i(TAG, "final bitmap crop rect: left=${cropRect.left}, top=${cropRect.top}, width=${cropRect.width()}, height=${cropRect.height()}")

        if (cropRect.left <= 0 && cropRect.top <= 0
                && cropRect.width() >= srcW - 2 && cropRect.height() >= srcH - 2) {
            return bitmap
        }

        return try {
            Bitmap.createBitmap(bitmap, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
        } catch (e: Exception) {
            Log.w(TAG, "Crop to display failed", e)
            bitmap
        }
    }
}
