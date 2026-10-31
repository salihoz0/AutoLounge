package com.autolounge.park.car

import com.autolounge.park.i18n.tr
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceResponse
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.autolounge.park.audio.LivePlayerFactory
import com.autolounge.park.audio.LivePlayerFactory.liveIf
import androidx.media3.ui.PlayerView
import coil.load
import coil.request.CachePolicy
import com.autolounge.park.BuildConfig
import com.autolounge.park.R
import com.autolounge.park.data.AppCatalogRepository
import com.autolounge.park.data.AutoLoungeSyncBridge
import com.autolounge.park.data.BrandLogos
import com.autolounge.park.data.CatalogItem
import com.autolounge.park.data.NewsChannel
import com.autolounge.park.data.NewsChannelsRepository
import com.autolounge.park.data.NowPlayingStore
import com.autolounge.park.data.ParkSafetyManager
import com.autolounge.park.ui.browser.CONSENT_AUTO_ACCEPT_JS
import com.autolounge.park.ui.browser.SPA_SYNC_JS
import com.google.android.apps.auto.sdk.CarActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Primary Android Auto Head Unit activity (CarActivity).
 * Provides automotive fullscreen video playback and media dashboard.
 */
class AutoLoungeCarActivity : CarActivity() {

    private val TAG = "AutoLoungeCarActivity"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var playerRetryCount = 0
    private var playerRetryJob: Job? = null

    private val appRegular: Typeface by lazy {
        ResourcesCompat.getFont(this, R.font.manrope_regular) ?: Typeface.DEFAULT
    }
    private val appMedium: Typeface by lazy {
        ResourcesCompat.getFont(this, R.font.manrope_medium) ?: Typeface.DEFAULT
    }
    private val appSemi: Typeface by lazy {
        ResourcesCompat.getFont(this, R.font.manrope_semibold) ?: Typeface.DEFAULT_BOLD
    }
    private val appBold: Typeface by lazy {
        ResourcesCompat.getFont(this, R.font.manrope_bold) ?: Typeface.DEFAULT_BOLD
    }
    private val appExtraBold: Typeface by lazy {
        ResourcesCompat.getFont(this, R.font.manrope_extrabold) ?: Typeface.DEFAULT_BOLD
    }

    private var exoPlayer: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var webView: WebView? = null
    private var rootLayout: FrameLayout? = null
    private var dashboardScrollView: ScrollView? = null
    private var safetyBarrierView: LinearLayout? = null
    private var channelsContainer: LinearLayout? = null
    private var countryButton: TextView? = null
    private var countryPickerOverlay: FrameLayout? = null
    private var channelsCategoryChipsRow: LinearLayout? = null
    private var channelsListColumn: LinearLayout? = null
    private var channelsSearchEditText: EditText? = null
    private var channelsCountBadge: TextView? = null
    private var channelsCountryCode: String? = null
    private var channelsSelectedCategory: String = "Tümü"
    private var channelsSearchQuery: String = ""
    private var currentCountryChannels: List<NewsChannel> = emptyList()
    private var searchDebounceJob: Job? = null

    // Standalone overlay container for HTML5 fullscreen video rendering.
    private var fullscreenContainer: FrameLayout? = null
    private var customVideoView: View? = null
    private var customVideoCallback: WebChromeClient.CustomViewCallback? = null

    private var titleTextView: TextView? = null
    private var parkBadgeTextView: TextView? = null
    private var backButton: TextView? = null
    private var webLoadingOverlay: View? = null
    private var topBarView: View? = null

    private var currentMode = Mode.DASHBOARD

    enum class Mode {
        DASHBOARD,
        WEB,
        VIDEO,
        CHANNELS
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CarActivityEntryPoint {
        fun safetyManager(): ParkSafetyManager
    }

    private val safetyManager: ParkSafetyManager? by lazy {
        try {
            val appContext = applicationContext ?: return@lazy null
            EntryPointAccessors.fromApplication(appContext, CarActivityEntryPoint::class.java).safetyManager()
        } catch (e: Exception) {
            Log.w(TAG, "SafetyManager inject error: ${e.message}")
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide native Android Auto header in favor of custom automotive navigation bar
        try {
            carUiController?.statusBarController?.apply {
                hideAppHeader()
                hideTitle()
            }
        } catch (e: Exception) {
            Log.d(TAG, "carUiController customize: ${e.message}")
        }

        val ctx = this
        CookieManager.getInstance().setAcceptCookie(true)

        val rootLayout = object : FrameLayout(ctx) {
            override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                if (ev.action == MotionEvent.ACTION_DOWN) {
                    CarPlayerHelper.dismissSettingsIfOpen(playerView, ev.rawX, ev.rawY)
                }
                return super.dispatchTouchEvent(ev)
            }
        }.apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#0A0B0E"))
        }
        this.rootLayout = rootLayout

        // 1. Dashboard ScrollView
        val dashboard = createDashboardView(ctx)
        dashboardScrollView = dashboard
        rootLayout.addView(dashboard)

        // ExoPlayer HLS Live TV View with in-car optimized buffer profile
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

