package com.autolounge.park.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.autolounge.park.audio.LivePlayerFactory.liveIf
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.autolounge.park.BuildConfig
import com.autolounge.park.R
import com.autolounge.park.i18n.tr
import com.autolounge.park.data.AutoLoungeSyncBridge
import com.autolounge.park.data.CarUsageTracker
import com.autolounge.park.data.NewsChannel
import com.autolounge.park.data.NewsChannelsRepository
import com.autolounge.park.data.NowPlayingStore
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Android Auto: saf ses uygulamasi.
 * - Sadece Canli TV (ses) + Radyo listelenir. Video (YouTube/Web) AA'da yok, parkta telefonda.
 * - Tum oynatilabilir ogelerin gecerli URI'si var -> "Seciminiz yuklenemedi" hatasi olmaz.
 * - Tek ExoPlayer. Ikinci player yok.
 * - Basliklarda emoji/unicode yok, simge drawable artwork ile verilir.
 */
@AndroidEntryPoint
class ParkAudioService : MediaLibraryService() {

    private val TAG = "ParkAudioService"

    @Inject
    lateinit var usageTracker: CarUsageTracker

    @Inject
    lateinit var presentationManager: CarPresentationManager

    private var session: MediaLibrarySession? = null
    private var player: ExoPlayer? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var currentSelectedCountry: String = NewsChannelsRepository.detectUserCountry()

    private val countryChannelCache = java.util.concurrent.ConcurrentHashMap<String, List<NewsChannel>>()
    private fun channelsForCountry(country: String): List<NewsChannel> =
        countryChannelCache.getOrPut(country) { NewsChannelsRepository.getCarAudioChannelsForCountry(country) }

    companion object {
        const val ROOT_ID = "ROOT_AUTOLOUNGE"
        const val FOLDER_DESKTOP = "FOLDER_DESKTOP"
        const val FOLDER_TV = "FOLDER_TV"
        const val FOLDER_RADIO = "FOLDER_RADIO"
        const val FOLDER_COUNTRIES = "FOLDER_COUNTRIES"
        const val COUNTRY_PREFIX = "FOLDER_COUNTRY_"

        // Kokpit kısayolları: ses çalmaz, telefonda resmi siteyi açar (sessiz öğe + hata yok)
        const val APP_YOUTUBE = "app_youtube"
        const val APP_BROWSER = "app_browser"
        const val APP_GEMINI = "app_gemini"
        const val APP_GALLERY = "app_gallery"

        const val CHANNEL_ID = "autolounge_playback_channel"
        const val DEFAULT_STREAM_URL = "https://tv-trthaber.medya.trt.com.tr/master.m3u8"

        const val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        const val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
        const val CONTENT_STYLE_GRID_ITEM_HINT_VALUE = 2
        const val CONTENT_STYLE_LIST_ITEM_HINT_VALUE = 1
        const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
    }

    private fun silentUri(): Uri {
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(packageName)
            .path(R.raw.silence.toString())
            .build()
    }

    private fun isPhoneShortcut(id: String) =
        id == APP_YOUTUBE || id == APP_BROWSER || id == APP_GEMINI || id == APP_GALLERY

    private fun drawableUri(resId: Int): Uri {
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(packageName)
            .path(resId.toString())
            .build()
    }

