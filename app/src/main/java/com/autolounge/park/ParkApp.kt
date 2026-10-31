package com.autolounge.park

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.autolounge.park.data.NewsChannelsRepository
import com.autolounge.park.i18n.I18n
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ParkApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .allowHardware(true)
            .respectCacheHeaders(false)
            .crossfade(false)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        I18n.init(this)
        NewsChannelsRepository.initBundledChannelsAsync(this)
        com.autolounge.park.ui.browser.WebContentBlocker.init(this)
    }
}