        val p = ExoPlayer.Builder(ctx)
            .setMediaSourceFactory(LivePlayerFactory.mediaSourceFactory(ctx))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .setLoadControl(loadControl)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .build()
        p.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) playerRetryCount = 0
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Car ExoPlayer error: ${error.errorCodeName} ${error.message}", error)
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    p.seekToDefaultPosition()
                    p.prepare()
                    return
                }
                if (LivePlayerFactory.isNetworkError(error.errorCode) && playerRetryCount < 2) {
                    val delayMs = 2000L shl playerRetryCount
                    playerRetryCount++
                    playerRetryJob?.cancel()
                    playerRetryJob = scope.launch {
                        delay(delayMs)
                        p.prepare()
                    }
                    return
                }
                val currentUri = p.currentMediaItem?.localConfiguration?.uri?.toString()
                if (!currentUri.isNullOrBlank() && (currentUri.startsWith("http://") || currentUri.startsWith("https://"))) {
                    openWeb(currentUri, titleTextView?.text?.toString() ?: tr("Canlı Yayın"))
                }
            }
        })
        exoPlayer = p
        val pView = LayoutInflater.from(ctx).inflate(R.layout.view_car_player, null) as PlayerView
        pView.apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                topMargin = dp(48)
            }
            useController = true
            this.player = p
            setBackgroundColor(Color.BLACK)
            visibility = View.GONE
        }
        playerView = pView
        CarPlayerHelper.setupDismissableSettings(pView, scope)
        rootLayout.addView(pView)

        // Driving safety lockout overlay
        val barrier = createSafetyBarrierView(ctx)
        safetyBarrierView = barrier
        rootLayout.addView(barrier)

        // Fixed top cockpit navigation bar
        val topBar = createTopBarView(ctx)
        topBarView = topBar
        rootLayout.addView(topBar)

        // HTML5 fullscreen video viewport
        val fsContainer = FrameLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
            visibility = View.GONE
        }
        fullscreenContainer = fsContainer
        rootLayout.addView(fsContainer)

        setContentView(rootLayout)

        // Remote command bridge from companion phone app
        AutoLoungeSyncBridge.setCarAppAlive(true)
        com.autolounge.park.data.CarSetupChecker.markCarSessionSeen(this)
        AutoLoungeSyncBridge.consumePendingCarRequest()?.let { handleCarRequest(it) }
        scope.launch {
            AutoLoungeSyncBridge.openOnCarFlow.collect { req ->
                handleCarRequest(req)
            }
        }
        scope.launch {
            AutoLoungeSyncBridge.remoteTextFlow.collect { text ->
                injectRemoteText(text)
            }
        }

        // Vehicle park/drive safety state observer
        safetyManager?.let { sm ->
            scope.launch {
                combine(sm.isParked, sm.passengerBypassActive) { isParked, bypass ->
                    Pair(isParked, bypass)
                }.collectLatest { (isParked, bypass) ->
                    val isLockoutActive = !isParked && !bypass
                    safetyBarrierView?.visibility = if (!isLockoutActive || currentMode == Mode.DASHBOARD) View.GONE else View.VISIBLE
                    if (isLockoutActive && currentMode != Mode.DASHBOARD) {
                        safetyBarrierView?.bringToFront()
                    }
                    updateParkBadge(isParked, bypass)
                    if (isLockoutActive) {
                        customVideoView?.visibility = View.INVISIBLE
                        // Sürüşte video görüntüsünü gizle, ses akmaya devam etsin
                        exoPlayer?.let { p ->
                            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
                                .build()
                        }
                        playerView?.videoSurfaceView?.visibility = View.INVISIBLE
                        webView?.evaluateJavascript(
                            "(function(){try{document.querySelectorAll('video').forEach(function(v){v.style.visibility='hidden';});}catch(e){}})();",
                            null
                        )
                    } else {
                        // Park modunda veya yolcu onayı devredeyken video görüntüsünü geri getir
                        customVideoView?.visibility = View.VISIBLE
                        exoPlayer?.let { p ->
                            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                                .build()
                        }
                        playerView?.videoSurfaceView?.visibility = View.VISIBLE
                        webView?.evaluateJavascript(
                            "(function(){try{document.querySelectorAll('video').forEach(function(v){v.style.visibility='visible';});}catch(e){}})();",
                            null
                        )
                    }
                }
            }
        }
    }

    private fun updateParkBadge(isParked: Boolean, isPassengerBypass: Boolean = false) {
        val badge = parkBadgeTextView ?: return
        val textColorHex: String
        val bgHex: String
        val borderHex: String
        val label: String

        if (isParked) {
            textColorHex = "#34D399"
            bgHex = "#12231C"
            borderHex = "#1E3E2B"
            label = tr("● PARK MODU")
        } else if (isPassengerBypass) {
            textColorHex = "#38BDF8"
            bgHex = "#0C2333"
            borderHex = "#164E63"
            label = tr("● YOLCU MODU")
        } else {
            textColorHex = "#FBBF24"
            bgHex = "#251A0D"
            borderHex = "#4A280B"
            label = tr("● SÜRÜŞ (GÜVENLİ)")
        }

        badge.text = label
        badge.setTextColor(Color.parseColor(textColorHex))
        badge.background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(Color.parseColor(bgHex))
            setStroke(dp(1), Color.parseColor(borderHex))
        }
    }

    private fun createTopBarView(ctx: Context): View {
        val topBar = LinearLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply { gravity = Gravity.TOP }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(12), 0)
            setBackgroundColor(Color.parseColor("#E60D0F14"))
        }

        // Cockpit dashboard return button
        val backBtn = TextView(ctx).apply {
            text = tr("Kokpit")
            setTextColor(Color.parseColor("#E2E8F0"))
            textSize = 12.5f
            typeface = appSemi
            letterSpacing = 0.02f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(11), dp(5), dp(13), dp(5))
            val backIcon = ContextCompat.getDrawable(ctx, R.drawable.ic_ui_back)?.apply {
                setTint(Color.parseColor("#94A3B8"))
                setBounds(0, 0, dp(14), dp(14))
            }
            setCompoundDrawables(backIcon, null, null, null)
            compoundDrawablePadding = dp(6)
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.parseColor("#1E222D"))
                setStroke(dp(1), Color.parseColor("#2E3646"))
            }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(32)
            ).apply {
                marginEnd = dp(10)
            }
            layoutParams = lp
            isClickable = true
            isFocusable = true
            visibility = View.GONE
            setOnClickListener { showDashboard() }
        }
        backButton = backBtn
        topBar.addView(backBtn)

        // Header title
        val title = TextView(ctx).apply {
            text = tr("AutoLounge Kokpit")
            setTextColor(Color.WHITE)
            textSize = 14.5f
            typeface = appBold
            letterSpacing = 0.01f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            val carIcon = ContextCompat.getDrawable(ctx, R.drawable.ic_ui_car)?.apply {
                setTint(Color.parseColor("#94A3B8"))
                setBounds(0, 0, dp(16), dp(16))
            }
            setCompoundDrawables(carIcon, null, null, null)
            compoundDrawablePadding = dp(7)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(8)
            }
        }
        titleTextView = title
        topBar.addView(title)

        // Quick launch pills (YouTube, Live TV, Web Browser)
        val quickTabs = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        quickTabs.addView(createPillButton(
            ctx = ctx,
            text = "YouTube",
            accentColor = Color.parseColor("#EF4444"),
            iconRes = R.drawable.ic_youtube,
            logoUri = BrandLogos.uri("youtube"),
            tintIcon = false
        ) {
            openWeb("https://m.youtube.com", "YouTube")
        })
        quickTabs.addView(createPillButton(
            ctx = ctx,
            text = "TV",
            accentColor = Color.parseColor("#10B981"),
            iconRes = R.drawable.ic_ui_livetv,
            tintIcon = true
        ) {
            openChannels()
        })
        quickTabs.addView(createPillButton(
            ctx = ctx,
            text = tr("Web"),
            accentColor = Color.parseColor("#38BDF8"),
            iconRes = R.drawable.ic_ui_globe,
            tintIcon = true
        ) {
            openWeb("https://www.google.com", "Web Tarayıcı")
        })

        topBar.addView(quickTabs)

        // Park Modu Rozeti (Sleek indicator pill)
        val badge = TextView(ctx).apply {
            textSize = 10.5f
            typeface = appBold
            letterSpacing = 0.06f
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(4), dp(10), dp(4))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(28)
            ).apply {
                marginStart = dp(4)
            }
            layoutParams = lp
        }
        parkBadgeTextView = badge
        updateParkBadge(isParked = true)
        topBar.addView(badge)

        return topBar
    }

    private fun createPillButton(
        ctx: Context,
        text: String,
        accentColor: Int,
        iconRes: Int? = null,
        logoUri: String? = null,
        tintIcon: Boolean = true,
        onClick: () -> Unit
    ): View {
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(6), dp(12), dp(6))

            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.parseColor("#161820"))
                setStroke(dp(1), Color.parseColor("#262A36"))
            }

            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(32)
            ).apply {
                marginEnd = dp(8)
            }
            layoutParams = params
            isClickable = true
            isFocusable = true

            // Brand logo or vector icon container
            if (!logoUri.isNullOrBlank()) {
                val img = ImageView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(16), dp(16)).apply {
                        marginEnd = dp(6)
                    }
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    load(logoUri) {
                        crossfade(true)
                        if (iconRes != null) {
                            placeholder(iconRes)
                            error(iconRes)
                        }
                    }
                }
                addView(img)
            } else if (iconRes != null) {
                val img = ImageView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(15), dp(15)).apply {
                        marginEnd = dp(6)
                    }
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    val drawable = ContextCompat.getDrawable(ctx, iconRes)?.mutate()
                    if (tintIcon) {
                        drawable?.setTint(accentColor)
                    }
                    setImageDrawable(drawable)
                }
                addView(img)
            }

            val label = TextView(ctx).apply {
                this.text = tr(text)
                setTextColor(Color.parseColor("#F1F5F9"))
                textSize = 12f
                typeface = appSemi
                letterSpacing = 0.02f
            }
            addView(label)

            setOnClickListener { onClick() }
        }
    }

    private fun createDashboardView(ctx: Context): ScrollView {
        val scroll = ScrollView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply { topMargin = dp(48) }
            setBackgroundColor(Color.parseColor("#0A0B0E"))
        }

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }

        // Video & Media Catalog section
        container.addView(createSectionHeader(ctx, "Video & Medya", R.drawable.ic_ui_movie, "#EF4444"))
        val videoRow = createHorizontalAppRow(ctx, listOf(
            AppCard("cat_youtube", "YouTube", "Popüler", "#EF4444", R.drawable.ic_aa_desktop, BrandLogos.uri("youtube")) {
                openWeb("https://m.youtube.com", "YouTube")
            },
            AppCard("cat_netflix", "Netflix", "Dizi & Film", "#E50914", R.drawable.ic_aa_desktop, BrandLogos.uri("netflix")) {
                openWeb("https://www.netflix.com", "Netflix")
            },
            AppCard("cat_disney", "Disney+", "Marvel & Pixar", "#1E40AF", R.drawable.ic_aa_desktop, BrandLogos.uri("disneyplus")) {
                openWeb("https://www.disneyplus.com", "Disney+")
            },
            AppCard("cat_prime", "Prime Video", "Amazon", "#0284C7", R.drawable.ic_aa_desktop, BrandLogos.uri("primevideo")) {
                openWeb("https://www.primevideo.com", "Prime Video")
            },
            AppCard("cat_twitch", "Twitch", "Canlı Oyun", "#9333EA", R.drawable.ic_aa_desktop, BrandLogos.uri("twitch")) {
                openWeb("https://m.twitch.tv", "Twitch")
            },
            AppCard("cat_kick", "Kick", "Yayın", "#10B981", R.drawable.ic_aa_desktop, BrandLogos.uri("kick")) {
                openWeb("https://kick.com", "Kick")
            },
            AppCard("cat_tabii", "TRT Tabii", "TRT Dizi & Film", "#00C389", R.drawable.ic_aa_desktop, BrandLogos.uri("tabii")) {
                openWeb("https://www.tabii.com", "TRT Tabii")
            },
            AppCard("cat_exxen", "Exxen", "Dizi & Spor", "#F59E0B", R.drawable.ic_aa_desktop, BrandLogos.uri("exxen")) {
                openWeb("https://www.exxen.com", "Exxen")
            },
            AppCard("cat_gain", "GAİN", "Yeni Nesil İçerik", "#EF4444", R.drawable.ic_aa_desktop, BrandLogos.uri("gain")) {
                openWeb("https://www.gain.tv", "GAİN")
            },
            AppCard("cat_blutv", "BluTV", "Özel Diziler", "#1E3A8A", R.drawable.ic_aa_desktop, BrandLogos.uri("blutv")) {
                openWeb("https://www.blutv.com", "BluTV")
            },
            AppCard("cat_max", "Max", "HBO & DC", "#0026FF", R.drawable.ic_aa_desktop, BrandLogos.uri("max")) {
                openWeb("https://www.max.com", "Max")
            },
            AppCard("cat_appletv", "Apple TV+", "Apple Orijinalleri", "#0F172A", R.drawable.ic_aa_desktop, BrandLogos.uri("appletv")) {
                openWeb("https://tv.apple.com", "Apple TV+")
            }
        ))
        container.addView(videoRow)

        // Live TV section
        container.addView(createSectionHeader(ctx, "Canlı TV", R.drawable.ic_ui_livetv, "#10B981", "Tüm Kanallar →") {
            openChannels()
        })
        val tvRow = createHorizontalAppRow(ctx, listOf(
            AppCard("tv_all", "Tüm Kanallar", "Ülke & Kanal Seç", "#10B981", R.drawable.ic_ui_livetv) {
                openChannels()
            },
            AppCard("tv_trthaber", "TRT Haber", "Canlı Yayın", "#EF4444", R.drawable.ic_aa_tv, NewsChannelsRepository.logoFor("TRT Haber") ?: BrandLogos.uri("trthaber")) {
                openLiveTv("https://tv-trthaber.medya.trt.com.tr/master.m3u8", "TRT Haber")
            },
            AppCard("tv_trt1", "TRT 1", "Dizi & Film", "#DC2626", R.drawable.ic_aa_tv, NewsChannelsRepository.logoFor("TRT 1") ?: BrandLogos.uri("trt1")) {
                openLiveTv("https://tv-trt1.medya.trt.com.tr/master.m3u8", "TRT 1")
            },
            AppCard("tv_trtspor", "TRT Spor", "Canlı Maç", "#16A34A", R.drawable.ic_aa_tv, NewsChannelsRepository.logoFor("TRT Spor") ?: BrandLogos.uri("trtspor")) {
                openLiveTv("https://tv-trtspor1.medya.trt.com.tr/master.m3u8", "TRT Spor")
            },
            AppCard("tv_atv", "ATV", "Diziler", "#F59E0B", R.drawable.ic_aa_tv, BrandLogos.uri("atv")) {
                openWeb("https://www.atv.com.tr/canli-yayin", "ATV")
            },
            AppCard("tv_showtv", "Show TV", "Dizi & Eğlence", "#EF4444", R.drawable.ic_aa_tv, BrandLogos.uri("showtv")) {
                openWeb("https://www.showtv.com.tr/canli-yayin", "Show TV")
            },
            AppCard("tv_tv8", "TV8", "Eğlence", "#DC2626", R.drawable.ic_aa_tv, BrandLogos.uri("tv8")) {
                openWeb("https://www.tv8.com.tr/canli-yayin", "TV8")
            },
            AppCard("tv_kanald", "Kanal D", "Canlı Yayın", "#2563EB", R.drawable.ic_aa_tv, BrandLogos.uri("kanald")) {
                openWeb("https://www.kanald.com.tr/canli-yayin", "Kanal D")
            },
            AppCard("tv_startv", "Star TV", "Diziler", "#EC4899", R.drawable.ic_aa_tv, BrandLogos.uri("startv")) {
                openWeb("https://www.startv.com.tr/canli-yayin", "Star TV")
            },
            AppCard("tv_now", "NOW", "Ulusal", "#EA580C", R.drawable.ic_aa_tv, BrandLogos.uri("now")) {
                openWeb("https://www.nowtv.com.tr/canli-yayin", "NOW (FOX)")
            },
            AppCard("tv_24", "24 TV", "Haber", "#2563EB", R.drawable.ic_aa_tv, NewsChannelsRepository.logoFor("24 TV") ?: BrandLogos.uri("24tv")) {
                openLiveTv("https://turkmedya-live.ercdn.net/tv24/tv24.m3u8", "24 TV")
            },
            AppCard("tv_tv360", "TV360", "Dizi & Gündem", "#991B1B", R.drawable.ic_aa_tv, NewsChannelsRepository.logoFor("TV360") ?: BrandLogos.uri("tv360")) {
                openLiveTv("https://turkmedya-live.ercdn.net/tv360/tv360.m3u8", "TV360")
            },
            AppCard("tv_ahaber", "A Haber", "Gündem", "#DC2626", R.drawable.ic_aa_tv, NewsChannelsRepository.logoFor("A Haber") ?: BrandLogos.uri("ahaber")) {
                openLiveTv("https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/ahaber/ahaber.m3u8", "A Haber")
            }
        ))
        container.addView(tvRow)

        // 3. Kesintisiz Radyolar
        container.addView(createSectionHeader(ctx, "Radyo", R.drawable.ic_ui_radio, "#F59E0B"))
        val radioRow = createHorizontalAppRow(ctx, listOf(
            AppCard("rad_power", "Power FM", "Hit Müzik", "#F59E0B", R.drawable.ic_aa_radio_tile, NewsChannelsRepository.logoFor("Power FM") ?: BrandLogos.uri("powerfm")) {
                openLiveTv("https://listen.powerapp.com.tr/powerfm/mpeg/icecast.audio", "Power FM")
            },
            AppCard("rad_powerturk", "Power Türk", "Türkçe Pop", "#EF4444", R.drawable.ic_aa_radio_tile, NewsChannelsRepository.logoFor("Power Türk") ?: BrandLogos.uri("powerturk")) {
                openLiveTv("https://listen.powerapp.com.tr/powerturk/mpeg/icecast.audio", "Power Türk")
            },
            AppCard("rad_kral", "KRAL FM", "Pop & Dans", "#3B82F6", R.drawable.ic_aa_radio_tile, NewsChannelsRepository.logoFor("KRAL FM") ?: BrandLogos.uri("kralfm")) {
                openLiveTv("https://dygedge2.radyotvonline.net/kralfm/playlist.m3u8", "KRAL FM")
            },
            AppCard("rad_powerpop", "Power POP", "Yabancı Pop", "#DC2626", R.drawable.ic_aa_radio_tile, NewsChannelsRepository.logoFor("Power POP") ?: BrandLogos.uri("powerpop")) {
                openLiveTv("https://listen.powerapp.com.tr/powerpop/128/chunks.m3u8", "Power POP")
            }
        ))
        container.addView(radioRow)

        // Tools & Web section
        container.addView(createSectionHeader(ctx, "Araçlar", R.drawable.ic_ui_bolt, "#38BDF8"))
        val toolsRow = createHorizontalAppRow(ctx, listOf(
            AppCard("tool_web", "Web Tarayıcı", "Google & Siteler", "#38BDF8", R.drawable.ic_ui_globe) {
                openWeb("https://www.google.com", "Web Tarayıcı")
            },
            AppCard("tool_gemini", "Gemini AI", "Yapay Zeka", "#8B5CF6", R.drawable.ic_aa_desktop, BrandLogos.uri("gemini")) {
                openWeb("https://gemini.google.com", "Google Gemini")
            },
            AppCard("tool_trading", "TradingView", "Borsa & Kripto", "#10B981", R.drawable.ic_aa_desktop, BrandLogos.uri("tradingview")) {
                openWeb("https://tr.tradingview.com", "TradingView")
            }
        ))
        container.addView(toolsRow)

        scroll.addView(container)
        return scroll
    }

    private data class AppCard(
        val id: String,
        val title: String,
        val subtitle: String,
        val accentHex: String,
        val iconRes: Int,
        val logoUrl: String? = null,
        val onClick: () -> Unit
    )

    private fun createHorizontalAppRow(ctx: Context, cards: List<AppCard>): View {
        val hScroll = HorizontalScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) }
            isHorizontalScrollBarEnabled = false
        }

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        cards.forEach { card ->
            val cardView = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(10), dp(12), dp(10), dp(10))
                val shape = GradientDrawable().apply {
                    cornerRadius = dp(18).toFloat()
                    setColor(Color.parseColor("#14161D"))
                    setStroke(dp(1), Color.parseColor("#242938"))
                }
                background = shape
                layoutParams = LinearLayout.LayoutParams(dp(134), dp(120)).apply {
                    marginEnd = dp(12)
                }

                val iconBox = FrameLayout(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(46), dp(46)).apply {
                        bottomMargin = dp(10)
                    }
                    background = GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        setColor(
                            if (card.logoUrl.isNullOrBlank()) Color.parseColor(card.accentHex)
                            else Color.parseColor("#F8FAFC")
                        )
                    }
                    if (!card.logoUrl.isNullOrBlank()) {
                        addView(ImageView(ctx).apply {
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            layoutParams = FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER)
                            load(card.logoUrl) {
                                crossfade(false)
                                allowHardware(true)
                                memoryCachePolicy(CachePolicy.ENABLED)
                                diskCachePolicy(CachePolicy.ENABLED)
                            }
                        })
                    } else {
                        addView(ImageView(ctx).apply {
                            setImageResource(card.iconRes)
                            setColorFilter(Color.WHITE)
                            layoutParams = FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER)
                        })
                    }
                }
                addView(iconBox)

                val t = TextView(ctx).apply {
                    text = tr(card.title)
                    setTextColor(Color.parseColor("#F1F5F9"))
                    textSize = 13f
                    typeface = appSemi
                    letterSpacing = 0.01f
                    maxLines = 1
                    gravity = Gravity.CENTER
                }
                addView(t)

                val sub = TextView(ctx).apply {
                    text = tr(card.subtitle)
                    setTextColor(Color.parseColor("#8A94A6"))
                    textSize = 10.5f
                    typeface = appRegular
                    letterSpacing = 0.01f
                    maxLines = 1
                    gravity = Gravity.CENTER
                }
                addView(sub)

                setOnClickListener { card.onClick() }
            }
            row.addView(cardView)
        }

        hScroll.addView(row)
        return hScroll
    }

    private fun createSectionHeader(
        ctx: Context,
        title: String,
        iconRes: Int,
        accentHex: String,
        actionText: String? = null,
        onAction: (() -> Unit)? = null
    ): View {
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(8))
            addView(ImageView(ctx).apply {
                setImageResource(iconRes)
                setColorFilter(Color.parseColor(accentHex))
                layoutParams = LinearLayout.LayoutParams(dp(16), dp(16)).apply { marginEnd = dp(7) }
            })
            addView(TextView(ctx).apply {
                text = tr(title)
                setTextColor(Color.parseColor("#E7EBF2"))
                textSize = 13.5f
                typeface = appSemi
                letterSpacing = 0.02f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            if (actionText != null && onAction != null) {
                addView(TextView(ctx).apply {
                    text = tr(actionText)
                    setTextColor(Color.parseColor(accentHex))
                    textSize = 11.5f
                    typeface = appBold
                    setPadding(dp(9), dp(4), dp(9), dp(4))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(12).toFloat()
                        setColor(Color.parseColor("#142721"))
                        setStroke(dp(1), Color.parseColor(accentHex))
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { onAction() }
                })
            }
        }
    }

    private fun createSafetyBarrierView(ctx: Context): LinearLayout {
        return LinearLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply { topMargin = dp(48) }
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#EE080C14"))
            visibility = View.GONE

            val shield = ImageView(ctx).apply {
                setImageResource(R.drawable.ic_aa_tv)
                setColorFilter(Color.parseColor("#F59E0B"))
                layoutParams = LinearLayout.LayoutParams(dp(54), dp(54)).apply {
                    bottomMargin = dp(14)
                }
            }
            addView(shield)

            val warning = TextView(ctx).apply {
                text = tr("Sürüş Güvenliği Uyarısı")
                setTextColor(Color.WHITE)
                textSize = 19f
                typeface = appBold
                letterSpacing = 0.01f
                gravity = Gravity.CENTER
            }
            addView(warning)

            val desc = TextView(ctx).apply {
                text = tr("Lütfen sürüş esnasında sürücü olarak video izlemeyiniz.\nTrafik ve can güvenliğiniz her şeyden önemlidir.\nSes yayını kesintisiz olarak araç hoparlörlerinde çalmaya devam ediyor.")
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 13f
                typeface = appRegular
                letterSpacing = 0.015f
                setLineSpacing(dp(3).toFloat(), 1f)
                gravity = Gravity.CENTER
                setPadding(dp(36), dp(10), dp(36), dp(18))
            }
            addView(desc)

            // Eylem Butonları (Tamam / Yolcuyum)
            val actionsRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                // 1. TAMAM (Sadece Dinle)
                val okBtn = TextView(ctx).apply {
                    text = tr("Tamam (Ses Devam Etsin)")
                    setTextColor(Color.parseColor("#E2E8F0"))
                    textSize = 12.5f
                    typeface = appSemi
                    gravity = Gravity.CENTER
                    setPadding(dp(16), dp(10), dp(16), dp(10))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(Color.parseColor("#1E2430"))
                        setStroke(dp(1), Color.parseColor("#333D52"))
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        // Sürücü onayı: bilgilendirme metnini güncelle, ses kesintisiz devam eder
                        desc.text = tr("Sürüş güvenliği modu devrede.\nCanlı ses kesintisiz olarak araç hoparlörlerinde çalmaya devam ediyor.")
                        visibility = View.GONE
                    }
                }
                addView(okBtn, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dp(12) })

                // 2. YOLCUYUM (Videoyu Aç)
                val passengerBtn = TextView(ctx).apply {
                    text = tr("Yolcuyum (Videoyu Aç)")
                    setTextColor(Color.WHITE)
                    textSize = 12.5f
                    typeface = appBold
                    gravity = Gravity.CENTER
                    setPadding(dp(18), dp(10), dp(18), dp(10))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(20).toFloat()
                        setColor(Color.parseColor("#059669"))
                        setStroke(dp(1), Color.parseColor("#10B981"))
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        // Yolcu onayı: sürüş esnasında video kilidini kaldır
                        safetyManager?.setPassengerBypass(true)
                        safetyBarrierView?.visibility = View.GONE
                    }
                }
                addView(passengerBtn)
            }
            addView(actionsRow)
        }
    }

    /** Dismisses custom HTML5 video view and restores WebView. */
    private fun hideCustomVideo() {
        val view = customVideoView ?: return
        try {
            fullscreenContainer?.removeView(view)
        } catch (_: Exception) {}
        fullscreenContainer?.visibility = View.GONE
        customVideoView = null
        try { customVideoCallback?.onCustomViewHidden() } catch (_: Exception) {}
        customVideoCallback = null
        if (currentMode == Mode.WEB) {
            webView?.visibility = View.VISIBLE
        }
    }

    /** Lazily instantiates and configures the WebView. */
    private fun ensureWebView(): WebView {
        webView?.let { return it }
        val wView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                topMargin = dp(48) // Top bar height offset
            }
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, true)
            isFocusable = true
            isFocusableInTouchMode = true
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                allowFileAccess = false
                allowContentAccess = false
                offscreenPreRaster = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    safeBrowsingEnabled = true
                }
                setGeolocationEnabled(false)
                userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
            }
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            if (BuildConfig.DEBUG) {
                WebView.setWebContentsDebuggingEnabled(true)
            }
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    // Reklam/izleyici istekleri ağa çıkmadan kapatılır (ParkBrowser ile aynı engelleyici)
                    if (request != null && !request.isForMainFrame &&
                        com.autolounge.park.ui.browser.WebContentBlocker.isBlocked(request.url)
                    ) {
                        return com.autolounge.park.ui.browser.WebContentBlocker.emptyResponse()
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val targetUri = request?.url ?: return false
                    val url = targetUri.toString()
                    val scheme = targetUri.scheme?.lowercase() ?: return false
                    if (scheme == "http" || scheme == "https") return false

                    val isLockoutActive = safetyManager?.isDrivingLockoutActive ?: false
                    if (isLockoutActive) return true
                    if (scheme in setOf("file", "content", "javascript", "data", "about")) return true

                    return try {
                        val intent = if (scheme == "intent") {
                            Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                                addCategory(Intent.CATEGORY_BROWSABLE)
                                component = null
                                selector = null
                                flags = 0
                            }
                        } else {
                            if (scheme !in setOf("tel", "mailto", "market", "geo")) return true
                            Intent(Intent.ACTION_VIEW, targetUri).apply {
                                addCategory(Intent.CATEGORY_BROWSABLE)
                                flags = 0
                            }
                        }
                        if (intent.action != null && intent.action != Intent.ACTION_VIEW) {
                            return true
                        }
                        val fallback = intent.getStringExtra("browser_fallback_url")
                        if (!fallback.isNullOrBlank() && (fallback.startsWith("http://") || fallback.startsWith("https://"))) {
                            view?.loadUrl(fallback)
                            return true
                        }
                        // Android Auto CarActivity: avoid launching arbitrary phone activities on the head unit
                        true
                    } catch (_: Exception) {
                        true
                    }
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    view?.evaluateJavascript(SPA_SYNC_JS, null)
                }

                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    view?.title?.let { t ->
                        if (t.isNotBlank() && currentMode == Mode.WEB) {
                            titleTextView?.text = t
                        }
                    }
                    view?.evaluateJavascript(SPA_SYNC_JS, null)
                }

                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    super.onPageCommitVisible(view, url)
                    if (currentMode == Mode.WEB) {
                        view?.visibility = View.VISIBLE
                        hideWebLoading()
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (com.autolounge.park.ui.browser.WebContentBlocker.enabled.value) {
                        view?.evaluateJavascript(com.autolounge.park.ui.browser.WebContentBlocker.COSMETIC_JS, null)
                    }
                    if (currentMode == Mode.WEB) {
                        view?.visibility = View.VISIBLE
                        hideWebLoading()
                    }
                    view?.title?.let { t ->
                        if (t.isNotBlank() && currentMode == Mode.WEB) {
                            titleTextView?.text = t
                        }
                    }
                    view?.evaluateJavascript(SPA_SYNC_JS, null)
                    // Suppress mobile app banner
                    if (url?.contains("youtube.com") == true) {
                        view?.evaluateJavascript(
                            "(function(){try{var u=document.querySelector('ytm-upsell-dialog-renderer');if(u)u.style.display='none';}catch(e){}})();",
                            null
                        )
                    }
                    // Auto-dismiss consent banners
                    view?.evaluateJavascript(CONSENT_AUTO_ACCEPT_JS, null)
                    view?.postDelayed({
                        try { view.evaluateJavascript(CONSENT_AUTO_ACCEPT_JS, null) } catch (_: Exception) {}
                    }, 1500)
                }
            }
            // WebChromeClient custom view support for HTML5 fullscreen playback
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    val isLockoutActive = safetyManager?.isDrivingLockoutActive ?: false
                    if (isLockoutActive) {
                        callback?.onCustomViewHidden()
                        return
                    }
                    if (customVideoView != null) {
                        callback?.onCustomViewHidden()
                        return
                    }
                    if (view == null) return
                    customVideoView = view
                    customVideoCallback = callback
                    webView?.visibility = View.GONE
                    fullscreenContainer?.apply {
                        removeAllViews()
                        addView(
                            view,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                        visibility = View.VISIBLE
                        bringToFront()
                    }
                }

                @Deprecated("Deprecated in Java", ReplaceWith("onShowCustomView(view, callback)"))
                @Suppress("DEPRECATION")
                override fun onShowCustomView(view: View?, requestedOrientation: Int, callback: CustomViewCallback?) {
                    onShowCustomView(view, callback)
                }

                override fun onHideCustomView() {
                    hideCustomVideo()
                }

                override fun onPermissionRequest(request: PermissionRequest?) {
                    val req = request ?: return
                    try {
                        // Only grant protected media DRM identifier for video platforms (e.g., Widevine DRM)
                        // Explicitly deny microphone/camera access to protect cabin privacy
                        val allowed = req.resources.filter { it == PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID }
                        if (allowed.isNotEmpty()) {
                            req.grant(allowed.toTypedArray())
                        } else {
                            req.deny()
                        }
                    } catch (_: Exception) {
                        try { req.deny() } catch (_: Exception) {}
                    }
                }

                // Auto-confirm JS alerts/dialogs to prevent UI freeze
                override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                    result?.confirm()
                    return true
                }

                override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                    result?.confirm()
                    return true
                }

                override fun onJsPrompt(
                    view: WebView?, url: String?, message: String?,
                    defaultValue: String?, result: JsPromptResult?
                ): Boolean {
                    result?.confirm(defaultValue ?: "")
                    return true
                }
            }

            addJavascriptInterface(
                object {
                    @android.webkit.JavascriptInterface
                    fun onSpaUrlChanged(url: String?, title: String?) {
                        if (!title.isNullOrBlank() && currentMode == Mode.WEB) {
                            titleTextView?.post {
                                titleTextView?.text = tr(title)
                            }
                        }
                    }
                },
                "AutoLoungeBridge"
            )

            setBackgroundColor(Color.BLACK)
            visibility = View.GONE
            loadUrl("about:blank")
        }
        webView = wView
        rootLayout?.let { root ->
            root.addView(wView, 1.coerceAtMost(root.childCount))
        }
        return wView
    }

    private fun createWebLoadingOverlay(ctx: Context): View {
        return FrameLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply { topMargin = dp(48) }
            setBackgroundColor(Color.parseColor("#0A0B0E"))
            visibility = View.GONE

            val content = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
                val spinner = ProgressBar(ctx).apply {
                    isIndeterminate = true
                    indeterminateTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
                }
                addView(spinner)

                val t = TextView(ctx).apply {
                    tag = "loading_text"
                    text = tr("Yükleniyor...")
                    setTextColor(Color.parseColor("#94A3B8"))
                    textSize = 13f
                    typeface = appSemi
                    setPadding(0, dp(14), 0, 0)
                }
                addView(t)
            }
            addView(content)
        }
    }

    private fun showWebLoading(title: String) {
        val overlay = webLoadingOverlay ?: createWebLoadingOverlay(this).also {
            webLoadingOverlay = it
            rootLayout?.addView(it, 2.coerceAtMost(rootLayout?.childCount ?: 1))
        }
        overlay.findViewWithTag<TextView>("loading_text")?.text = tr("%s açılıyor...", title)
        overlay.visibility = View.VISIBLE
        overlay.bringToFront()
        topBarView?.bringToFront()
    }

    private fun hideWebLoading() {
        webLoadingOverlay?.visibility = View.GONE
    }

    /** Dispatches remote intent requests from the mobile app bridge. */
    private fun handleCarRequest(req: AutoLoungeSyncBridge.CarRequest) {
        when (req.kind) {
            "web" -> openWeb(
                url = req.url.ifBlank { "https://www.google.com" },
                title = req.title.ifBlank { "AutoLounge" },
                fromRemote = true
            )
            "live_tv" -> {
                val stream = req.streamUrl ?: req.url
                if (AutoLoungeSyncBridge.isDirectMediaStream(stream)) {
                    openLiveTv(stream, req.title.ifBlank { tr("Canlı Yayın") }, fromRemote = true)
                } else {
                    val targetUrl = req.url.ifBlank { stream }
                    if (targetUrl.isNotBlank()) {
                        openWeb(targetUrl, req.title.ifBlank { tr("Canlı Yayın") }, fromRemote = true)
                    }
                }
            }
            "dashboard" -> showDashboard(fromRemote = true)
        }
    }

    /** Injects remote keystrokes into active web input element with React/Vue state synchronization. */
    private fun injectRemoteText(text: String) {
        val wv = ensureWebView()
        val quoted = JSONObject.quote(text)
        wv.evaluateJavascript(
            "(function(){try{var t=$quoted;var el=document.activeElement;" +
                "if(!el||el===document.body||el.tagName==='BODY'||el.tagName==='HTML'){el=document.querySelector('input:focus,textarea:focus,[contenteditable=\"true\"]:focus');}" +
                "if(!el||el===document.body||el.tagName==='BODY'){el=document.querySelector('input:not([type=\"hidden\"]):not([disabled]):not([readonly]),textarea:not([disabled]):not([readonly]),[contenteditable=\"true\"]');}" +
                "if(!el)return 'none';el.focus();" +
                "var inserted=false;try{inserted=document.execCommand('insertText',false,t);}catch(e){inserted=false;}" +
                "if(!inserted||(el.value!==undefined&&!el.value.includes(t))){" +
                "if('value' in el){var prevVal=el.value||'';var newVal=prevVal+t;" +
                "var proto=Object.getPrototypeOf(el);var descriptor=Object.getOwnPropertyDescriptor(proto,'value');var setter=descriptor?descriptor.set:null;" +
                "if(!setter&&window.HTMLInputElement){var inputDesc=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value');setter=inputDesc?inputDesc.set:null;}" +
                "if(setter){setter.call(el,newVal);}else{el.value=newVal;}" +
                "if(el._valueTracker){try{el._valueTracker.setValue(prevVal);}catch(e){}}" +
                "}else{el.textContent=(el.textContent||'')+t;}" +
                "try{el.dispatchEvent(new InputEvent('input',{bubbles:true,cancelable:true,inputType:'insertText',data:t}));}catch(e){el.dispatchEvent(new Event('input',{bubbles:true,cancelable:true}));}" +
                "el.dispatchEvent(new Event('change',{bubbles:true,cancelable:true}));}" +
                "try{el.dispatchEvent(new KeyboardEvent('keyup',{bubbles:true,cancelable:true,key:t.slice(-1)}));}catch(e){}" +
                "return 'ok';}catch(err){return 'err:'+err;}})()",
            null
        )
    }

    fun openWeb(url: String, title: String, fromRemote: Boolean = false) {
        hideCountryPicker()
        CarPlayerHelper.dismissSettingsIfOpen(playerView)
        hideCustomVideo()
        currentMode = Mode.WEB
        titleTextView?.text = tr(title)
        backButton?.visibility = View.VISIBLE
        dashboardScrollView?.visibility = View.GONE
        channelsContainer?.visibility = View.GONE
        playerView?.visibility = View.GONE
        try { exoPlayer?.stop() } catch (_: Exception) {}

        val isLockoutActive = safetyManager?.isDrivingLockoutActive ?: false
        safetyBarrierView?.visibility = if (isLockoutActive) View.VISIBLE else View.GONE

        NowPlayingStore.update(title, "AutoLounge Web", true)

        val wv = ensureWebView()
        if (wv.url != url) {
            wv.visibility = View.INVISIBLE
            showWebLoading(title)
            wv.stopLoading()
            wv.onResume()
            wv.loadUrl(url)
        } else {
            hideWebLoading()
            wv.visibility = View.VISIBLE
            wv.onResume()
        }
        if (isLockoutActive) {
            wv.evaluateJavascript(
                "(function(){try{document.querySelectorAll('video').forEach(function(v){v.style.visibility='hidden';});}catch(e){}})();",
                null
            )
        }
    }

    fun openLiveTv(streamUrl: String, title: String, fromRemote: Boolean = false) {
        hideCountryPicker()
        CarPlayerHelper.dismissSettingsIfOpen(playerView)
        if (!AutoLoungeSyncBridge.isDirectMediaStream(streamUrl)) {
            openWeb(streamUrl, title, fromRemote)
            return
        }
        hideCustomVideo()
        hideWebLoading()
        currentMode = Mode.VIDEO
        titleTextView?.text = tr(title)
        backButton?.visibility = View.VISIBLE
        dashboardScrollView?.visibility = View.GONE
        channelsContainer?.visibility = View.GONE
        webView?.apply {
            visibility = View.GONE
            stopLoading()
            onPause()
            loadUrl("about:blank")
        }

        NowPlayingStore.update(title, tr("Canlı TV"), true)

        val isLockoutActive = safetyManager?.isDrivingLockoutActive ?: false
        safetyBarrierView?.visibility = if (isLockoutActive) View.VISIBLE else View.GONE

        playerView?.apply {
            visibility = View.VISIBLE
            exoPlayer?.apply {
                stop()
                clearMediaItems()
                // Strictly disable video track if driving lockout is active, ensuring zero video rendering while audio plays
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, isLockoutActive)
                    .build()
                playerView?.videoSurfaceView?.visibility = if (!isLockoutActive) View.VISIBLE else View.INVISIBLE
                playerRetryJob?.cancel()
                playerRetryCount = 0
                setMediaItem(
                    MediaItem.Builder().setUri(Uri.parse(streamUrl))
                        .liveIf(LivePlayerFactory.isLiveUrl(streamUrl)).build()
                )
                prepare()
                play()
            }
        }
    }

    /** Full-screen channels browser with regional filtering. */
    fun openChannels() {
        hideCustomVideo()
        hideWebLoading()
        currentMode = Mode.CHANNELS
        titleTextView?.text = tr("Tüm Kanallar")
        backButton?.visibility = View.VISIBLE
        dashboardScrollView?.visibility = View.GONE
        webView?.apply {
            visibility = View.GONE
            stopLoading()
            onPause()
            loadUrl("about:blank")
        }
        playerView?.apply { visibility = View.GONE; try { exoPlayer?.stop() } catch (_: Exception) {} }
        safetyBarrierView?.visibility = View.GONE

        val container = channelsContainer ?: buildChannelsBrowser(this).also {
            channelsContainer = it
            rootLayout?.addView(it, 1.coerceAtMost(rootLayout?.childCount ?: 1))
        }
        container.visibility = View.VISIBLE

        val targetCountry = channelsCountryCode ?: NewsChannelsRepository.detectUserCountry()
        populateChannels(targetCountry)

        // Load bundled channels strictly on Dispatchers.IO and refresh UI on completion
        scope.launch(Dispatchers.IO) {
            NewsChannelsRepository.initBundledChannels(applicationContext)
            withContext(Dispatchers.Main) {
                if (currentMode == Mode.CHANNELS) {
                    populateChannels(channelsCountryCode ?: targetCountry)
                }
            }
        }
    }

    private fun buildChannelsBrowser(ctx: Context): LinearLayout {
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply { topMargin = dp(46) }
            setBackgroundColor(Color.parseColor("#0A0B0E"))
            setPadding(dp(14), dp(6), dp(14), dp(4))
            visibility = View.GONE
        }

        // Satır 1: Kompakt Arama Çubuğu + Kanal Rozeti (Tek Satırda Birleşik)
        val searchRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(36)
            ).apply { bottomMargin = dp(6) }
        }

        val searchContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#14161F"))
                setStroke(dp(1), Color.parseColor("#262D3D"))
            }
            setPadding(dp(10), 0, dp(8), 0)
        }

        val searchIcon = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_ui_globe)
            setColorFilter(Color.parseColor("#64748B"))
            layoutParams = LinearLayout.LayoutParams(dp(15), dp(15)).apply { marginEnd = dp(6) }
        }
        searchContainer.addView(searchIcon)

        val clearBtn = TextView(ctx).apply {
            text = "✕"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 13f
            typeface = appBold
            setPadding(dp(6), dp(2), dp(6), dp(2))
            visibility = View.GONE
        }

        val searchInput = EditText(ctx).apply {
            hint = tr("Kanal veya kategori ara...")
            setHintTextColor(Color.parseColor("#64748B"))
            setTextColor(Color.WHITE)
            textSize = 12.5f
            typeface = appSemi
            background = null
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    channelsSearchQuery = s?.toString()?.trim() ?: ""
                    clearBtn.visibility = if (channelsSearchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                    searchDebounceJob?.cancel()
                    searchDebounceJob = scope.launch {
                        delay(180)
                        filterAndRenderChannels()
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        channelsSearchEditText = searchInput
        searchContainer.addView(searchInput)

        clearBtn.setOnClickListener {
            searchDebounceJob?.cancel()
            searchInput.setText("")
        }
        searchContainer.addView(clearBtn)
        searchRow.addView(searchContainer)

        // Sağ: Kanal Sayısı Rozeti
        val countBadge = TextView(ctx).apply {
            text = tr("● %d Kanal", 0)
            setTextColor(Color.parseColor("#10B981"))
            textSize = 11f
            typeface = appBold
            setPadding(dp(10), dp(4), dp(10), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(8) }
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(Color.parseColor("#142721"))
                setStroke(dp(1), Color.parseColor("#10B981"))
            }
        }
        channelsCountBadge = countBadge
        searchRow.addView(countBadge)

        root.addView(searchRow)

        // Satır 2: Tek Sıra Birleşik Yatay Filtre Çubuğu (Ülkeler + | + Kategoriler)
        val unifiedScroll = HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            isSmoothScrollingEnabled = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(6) }
        }

        val unifiedFilterRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val countryBtn = TextView(ctx).apply {
            val code = channelsCountryCode ?: NewsChannelsRepository.detectUserCountry()
            text = "${formatCountryChipLabel(code, "Türkiye")} ▾"
            setTextColor(Color.parseColor("#34D399"))
            textSize = 11.5f
            typeface = appBold
            letterSpacing = 0.02f
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = GradientDrawable().apply {
                cornerRadius = dp(9).toFloat()
                setColor(Color.parseColor("#12231C"))
                setStroke(dp(1), Color.parseColor("#10B981"))
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { showCountryPicker() }
        }
        countryButton = countryBtn
        unifiedFilterRow.addView(countryBtn, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { marginEnd = dp(6) })

        val divider = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(1), dp(18)).apply {
                marginEnd = dp(6)
            }
            setBackgroundColor(Color.parseColor("#2A3142"))
        }
        unifiedFilterRow.addView(divider)

        channelsCategoryChipsRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        unifiedFilterRow.addView(channelsCategoryChipsRow)

        unifiedScroll.addView(unifiedFilterRow)
        root.addView(unifiedScroll)

        // In-car smooth scrollable channel list (Genişletilmiş Alan)
        val listScroll = ScrollView(ctx).apply {
            isFillViewport = true
            isSmoothScrollingEnabled = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        channelsListColumn = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        listScroll.addView(channelsListColumn)
        root.addView(listScroll)

        return root
    }

    private fun formatCountryChipLabel(code: String, defaultName: String): String {
        val upper = code.uppercase(Locale.ROOT)
        val flag = when (upper) {
            "TR" -> "🇹🇷"
            "US" -> "🇺🇸"
            "GB", "UK" -> "🇬🇧"
            "DE" -> "🇩🇪"
            "AZ" -> "🇦🇿"
            "GLOBAL" -> "🌐"
            "UA" -> "🇺🇦"
            "FR" -> "🇫🇷"
            "IT" -> "🇮🇹"
            "ES" -> "🇪🇸"
            "RU" -> "🇷🇺"
            else -> "🏳️"
        }
        val shortName = when (upper) {
            "TR" -> tr("Türkiye")
            "US" -> tr("ABD")
            "GB", "UK" -> tr("İngiltere")
            "DE" -> tr("Almanya")
            "AZ" -> tr("Azerbaycan")
            "GLOBAL" -> tr("Global")
            "FR" -> tr("Fransa")
            "IT" -> tr("İtalya")
            "ES" -> tr("İspanya")
            else -> tr(defaultName)
        }
        return "$flag $shortName"
    }

    private fun populateChannels(countryCode: String) {
        channelsCountryCode = countryCode

        scope.launch(Dispatchers.IO) {
            val allSupported = NewsChannelsRepository.getSupportedCountries()
            val channels = NewsChannelsRepository.getChannelsForCountry(countryCode)

            val currentName = allSupported.firstOrNull {
                it.first.equals(countryCode, ignoreCase = true) ||
                (it.first.equals("GB", ignoreCase = true) && countryCode.equals("UK", ignoreCase = true))
            }?.second ?: "Türkiye"

            val cats = channels.map { it.category }.filter { it.isNotBlank() }.distinct()
            val counts = (listOf("Tümü") + cats).map { cat ->
                cat to if (cat == "Tümü") channels.size else channels.count { it.category.equals(cat, ignoreCase = true) }
            }

            withContext(Dispatchers.Main) {
                if (currentMode != Mode.CHANNELS || channelsCountryCode != countryCode) return@withContext
                currentCountryChannels = channels

                // Update country button label
                countryButton?.text = "${formatCountryChipLabel(countryCode, currentName)} ▾"

                // Update category chips and reload channel items
                populateCategoryChips(channels, counts)
                filterAndRenderChannels()
            }
        }
    }

    private fun populateCategoryChips(channels: List<NewsChannel>, precomputed: List<Pair<String, Int>>? = null) {
        val row = channelsCategoryChipsRow ?: return
        row.removeAllViews()

        val catCounts = precomputed ?: (listOf("Tümü") + channels.map { it.category }.filter { it.isNotBlank() }.distinct()).map { cat ->
            cat to if (cat == "Tümü") channels.size else channels.count { it.category.equals(cat, ignoreCase = true) }
        }
        val allCats = catCounts.map { it.first }

        if (!allCats.contains(channelsSelectedCategory)) {
            channelsSelectedCategory = "Tümü"
        }

        catCounts.forEach { (cat, count) ->
            val isSelected = cat == channelsSelectedCategory
            val chip = TextView(this).apply {
                text = "${if (cat == "Tümü") tr("Tümü") else tr(cat)} ($count)"
                setTextColor(if (isSelected) Color.parseColor("#041E15") else Color.parseColor("#94A3B8"))
                textSize = 11f
                typeface = if (isSelected) appBold else appSemi
                setPadding(dp(9), dp(4), dp(9), dp(4))
                background = GradientDrawable().apply {
                    cornerRadius = dp(9).toFloat()
                    setColor(if (isSelected) Color.parseColor("#38BDF8") else Color.parseColor("#151823"))
                    setStroke(dp(1), if (isSelected) Color.parseColor("#7DD3FC") else Color.parseColor("#242C3C"))
                }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    channelsSelectedCategory = cat
                    populateCategoryChips(channels, catCounts)
                    filterAndRenderChannels()
                }
            }
            row.addView(chip, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(5) })
        }
    }

    private fun filterAndRenderChannels() {
        val listCol = channelsListColumn ?: return
        listCol.removeAllViews()

        val query = channelsSearchQuery.trim()
        val filtered = currentCountryChannels.filter { ch ->
            val matchesCat = channelsSelectedCategory == "Tümü" || ch.category.equals(channelsSelectedCategory, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    ch.title.contains(query, ignoreCase = true) ||
                    ch.category.contains(query, ignoreCase = true)
            matchesCat && matchesQuery
        }

        channelsCountBadge?.text = tr("● %d Kanal", filtered.size)

        if (filtered.isEmpty()) {
            val emptyContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(36), dp(16), dp(36))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                addView(ImageView(this@AutoLoungeCarActivity).apply {
                    setImageResource(R.drawable.ic_aa_tv)
                    setColorFilter(Color.parseColor("#475569"))
                    layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply { bottomMargin = dp(12) }
                })
                addView(TextView(this@AutoLoungeCarActivity).apply {
                    text = tr("Eşleşen Kanal Bulunamadı")
                    setTextColor(Color.WHITE)
                    textSize = 15f
                    typeface = appBold
                    gravity = Gravity.CENTER
                })
                addView(TextView(this@AutoLoungeCarActivity).apply {
                    text = tr("Farklı bir arama terimi deneyin veya filtreyi sıfırlayın")
                    setTextColor(Color.parseColor("#8A94A6"))
                    textSize = 12f
                    typeface = appSemi
                    gravity = Gravity.CENTER
                    setPadding(0, dp(4), 0, dp(14))
                })
                addView(TextView(this@AutoLoungeCarActivity).apply {
                    text = tr("Filtreleri Sıfırla")
                    setTextColor(Color.parseColor("#10B981"))
                    textSize = 12.5f
                    typeface = appBold
                    setPadding(dp(14), dp(7), dp(14), dp(7))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(14).toFloat()
                        setColor(Color.parseColor("#142721"))
                        setStroke(dp(1), Color.parseColor("#10B981"))
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        channelsSearchQuery = ""
                        channelsSearchEditText?.setText("")
                        channelsSelectedCategory = "Tümü"
                        populateCategoryChips(currentCountryChannels)
                        filterAndRenderChannels()
                    }
                })
            }
            listCol.addView(emptyContainer)
            return
        }

        val maxItems = 50
        val displayList = if (filtered.size > maxItems) filtered.take(maxItems) else filtered
        displayList.forEach { ch ->
            listCol.addView(createChannelRow(ch))
        }

        if (filtered.size > maxItems) {
            val moreHint = TextView(this).apply {
                text = tr("+ %d kanal daha (belirli bir kanalı bulmak için yukarıdan arama yapın)", filtered.size - maxItems)
                setTextColor(Color.parseColor("#64748B"))
                textSize = 12f
                typeface = appRegular
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(12), dp(12), dp(16))
            }
            listCol.addView(moreHint)
        }
    }

    private fun createChannelRow(ch: NewsChannel): View {
        val ctx = this
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#141722"))
                setStroke(dp(1), Color.parseColor("#22293A"))
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(5) }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val stream = ch.audioStreamUrl ?: ch.url
                if (AutoLoungeSyncBridge.isDirectMediaStream(stream)) {
                    openLiveTv(stream, ch.title)
                } else {
                    openWeb(ch.url, ch.title)
                }
            }
        }

        // High-resolution channel logo with disk caching
        val logoBox = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38))
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(Color.parseColor("#F8FAFC"))
            }
        }
        val logoModel = NewsChannelsRepository.logoModelFor(ch)
        if (!logoModel.isNullOrBlank()) {
            logoBox.addView(ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                layoutParams = FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER)
                setPadding(dp(1), dp(1), dp(1), dp(1))
                load(logoModel) {
                    crossfade(true)
                    allowHardware(true)
                    memoryCachePolicy(CachePolicy.ENABLED)
                    diskCachePolicy(CachePolicy.ENABLED)
                    placeholder(R.drawable.ic_aa_tv)
                    error(R.drawable.ic_aa_tv)
                }
            })
        } else {
            logoBox.addView(TextView(ctx).apply {
                text = ch.title.firstOrNull()?.uppercase() ?: "T"
                setTextColor(Color.parseColor("#0F172A"))
                textSize = 15f
                typeface = appBold
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            })
        }
        row.addView(logoBox)

        // Channel metadata column
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(10)
                marginEnd = dp(6)
            }
        }
        col.addView(TextView(ctx).apply {
            text = ch.title
            setTextColor(Color.parseColor("#F8FAFC"))
            textSize = 13.5f
            typeface = appBold
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        })

        // Channel badges and tags
        val metaRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(2) }
        }

        // Kategori Rozeti (Category Badge)
        val catBadge = TextView(ctx).apply {
            text = tr(ch.category).uppercase(Locale.ROOT)
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 9f
            typeface = appBold
            setPadding(dp(5), dp(1), dp(5), dp(1))
            background = GradientDrawable().apply {
                cornerRadius = dp(5).toFloat()
                setColor(Color.parseColor("#1E293B"))
                setStroke(dp(1), Color.parseColor("#334155"))
            }
        }
        metaRow.addView(catBadge)

        val dot = TextView(ctx).apply {
            text = " • "
            setTextColor(Color.parseColor("#64748B"))
            textSize = 9.5f
        }
        metaRow.addView(dot)

        val sub = TextView(ctx).apply {
            text = "${tr(ch.countryName)} • ${tr("Canlı Yayın")}"
            setTextColor(Color.parseColor("#8A94A6"))
            textSize = 10.5f
            typeface = appSemi
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        metaRow.addView(sub)

        col.addView(metaRow)
        row.addView(col)

        // Direct play action button
        val playActionBox = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(30), dp(30))
            background = GradientDrawable().apply {
                cornerRadius = dp(15).toFloat()
                setColor(Color.parseColor("#10261E"))
                setStroke(dp(1), Color.parseColor("#10B981"))
            }
            addView(ImageView(ctx).apply {
                setImageResource(R.drawable.ic_ui_play)
                setColorFilter(Color.parseColor("#10B981"))
                layoutParams = FrameLayout.LayoutParams(dp(13), dp(13), Gravity.CENTER)
            })
        }
        row.addView(playActionBox)

        return row
    }

    private fun showCountryPicker() {
        var overlay = countryPickerOverlay
        if (overlay == null) {
            overlay = createCountryPickerOverlay()
            countryPickerOverlay = overlay
            rootLayout?.addView(overlay)
        }
        populateCountryPickerList()
        overlay.visibility = View.VISIBLE
    }

    private fun hideCountryPicker() {
        countryPickerOverlay?.visibility = View.GONE
    }

    private fun createCountryPickerOverlay(): FrameLayout {
        val ctx = this
        val overlay = FrameLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#B3000000"))
            isClickable = true
            isFocusable = true
            setOnClickListener { hideCountryPicker() }
            visibility = View.GONE
        }

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val lp = FrameLayout.LayoutParams(dp(440), dp(290)).apply {
                gravity = Gravity.CENTER
            }
            layoutParams = lp
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.parseColor("#11141D"))
                setStroke(dp(1), Color.parseColor("#293245"))
            }
            setPadding(dp(16), dp(14), dp(16), dp(14))
            isClickable = true
            isFocusable = true
        }

        // Header row
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        }

        val titleCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val titleText = TextView(ctx).apply {
            text = tr("🌍 Yayın Ülkesi Değiştir")
            setTextColor(Color.parseColor("#F8FAFC"))
            textSize = 14f
            typeface = appBold
        }
        val subText = TextView(ctx).apply {
            text = tr("Kanal listesini görüntülemek istediğiniz ülkeyi seçin")
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 10.5f
            typeface = appRegular
        }
        titleCol.addView(titleText)
        titleCol.addView(subText)
        header.addView(titleCol)

        val closeBtn = TextView(ctx).apply {
            text = "✕"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 14f
            typeface = appBold
            gravity = Gravity.CENTER
            val s = dp(28)
            layoutParams = LinearLayout.LayoutParams(s, s)
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Color.parseColor("#1C2333"))
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { hideCountryPicker() }
        }
        header.addView(closeBtn)
        card.addView(header)

        // Scrollable country list
        val scroll = ScrollView(ctx).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        val countryListCol = LinearLayout(ctx).apply {
            tag = "country_list_container"
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(countryListCol)
        card.addView(scroll)

        overlay.addView(card)
        return overlay
    }

    private fun populateCountryPickerList() {
        val overlay = countryPickerOverlay ?: return
        val container = overlay.findViewWithTag<LinearLayout>("country_list_container") ?: return
        container.removeAllViews()

        val allSupported = NewsChannelsRepository.getSupportedCountries()
        val priorityOrder = listOf("TR", "US", "GB", "DE", "AZ", "GLOBAL", "FR", "IT", "ES")
        val sortedCountries = allSupported.sortedWith(Comparator { a, b ->
            val aCode = if (a.first.equals("UK", true)) "GB" else a.first.uppercase(Locale.ROOT)
            val bCode = if (b.first.equals("UK", true)) "GB" else b.first.uppercase(Locale.ROOT)
            val aIdx = priorityOrder.indexOf(aCode)
            val bIdx = priorityOrder.indexOf(bCode)
            if (aIdx != -1 && bIdx != -1) aIdx.compareTo(bIdx)
            else if (aIdx != -1) -1
            else if (bIdx != -1) 1
            else a.second.compareTo(b.second)
        })

        val currentCode = channelsCountryCode ?: NewsChannelsRepository.detectUserCountry()

        sortedCountries.forEach { (code, name) ->
            val isSelected = code.equals(currentCode, ignoreCase = true) ||
                    (code.equals("GB", ignoreCase = true) && currentCode.equals("UK", ignoreCase = true))

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(44)
                ).apply { bottomMargin = dp(6) }
                setPadding(dp(12), dp(6), dp(12), dp(6))
                background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(if (isSelected) Color.parseColor("#142B22") else Color.parseColor("#181D29"))
                    setStroke(dp(1), if (isSelected) Color.parseColor("#10B981") else Color.parseColor("#262E3E"))
                }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    hideCountryPicker()
                    NewsChannelsRepository.saveSelectedCountry(code)
                    populateChannels(code)
                }
            }

            val flagLabel = formatCountryChipLabel(code, name)
            val nameView = TextView(this).apply {
                text = flagLabel
                setTextColor(if (isSelected) Color.parseColor("#34D399") else Color.parseColor("#F1F5F9"))
                textSize = 13f
                typeface = if (isSelected) appBold else appSemi
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(nameView)

            if (isSelected) {
                val checkView = TextView(this).apply {
                    text = tr("✓ Seçili")
                    setTextColor(Color.parseColor("#34D399"))
                    textSize = 11f
                    typeface = appBold
                }
                row.addView(checkView)
            }

            container.addView(row)
        }
    }

    fun showDashboard(fromRemote: Boolean = false) {
        hideCountryPicker()
        CarPlayerHelper.dismissSettingsIfOpen(playerView)
        hideCustomVideo()
        hideWebLoading()
        currentMode = Mode.DASHBOARD
        titleTextView?.text = tr("AutoLounge Kokpit")
        backButton?.visibility = View.GONE
        dashboardScrollView?.visibility = View.VISIBLE
        channelsContainer?.visibility = View.GONE

        webView?.apply {
            visibility = View.GONE
            stopLoading()
            onPause()
            loadUrl("about:blank")
            clearHistory()
        }

        playerView?.apply {
            visibility = View.GONE
            try { exoPlayer?.stop() } catch (_: Exception) {}
        }

        safetyBarrierView?.visibility = View.GONE
        NowPlayingStore.clear()
    }

    override fun onBackPressed() {
        if (countryPickerOverlay?.visibility == View.VISIBLE) {
            hideCountryPicker()
            return
        }
        if (CarPlayerHelper.dismissSettingsIfOpen(playerView)) {
            return
        }
        if (customVideoView != null) {
            hideCustomVideo()
        } else if (currentMode == Mode.WEB && webView?.canGoBack() == true) {
            webView?.goBack()
        } else if (currentMode != Mode.DASHBOARD) {
            showDashboard()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        safetyManager?.refreshMonitoring()
        if (currentMode == Mode.WEB) {
            try { webView?.onResume() } catch (_: Exception) {}
        } else if (currentMode == Mode.VIDEO) {
            try {
                if (exoPlayer?.playbackState == Player.STATE_IDLE || exoPlayer?.playWhenReady == false) {
                    exoPlayer?.prepare()
                    exoPlayer?.play()
                }
            } catch (_: Exception) {}
        }
    }

    override fun onPause() {
        super.onPause()
        try { webView?.onPause() } catch (_: Exception) {}
    }

    override fun onStop() {
        super.onStop()
        hideCustomVideo()
        try { exoPlayer?.stop() } catch (_: Exception) {}
        try { webView?.onPause() } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        AutoLoungeSyncBridge.setCarAppAlive(false)
        searchDebounceJob?.cancel()
        scope.cancel()
        hideCustomVideo()
        try { exoPlayer?.release() } catch (_: Exception) {}
        exoPlayer = null
        webView?.let { wv ->
            try {
                wv.stopLoading()
                wv.onPause()
                wv.webChromeClient = null
                wv.webViewClient = WebViewClient()
                wv.loadUrl("about:blank")
                wv.clearHistory()
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.removeAllViews()
                wv.destroy()
            } catch (e: Exception) {
                Log.w(TAG, "WebView cleanup error: ${e.message}")
            }
        }
        webView = null
        fullscreenContainer?.removeAllViews()
        fullscreenContainer = null
    }

    private fun dp(value: Int): Int {
        val density = resources?.displayMetrics?.density ?: 1f
        return (value * density).toInt()
    }
}