    private fun stopAllPlayback(reason: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "stopAllPlayback: $reason")
        }
        try {
            player?.pause()
            player?.stop()
            player?.clearMediaItems()
        } catch (e: Exception) {
            Log.w(TAG, "stop error: ${e.message}")
        }
        usageTracker.setAudioStreamingActive(false)
        NowPlayingStore.clear()
        try { presentationManager.dismissPresentation() } catch (_: Exception) { }
        AutoLoungeSyncBridge.requestStop()
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                stopAllPlayback("becoming_noisy_usb_bt_disconnect")
            }
        }
    }

    private var retryCount = 0
    private var retryJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            usageTracker.setAudioStreamingActive(isPlaying)
            // Başlık korunur, sadece durum güncellenir
            NowPlayingStore.update(
                NowPlayingStore.title.value.ifBlank { tr("Canlı Yayın") },
                NowPlayingStore.subtitle.value.ifBlank { "AutoLounge" },
                isPlaying
            )
            if (!isPlaying && player?.playbackState == Player.STATE_IDLE) {
                NowPlayingStore.clear()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                retryCount = 0
                retryJob?.cancel()
            }
            if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                usageTracker.setAudioStreamingActive(false)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "PLAYBACK ERROR: ${error.errorCodeName} ${error.message}", error)
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                player?.seekToDefaultPosition()
                player?.prepare()
                return
            }
            // In-car cellular stall recovery: exponential backoff, max 5 attempts
            if (LivePlayerFactory.isNetworkError(error.errorCode) ||
                error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED) {
                if (retryCount >= 5) return
                val delayMs = minOf(2000L shl retryCount, 30_000L)
                retryCount++
                retryJob?.cancel()
                retryJob = serviceScope.launch {
                    delay(delayMs)
                    try {
                        player?.prepare()
                        player?.play()
                    } catch (e: Exception) {
                        Log.w(TAG, "Cellular stall recovery retry failed: ${e.message}")
                    }
                }
            }
        }
    }

    private fun resolveMediaItem(item: MediaItem): MediaItem {
        val mediaId = item.mediaId
        // Kokpit kısayolları: geçerli sessiz URI (hata ekranı yok), ses çalmaz
        if (isPhoneShortcut(mediaId)) {
            val (title, subtitle, iconRes) = when (mediaId) {
                APP_YOUTUBE -> Triple("YouTube", tr("Videoyu parkta telefonda aç"), R.drawable.ic_aa_youtube_tile)
                APP_BROWSER -> Triple(tr("Web Tarayıcı"), tr("Siteyi parkta telefonda aç"), R.drawable.ic_aa_browser_tile)
                APP_GEMINI -> Triple(tr("Gemini Asistan"), tr("Telefon asistanını başlat"), R.drawable.ic_aa_gemini_tile)
                else -> Triple(tr("Galeri"), tr("Dosyayı telefonda seç"), R.drawable.ic_aa_gallery_tile)
            }
            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setArtist("AutoLounge")
                .setArtworkUri(drawableUri(iconRes))
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build()
            return MediaItem.Builder()
                .setMediaId(mediaId)
                .setUri(silentUri())
                .setMediaMetadata(metadata)
                .build()
        }
        val channel = NewsChannelsRepository.findById(mediaId)
        // Bilinmeyen/legacy id gelirse TRT Haber'e düş (boş URI ile hata vermesin)
        val streamUri = channel?.audioStreamUrl ?: DEFAULT_STREAM_URL
        val title = channel?.title ?: item.mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() } ?: tr("Canlı Yayın")
        val subtitle = channel?.let { "${tr(it.countryName)} - ${tr(it.category)}" }
            ?: item.mediaMetadata.subtitle?.toString()?.takeIf { it.isNotBlank() }
            ?: tr("Canlı")

        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setArtist("AutoLounge")
            .setArtworkUri(drawableUri(R.drawable.ic_aa_radio_tile))
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()

        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(Uri.parse(streamUri))
            .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
            .setMediaMetadata(metadata)
            .liveIf(true)
            .build()
    }

    private val libraryCallback = object : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootExtras = Bundle().apply {
                putBoolean(CONTENT_STYLE_SUPPORTED, true)
                putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_LIST_ITEM_HINT_VALUE)
                putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST_ITEM_HINT_VALUE)
            }
            val rootMetadata = MediaMetadata.Builder()
                .setTitle("AutoLounge")
                .setSubtitle(tr("Kokpit ve TV"))
                .setArtworkUri(drawableUri(R.drawable.ic_logo))
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setExtras(rootExtras)
                .build()
            val rootItem = MediaItem.Builder().setMediaId(ROOT_ID).setMediaMetadata(rootMetadata).build()
            val resultParams = LibraryParams.Builder().setExtras(rootExtras).build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, resultParams))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val items = mutableListOf<MediaItem>()

            when {
                parentId == ROOT_ID -> {
                    // Android Auto Ana Sekmeleri (Tabs): 1. Kokpit, 2. Canlı TV, 3. Radyo
                    // Asla grid hint verme (sekmeler listelenir, Kokpit 1. sırada sabit kalır, 'Diğer' altına kaymaz)
                    items.add(
                        createBrowsableFolder(
                            FOLDER_DESKTOP, tr("Kokpit"),
                            tr("Masaüstü"), R.drawable.ic_aa_desktop, grid = false
                        )
                    )
                    items.add(
                        createBrowsableFolder(
                            FOLDER_TV, tr("Canlı TV"),
                            tr("Kanallar"), R.drawable.ic_aa_tv, grid = false
                        )
                    )
                    items.add(
                        createBrowsableFolder(
                            FOLDER_RADIO, tr("Radyo"),
                            tr("İstasyonlar"), R.drawable.ic_aa_radio_tile, grid = false
                        )
                    )
                }

                parentId == FOLDER_DESKTOP || parentId == "FOLDER_TV_DESKTOP" -> {
                    // Kokpit Masaüstü Izgarası: YouTube, TV, Web Tarayıcı, Radyo, Asistan, Galeri
                    // YouTube ve Web Tarayıcı doğrudan OYNATILABİLİR (playable) öğelerdir -> tek tıkla araç ekranında açılır, ara klasör açmaz!
                    items.add(
                        createPlayableItem(
                            APP_YOUTUBE, "YouTube",
                            tr("Tam Ekran Video"), R.drawable.ic_aa_youtube_tile
                        )
                    )
                    items.add(
                        createBrowsableFolder(
                            FOLDER_TV, tr("Canlı TV"),
                            tr("Haber, Ulusal, Spor"), R.drawable.ic_aa_tv, grid = true
                        )
                    )
                    items.add(
                        createPlayableItem(
                            APP_BROWSER, tr("Web Tarayıcı"),
                            tr("Tam Ekran Web"), R.drawable.ic_aa_browser_tile
                        )
                    )
                    items.add(
                        createBrowsableFolder(
                            FOLDER_RADIO, tr("Radyo"),
                            tr("Müzik ve Haber Radyoları"), R.drawable.ic_aa_radio_tile, grid = true
                        )
                    )
                    items.add(
                        createPlayableItem(
                            APP_GEMINI, tr("Gemini Asistan"),
                            tr("Sesli Asistan"), R.drawable.ic_aa_gemini_tile
                        )
                    )
                    items.add(
                        createPlayableItem(
                            APP_GALLERY, tr("Galeri"),
                            tr("Medya Oynatıcı"), R.drawable.ic_aa_gallery_tile
                        )
                    )
                    val gridParams = LibraryParams.Builder().setExtras(Bundle().apply {
                        putBoolean(CONTENT_STYLE_SUPPORTED, true)
                        putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID_ITEM_HINT_VALUE)
                        putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_GRID_ITEM_HINT_VALUE)
                    }).build()
                    return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(items), gridParams))
                }

                parentId == APP_YOUTUBE -> {
                    // Olası cache/arama durumunda klasör gibi çağrılırsa anında araç ekranında aç
                    NowPlayingStore.update("YouTube", tr("Video Kokpitte"), true)
                    presentationManager.showOnCarScreen("https://m.youtube.com", "YouTube", isWeb = true)
                    items.add(createPlayableItem(APP_YOUTUBE, "YouTube", tr("Tam Ekran Video"), R.drawable.ic_aa_youtube_tile))
                    return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
                }

                parentId == APP_BROWSER -> {
                    NowPlayingStore.update(tr("Web Tarayıcı"), tr("Web Kokpitte"), true)
                    presentationManager.showOnCarScreen("https://www.google.com", tr("Web Tarayıcı"), isWeb = true)
                    items.add(createPlayableItem(APP_BROWSER, tr("Web Tarayıcı"), tr("Tam Ekran Web"), R.drawable.ic_aa_browser_tile))
                    return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
                }

                parentId == FOLDER_TV -> {
                    val currentCountryName = NewsChannelsRepository.getCarSupportedCountries()
                        .firstOrNull { it.first == currentSelectedCountry }?.second ?: "Türkiye"
                    items.add(
                        createBrowsableFolder(
                            FOLDER_COUNTRIES,
                            tr("Ülke Seçimi"),
                            tr("Seçili: %s", tr(currentCountryName)),
                            R.drawable.ic_aa_tv_tile
                        )
                    )
                    channelsForCountry(currentSelectedCountry).forEach { ch ->
                        items.add(createChannelItem(ch.id, ch.title, "${tr(ch.countryName)} - ${tr(ch.category)}"))
                    }
                }

                parentId == FOLDER_COUNTRIES -> {
                    NewsChannelsRepository.getCarSupportedCountries().forEach { (code, name) ->
                        val label = if (code == currentSelectedCountry) tr("%s (Seçili)", tr(name)) else tr(name)
                        items.add(
                            createBrowsableFolder(
                                "$COUNTRY_PREFIX$code",
                                label,
                                tr("Kanalları göster"),
                                R.drawable.ic_aa_tv_tile
                            )
                        )
                    }
                }

                parentId.startsWith(COUNTRY_PREFIX) -> {
                    val code = parentId.removePrefix(COUNTRY_PREFIX)
                    currentSelectedCountry = code
                    NewsChannelsRepository.saveSelectedCountry(code)
                    channelsForCountry(code).forEach { ch ->
                        items.add(createChannelItem(ch.id, ch.title, "${tr(ch.countryName)} - ${tr(ch.category)}"))
                    }
                }

                parentId == FOLDER_RADIO -> {
                    val radios = channelsForCountry(currentSelectedCountry)
                        .filter { it.category == "Radyo" || it.category == "Müzik" }
                    val list = if (radios.isNotEmpty()) radios
                    else channelsForCountry("GLOBAL").take(8)
                    list.forEach { ch ->
                        items.add(createChannelItem(ch.id, ch.title, "${tr(ch.countryName)} - ${tr("Radyo")}"))
                    }
                }

                else -> {
                    // Bilgi satırları (app_*_info) ve bilinmeyen id'ler: boş dön, kanal listesi verme.
                    ImmutableList.of<MediaItem>().let {
                        return Futures.immediateFuture(LibraryResult.ofItemList(it, params))
                    }
                }
            }

            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val resolved = resolveMediaItem(MediaItem.Builder().setMediaId(mediaId).build())
            return Futures.immediateFuture(LibraryResult.ofItem(resolved, null))
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            return Futures.immediateFuture(LibraryResult.ofVoid(params))
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val list = NewsChannelsRepository.searchCarAudioChannels(query).take(20)
            val items = list.map { ch -> createChannelItem(ch.id, ch.title, "${tr(ch.countryName)} - ${tr(ch.category)}") }
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val firstId = mediaItems.firstOrNull()?.mediaId
            if (firstId == null) {
                return Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0))
            }
            // Kokpit kısayolları: sessiz çal (hata yok) + Now Playing ekranında aktif kal (kapanmasın)
            if (isPhoneShortcut(firstId)) {
                val resolved = resolveMediaItem(mediaItems.first())
                try {
                    player?.repeatMode = Player.REPEAT_MODE_ONE // Sessiz sesin bitip ekranın anında kapanmasını önler
                    player?.setMediaItem(resolved)
                    player?.prepare()
                    player?.play()
                } catch (e: Exception) {
                    Log.w(TAG, "silent play: ${e.message}")
                }
                when (firstId) {
                    APP_YOUTUBE -> {
                        NowPlayingStore.update("YouTube", tr("Video Kokpitte"), true)
                        presentationManager.showOnCarScreen("https://m.youtube.com", "YouTube", isWeb = true)
                    }
                    APP_BROWSER -> {
                        NowPlayingStore.update(tr("Web Tarayıcı"), tr("Web Kokpitte"), true)
                        presentationManager.showOnCarScreen("https://www.google.com", tr("Web Tarayıcı"), isWeb = true)
                    }
                    APP_GEMINI -> {
                        NowPlayingStore.update(tr("Gemini Asistan"), tr("Sesli Asistan"), true)
                        runCatching { com.autolounge.park.ui.AssistantLauncher.launch(applicationContext) }
                    }
                    APP_GALLERY -> {
                        NowPlayingStore.update(tr("Galeri"), tr("Medya Kokpiti"), true)
                    }
                }
                return Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(listOf(resolved), 0, 0))
            }
            // Legacy bilinmeyen id gelirse TRT Haber'e düş (hata ekranı olmasın)
            val effectiveId = if (NewsChannelsRepository.findById(firstId) == null) "tr_trthaber" else firstId
            val effectiveItems = if (effectiveId != firstId) {
                mutableListOf(MediaItem.Builder().setMediaId(effectiveId).build())
            } else mediaItems

            val resolvedList = effectiveItems.map { resolveMediaItem(it) }.toMutableList()
            val channel = NewsChannelsRepository.findById(effectiveId)
            try {
                player?.repeatMode = Player.REPEAT_MODE_OFF
                player?.setMediaItems(resolvedList, 0, startPositionMs)
                player?.prepare()
                player?.play()
            } catch (e: Exception) {
                Log.e(TAG, "play error", e)
            }
            val title = channel?.title ?: tr("Canlı TV")
            val sub = channel?.let { "${tr(it.countryName)} - ${tr(it.category)}" } ?: "AutoLounge"
            val stream = channel?.audioStreamUrl ?: "https://tv-trthaber.medya.trt.com.tr/master.m3u8"
            NowPlayingStore.update(title, sub, true)
            // Araç ekranında canlı televizyon videosunu başlat!
            presentationManager.showOnCarScreen(stream, title, isWeb = false)
            return Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(resolvedList, 0, startPositionMs))
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val resolved = mediaItems.map {
                val id = it.mediaId
                if (isPhoneShortcut(id)) {
                    resolveMediaItem(MediaItem.Builder().setMediaId(id).build())
                } else {
                    val eff = if (NewsChannelsRepository.findById(id) == null) "tr_trthaber" else id
                    resolveMediaItem(MediaItem.Builder().setMediaId(eff).build())
                }
            }.toMutableList()
            return Futures.immediateFuture(resolved)
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val defaultItem = resolveMediaItem(MediaItem.Builder().setMediaId("tr_trthaber").build())
            return Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(listOf(defaultItem), 0, 0))
        }
    }

    private fun createBrowsableFolder(id: String, title: String, subtitle: String, icon: Int, grid: Boolean = false): MediaItem {
        val meta = MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setArtworkUri(drawableUri(icon))
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setExtras(Bundle().apply {
                putInt(CONTENT_STYLE_BROWSABLE_HINT, if (grid) CONTENT_STYLE_GRID_ITEM_HINT_VALUE else CONTENT_STYLE_LIST_ITEM_HINT_VALUE)
                putInt(CONTENT_STYLE_PLAYABLE_HINT, if (grid) CONTENT_STYLE_GRID_ITEM_HINT_VALUE else CONTENT_STYLE_LIST_ITEM_HINT_VALUE)
            })
            .build()
        return MediaItem.Builder().setMediaId(id).setMediaMetadata(meta).build()
    }

    /** Bilgi satırı: tıklanmaz, ses çalmaz, oynatıcı açmaz. */
    private fun createStaticInfoRow(id: String, title: String, subtitle: String, icon: Int = R.drawable.ic_logo): MediaItem {
        val meta = MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setArtworkUri(drawableUri(icon))
            .setIsBrowsable(false)
            .setIsPlayable(false)
            .build()
        return MediaItem.Builder().setMediaId(id).setMediaMetadata(meta).build()
    }

    private fun createPlayableItem(id: String, title: String, subtitle: String, icon: Int): MediaItem {
        val meta = MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setArtworkUri(drawableUri(icon))
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()
        return MediaItem.Builder().setMediaId(id).setUri(silentUri()).setMediaMetadata(meta).build()
    }

    /** Eski sürümden kalma oynatılabilir kısayol. */
    private fun createPhoneShortcutItem(id: String, title: String, subtitle: String, icon: Int): MediaItem =
        createPlayableItem(id, title, subtitle, icon)

    private fun createChannelItem(id: String, title: String, subtitle: String): MediaItem {
        val channel = NewsChannelsRepository.findById(id)
        val meta = MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setArtist("AutoLounge")
            .setArtworkUri(drawableUri(R.drawable.ic_aa_radio_tile))
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()
        val builder = MediaItem.Builder().setMediaId(id).setMediaMetadata(meta)
        channel?.audioStreamUrl?.let { builder.setUri(Uri.parse(it)) }
            ?: builder.setUri(Uri.parse(DEFAULT_STREAM_URL))
        return builder.build()
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                tr("AutoLounge Medya"),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = tr("Araç medya oynatma") }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15000,
                /* maxBufferMs = */ 50000,
                /* bufferForPlaybackMs = */ 1000,
                /* bufferForPlaybackAfterRebufferMs = */ 2000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(
                /* backBufferDurationMs = */ 5000,
                /* retainBackBufferFromKeyframe = */ true
            )
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(LivePlayerFactory.mediaSourceFactory(this))
            .setAudioAttributes(audioAttributes, true)
            .setLoadControl(loadControl)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .build()
        exoPlayer.addListener(playerListener)
        player = exoPlayer
        session = MediaLibrarySession.Builder(this, exoPlayer, libraryCallback).build()

        // Preload bundled channels JSON strictly on IO dispatcher
        serviceScope.launch(Dispatchers.IO) {
            NewsChannelsRepository.initBundledChannels(applicationContext)
        }

        runCatching {
            ContextCompat.registerReceiver(
                this,
                noisyReceiver,
                IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                ContextCompat.RECEIVER_EXPORTED
            )
        }

        serviceScope.launch {
            AutoLoungeSyncBridge.activeContentFlow.collectLatest { payload ->
                val uriToPlay = payload.streamUri
                if (uriToPlay != null) {
                    val meta = MediaMetadata.Builder()
                        .setTitle(payload.title)
                        .setSubtitle(payload.subtitle)
                        .setArtist("AutoLounge")
                        .setArtworkUri(drawableUri(R.drawable.ic_logo))
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .build()
                    val item = MediaItem.Builder()
                        .setMediaId(payload.id)
                        .setUri(Uri.parse(uriToPlay))
                        .setMediaMetadata(meta)
                        .liveIf(LivePlayerFactory.isLiveUrl(uriToPlay))
                        .build()
                    retryCount = 0
                    exoPlayer.setMediaItem(item)
                    exoPlayer.prepare()
                    exoPlayer.play()
                    NowPlayingStore.update(payload.title, payload.subtitle, true)
                    presentationManager.showAudioOnly(payload.title)
                } else {
                    try { exoPlayer.stop() } catch (_: Exception) { }
                    usageTracker.setAudioStreamingActive(false)
                    NowPlayingStore.clear()
                    if (presentationManager.hasSecondaryDisplay()) {
                        presentationManager.showOnCarScreen(payload.webUrl, payload.title, isWeb = true)
                    } else {
                        presentationManager.dismissPresentation()
                    }
                }
            }
        }

        serviceScope.launch {
            AutoLoungeSyncBridge.stopRequestFlow.collectLatest {
                try { exoPlayer.pause(); exoPlayer.stop(); exoPlayer.clearMediaItems() } catch (_: Exception) { }
                usageTracker.setAudioStreamingActive(false)
                NowPlayingStore.clear()
                try { presentationManager.dismissPresentation() } catch (_: Exception) { }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopAllPlayback("task_removed")
    }

    override fun onGetSession(info: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onDestroy() {
        retryJob?.cancel()
        runCatching { unregisterReceiver(noisyReceiver) }
        serviceScope.cancel()
        usageTracker.setAudioStreamingActive(false)
        NowPlayingStore.clear()
        player?.removeListener(playerListener)
        session?.run { player.release(); release() }
        player = null
        super.onDestroy()
    }
}
