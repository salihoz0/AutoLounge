package com.autolounge.park.data

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

/**
 * Data bridge for syncing media state and playback commands
 * between the phone UI and Android Auto (CarActivity).
 */
data class ActiveMediaPayload(
    val id: String,
    val title: String,
    val subtitle: String,
    val webUrl: String,
    val streamUri: String? = null,
    val iconUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

object AutoLoungeSyncBridge {
    private val _activeContentFlow = MutableSharedFlow<ActiveMediaPayload>(
        replay = 1,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val activeContentFlow: SharedFlow<ActiveMediaPayload> = _activeContentFlow.asSharedFlow()

    private val _stopRequestFlow = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val stopRequestFlow: SharedFlow<Unit> = _stopRequestFlow.asSharedFlow()

    @Volatile
    var currentActivePayload: ActiveMediaPayload? = null
        private set

    fun syncToCar(
        id: String,
        title: String,
        subtitle: String,
        webUrl: String,
        streamUri: String? = null,
        iconUri: String? = null
    ) {
        val payload = ActiveMediaPayload(
            id = id,
            title = title,
            subtitle = subtitle,
            webUrl = webUrl,
            streamUri = streamUri,
            iconUri = iconUri
        )
        currentActivePayload = payload
        _activeContentFlow.tryEmit(payload)
    }

    fun syncWebToCar(id: String, title: String, webUrl: String) {
        syncToCar(id = id, title = title, subtitle = "Phone Video", webUrl = webUrl, streamUri = null)
    }

    fun requestStop() {
        currentActivePayload = null
        _stopRequestFlow.tryEmit(Unit)
    }

    data class OpenOnPhone(
        val kind: String, // "web", "assistant", "gallery"
        val url: String = "",
        val title: String = ""
    )
    private val _openOnPhoneFlow = MutableSharedFlow<OpenOnPhone>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val openOnPhoneFlow: SharedFlow<OpenOnPhone> = _openOnPhoneFlow.asSharedFlow()

    fun openOnPhone(kind: String, url: String = "", title: String = "") {
        _openOnPhoneFlow.tryEmit(OpenOnPhone(kind, url, title))
    }

    data class CarRequest(
        val kind: String, // "web", "live_tv", "dashboard"
        val url: String = "",
        val title: String = "",
        val streamUrl: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _openOnCarFlow = MutableSharedFlow<CarRequest>(
        replay = 0,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val openOnCarFlow: SharedFlow<CarRequest> = _openOnCarFlow.asSharedFlow()

    private val _pendingCarRequest = AtomicReference<CarRequest?>(null)
    var pendingCarRequest: CarRequest?
        get() = _pendingCarRequest.get()
        private set(value) { _pendingCarRequest.set(value) }

    fun openOnCar(kind: String, url: String = "", title: String = "", streamUrl: String? = null) {
        val req = CarRequest(kind = kind, url = url, title = title, streamUrl = streamUrl)
        _pendingCarRequest.set(req)
        _openOnCarFlow.tryEmit(req)
    }

    fun consumePendingCarRequest(): CarRequest? = _pendingCarRequest.getAndSet(null)

    /**
     * Determines whether a given URL is a direct streaming endpoint (.m3u8, .mpd, audio stream)
     * rather than an HTML webpage, preventing playback stalls in ExoPlayer.
     */
    fun isDirectMediaStream(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val clean = url.trim().lowercase(Locale.ROOT)
        return clean.endsWith(".m3u8") || clean.contains(".m3u8?") ||
            clean.endsWith(".mpd") || clean.contains(".mpd?") ||
            clean.endsWith(".mp4") || clean.contains(".mp4?") ||
            clean.endsWith(".m4a") || clean.contains(".m4a?") ||
            clean.endsWith(".aac") || clean.contains(".aac?") ||
            clean.endsWith(".mp3") || clean.contains(".mp3?") ||
            clean.endsWith(".audio") || clean.contains(".audio?") ||
            clean.contains("/hls/") || clean.contains("format=m3u8") ||
            clean.contains("chunks.m3u8") || clean.contains("playlist.m3u8") ||
            clean.contains("master.m3u8") || clean.contains("manifest") ||
            clean.contains("icecast") || clean.contains("shoutcast") ||
            clean.contains("bbcmedia") || clean.contains("stream.live.") ||
            clean.contains("/mpeg/") || clean.contains("/ts/") ||
            clean.contains(".m3u") || clean.contains(".flv")
    }

    // Telefon klavyesinden araç WebView'ına metin gönderimi
    private val _remoteTextFlow = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val remoteTextFlow: SharedFlow<String> = _remoteTextFlow.asSharedFlow()

    fun sendRemoteText(text: String) {
        if (text.isNotEmpty()) _remoteTextFlow.tryEmit(text)
    }

    private val _carAppAliveFlow = MutableStateFlow(false)
    val carAppAliveFlow: StateFlow<Boolean> = _carAppAliveFlow.asStateFlow()

    fun setCarAppAlive(alive: Boolean) {
        _carAppAliveFlow.value = alive
    }
}
