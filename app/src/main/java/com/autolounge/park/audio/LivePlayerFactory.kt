package com.autohub.park.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/** Canlı yayın oynatıcıları için ortak ağ/kaynak ayarları. */
@UnstableApi
object LivePlayerFactory {

    fun mediaSourceFactory(ctx: Context): DefaultMediaSourceFactory {
        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
            .setUserAgent("AutoLounge/1.0")
        return DefaultMediaSourceFactory(ctx).setDataSourceFactory(DefaultDataSource.Factory(ctx, http))
    }

    fun isLiveUrl(url: String?): Boolean = url?.contains(".m3u8", ignoreCase = true) == true

    /** Sadece canlı akışlarda hedef gecikme/hız ayarı ekler. */
    fun MediaItem.Builder.liveIf(live: Boolean): MediaItem.Builder =
        if (!live) this else setLiveConfiguration(
            MediaItem.LiveConfiguration.Builder()
                .setTargetOffsetMs(8000)
                .setMaxPlaybackSpeed(1.05f)
                .build()
        )

    fun isNetworkError(code: Int): Boolean = code in listOf(
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED
    )
}
