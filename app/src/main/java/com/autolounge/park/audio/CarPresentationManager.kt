package com.autohub.park.audio

import android.app.Presentation
import android.content.Context
import android.graphics.Color
import com.autohub.park.i18n.tr
import android.graphics.Point
import android.graphics.Typeface
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.autohub.park.BuildConfig
import com.autohub.park.R
import com.autohub.park.car.CarPlayerHelper
import com.autohub.park.data.ParkSafetyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CarPresentationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safetyManager: ParkSafetyManager
) {
    private val TAG = "CarPresentationManager"
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var activePresentation: CarVideoPresentation? = null
    private var currentMediaUrl: String? = null
    private var currentTitle: String = "AutoLounge Kokpit"
    private var isWebMode: Boolean = false
    private var audioOnlyMode: Boolean = false

    init {
        displayManager?.registerDisplayListener(object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "onDisplayAdded: $displayId")
                }
                checkAndAttachToCarDisplay()
            }

            override fun onDisplayRemoved(displayId: Int) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "onDisplayRemoved: $displayId")
                }
                if (activePresentation?.display?.displayId == displayId) {
                    dismissPresentation()
                }
            }

            override fun onDisplayChanged(displayId: Int) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "onDisplayChanged: $displayId")
                }
                if (activePresentation?.display?.displayId == displayId) {
                    val display = displayManager.getDisplay(displayId)
                    if (display == null || !display.isValid) {
                        dismissPresentation()
                    }
                }
            }
        }, null)
        checkAndAttachToCarDisplay()
    }

    /**
     * AA ses yolu için: ikinci ExoPlayer BAŞLATMAZ.
     * Sadece gerçek ikincil ekran (HDMI/Automotive) varsa başlığı günceller.
     * Araç sesi her zaman ParkAudioService tek ExoPlayer'ından gelir (çift ses engeli).
     */
    fun showAudioOnly(title: String) {
        currentTitle = title
        audioOnlyMode = true
        currentMediaUrl = null
        val display = findCarDisplay()
        if (display != null) {
            attachAudioOnly(display, title)
        }
        // AA projection'da ikincil display yoksa hiçbir şey yapma (ses zaten servisten geliyor).
    }

    fun showOnCarScreen(mediaUrl: String, title: String, isWeb: Boolean = false) {
        currentMediaUrl = mediaUrl
        currentTitle = title
        isWebMode = isWeb
        audioOnlyMode = false
        checkAndAttachToCarDisplay()
    }

    fun dismissPresentation() {
        try {
            activePresentation?.dismiss()
        } catch (e: Exception) {
            Log.w(TAG, "Error dismissing presentation: ${e.message}")
        } finally {
            activePresentation = null
        }
    }

    fun hasSecondaryDisplay(): Boolean = findCarDisplay() != null

    private fun findCarDisplay(): Display? {
        val allDisplays = displayManager?.displays ?: emptyArray()
        val presCategoryDisplays = displayManager?.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION) ?: emptyArray()

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "ALL RAW DISPLAYS (${allDisplays.size}): ${allDisplays.map { "${it.name} (id=${it.displayId}, flags=${it.flags}, valid=${it.isValid})" }}")
            Log.d(TAG, "PRESENTATION CATEGORY DISPLAYS (${presCategoryDisplays.size}): ${presCategoryDisplays.map { "${it.name} (id=${it.displayId})" }}")
        }

        // Maps veya facetbar gibi uygunsuz servis ekranlarını hariç tut, gearhead ekranlarına izin ver
        val validDisplays = allDisplays.filter { display ->
            val name = display.name.lowercase()
            display.displayId != Display.DEFAULT_DISPLAY &&
                display.isValid &&
                !name.contains("maps") &&
                !name.contains("templatecarfragment") &&
                !name.contains("carappservice") &&
                !name.contains("ghfacetbar")
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Eligible displays for presentation: ${validDisplays.map { "${it.name} (id=${it.displayId})" }}")
        }

        // 1. Öncelik: Gerçek Android Auto Gearhead Projeksiyon / Medya Ekranı
        return validDisplays.firstOrNull { it.name.contains("gearhead", ignoreCase = true) }
            // 2. Öncelik: Gerçek Android Auto / DHU Ana Projeksiyon Ekranı (lon)
            ?: validDisplays.firstOrNull { it.name.contains("lon", ignoreCase = true) }
            // 3. Öncelik: Presentation kategorisindeki ekranlar
            ?: presCategoryDisplays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY && !it.name.contains("maps", ignoreCase = true) }
            // 4. Öncelik: FLAG_PRESENTATION bayraklı geniş ekran
            ?: validDisplays.firstOrNull { (it.flags and Display.FLAG_PRESENTATION) != 0 && getDisplayWidth(it) >= 600 }
            // 5. Öncelik: FLAG_PRESENTATION bayraklı herhangi bir ikincil ekran
            ?: validDisplays.firstOrNull { (it.flags and Display.FLAG_PRESENTATION) != 0 }
            // 6. Öncelik: Kalan geçerli ilk ekran
            ?: validDisplays.firstOrNull()
    }

    @Suppress("DEPRECATION")
    private fun getDisplayWidth(display: Display): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display.mode?.physicalWidth ?: run {
                val point = Point()
                display.getRealSize(point)
                point.x
            }
        } else {
            val point = Point()
            display.getRealSize(point)
            point.x
        }
    }

    private fun checkAndAttachToCarDisplay() {
        val carDisplay = findCarDisplay()
        if (carDisplay != null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Target car display found: ${carDisplay.name} (${carDisplay.displayId})")
            }
            if (audioOnlyMode) {
                attachAudioOnly(carDisplay, currentTitle)
            } else {
                attachPresentation(carDisplay)
            }
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "No valid car display found at this moment.")
            }
        }
    }

    private fun attachAudioOnly(display: Display, title: String) {
        try {
            if (activePresentation?.display?.displayId != display.displayId ||
                activePresentation?.isShowing != true ||
                activePresentation?.display?.isValid != true) {
                dismissPresentation()
                val pres = CarVideoPresentation(
                    context = context,
                    display = display,
                    safetyManager = safetyManager
                )
                pres.window?.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
                pres.show()
                activePresentation = pres
            }
            activePresentation?.showAudioOnly(title)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach audio-only: ${e.message}", e)
            dismissPresentation()
        }
    }

    private fun attachPresentation(display: Display) {
        try {
            if (activePresentation?.display?.displayId != display.displayId ||
                activePresentation?.isShowing != true ||
                activePresentation?.display?.isValid != true) {
                dismissPresentation()
                val pres = CarVideoPresentation(
                    context = context,
                    display = display,
                    safetyManager = safetyManager
                )
                pres.window?.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
                pres.show()
                activePresentation = pres
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "CarVideoPresentation shown on display ${display.displayId} (${display.name})")
                }
            }

            if (audioOnlyMode) {
                activePresentation?.showAudioOnly(currentTitle)
                return
            }

            val url = currentMediaUrl ?: return
            if (isWebMode) {
                activePresentation?.loadWeb(url, currentTitle)
            } else {
                activePresentation?.playVideoMuted(url, currentTitle)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach presentation: ${e.message}", e)
            dismissPresentation()
        }
    }
}

/**
 * Gerçek harici ekranda video gösterir.
 * KRİTİK: Presentation ExoPlayer SESSİZDİR (volume=0).
 * Ses her zaman ParkAudioService tek player'ından gelir -> çift ses olmaz.
 * Park: video açık. Sürüş: bariyer + video gizli, servis sesi devam eder.
 */
class CarVideoPresentation(
    context: Context,
    display: Display,
    private val safetyManager: ParkSafetyManager
) : Presentation(context, display) {

    private val TAG = "CarVideoPresentation"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var exoPlayer: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var webView: WebView? = null
    private var safetyBarrierView: LinearLayout? = null
    private var audioOnlyView: LinearLayout? = null
    private var audioOnlyTitle: TextView? = null
    private var titleTextView: TextView? = null
    private var parkBadge: TextView? = null

    private var pendingUrl: String? = null
    private var pendingTitle: String? = null
    private var pendingIsWeb: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = object : FrameLayout(context) {
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
            setBackgroundColor(Color.BLACK)
        }

        // 1. Video katmanı (SESSİZ - ses servisten gelir)
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

        val player = ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build().apply {
                volume = 0f
            }
        exoPlayer = player
        // TextureView: ikincil/projeksiyon ekranlarında SurfaceView yansımayabilir,
        // bu yüzden görüntünün garanti gelmesi için texture_view kullanılır.
        val pView = LayoutInflater.from(context).inflate(R.layout.view_car_player, null) as PlayerView
        pView.apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            useController = true
            this.player = player
            setBackgroundColor(Color.BLACK)
        }
        playerView = pView
        CarPlayerHelper.setupDismissableSettings(pView, CoroutineScope(SupervisorJob() + Dispatchers.Main))
        rootLayout.addView(pView)

        // 2. WebView katmanı (YouTube & Web için tam ekran, URL barsız)
        val wView = WebView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true
                loadWithOverviewMode = true
                offscreenPreRaster = true
                allowFileAccess = false
                allowContentAccess = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
            }
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    // Reklam/izleyici istekleri ağa çıkmadan kapatılır (ParkBrowser ile aynı engelleyici)
                    if (request != null && !request.isForMainFrame &&
                        com.autohub.park.ui.browser.WebContentBlocker.isBlocked(request.url)
                    ) {
                        return com.autohub.park.ui.browser.WebContentBlocker.emptyResponse()
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    super.onPageCommitVisible(view, url)
                    view?.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (com.autohub.park.ui.browser.WebContentBlocker.enabled.value) {
                        view?.evaluateJavascript(com.autohub.park.ui.browser.WebContentBlocker.COSMETIC_JS, null)
                    }
                    view?.visibility = View.VISIBLE
                }
            }
            webChromeClient = android.webkit.WebChromeClient()
            visibility = View.GONE
            setBackgroundColor(Color.BLACK)
        }
        webView = wView
        rootLayout.addView(wView)

        val appBold = ResourcesCompat.getFont(context, R.font.manrope_bold) ?: Typeface.DEFAULT_BOLD
        val appRegular = ResourcesCompat.getFont(context, R.font.manrope_regular) ?: Typeface.DEFAULT

        // 3. Ses modu bilgi katmanı (AA sesi çalarken harici ekranda gösterilir)
        val audioView = LinearLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#FF0A0B0E"))
            visibility = View.GONE

            val icon = ImageView(context).apply {
                setImageResource(R.drawable.ic_aa_radio_tile)
                setColorFilter(Color.parseColor("#10B981"))
                layoutParams = LinearLayout.LayoutParams(96, 96).apply { bottomMargin = 24 }
            }
            addView(icon)

            val t = TextView(context).apply {
                text = "AutoLounge"
                setTextColor(Color.WHITE)
                textSize = 20f
                typeface = appBold
                letterSpacing = 0.01f
                gravity = Gravity.CENTER
            }
            audioOnlyTitle = t
            addView(t)

            val sub = TextView(context).apply {
                text = tr("Ses araç hoparlöründe çalıyor\nVideo park halinde telefonda / bu ekranda")
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 14f
                typeface = appRegular
                letterSpacing = 0.01f
                gravity = Gravity.CENTER
                setPadding(32, 16, 32, 0)
            }
            addView(sub)
        }
        audioOnlyView = audioView
        rootLayout.addView(audioView)

        // 4. Sürüş bariyeri
        val barrier = LinearLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#FF0A0B0E"))
            visibility = View.GONE

            val shieldIcon = ImageView(context).apply {
                setImageResource(R.drawable.ic_aa_tv)
                setColorFilter(Color.parseColor("#38BDF8"))
                layoutParams = LinearLayout.LayoutParams(96, 96).apply { bottomMargin = 24 }
            }
            addView(shieldIcon)

            val warningTitle = TextView(context).apply {
                text = tr("Sürüş Güvenliği Modu")
                setTextColor(Color.WHITE)
                textSize = 20f
                typeface = appBold
                letterSpacing = 0.01f
                gravity = Gravity.CENTER
            }
            addView(warningTitle)

            val warningSub = TextView(context).apply {
                text = tr("Araç hareket halindeyken video güvenlik gereği gizlenir.\nSes yayını kesintisiz devam ediyor.")
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 14f
                typeface = appRegular
                letterSpacing = 0.015f
                gravity = Gravity.CENTER
                setPadding(32, 16, 32, 0)
            }
            addView(warningSub)
        }
        safetyBarrierView = barrier
        rootLayout.addView(barrier)

        // 5. Üst bar
        val topBar = LinearLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.TOP }
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 16, 24, 16)
            setBackgroundColor(Color.parseColor("#990B0C10"))

            val title = TextView(context).apply {
                text = tr("AutoLounge Kokpit")
                setTextColor(Color.WHITE)
                textSize = 15f
                typeface = appBold
                letterSpacing = 0.01f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            titleTextView = title
            addView(title)

            val badge = TextView(context).apply {
                text = tr("● PARK MODU")
                setTextColor(Color.parseColor("#10B981"))
                textSize = 12f
                typeface = appBold
                letterSpacing = 0.05f
            }
            parkBadge = badge
            addView(badge)
        }
        rootLayout.addView(topBar)

        setContentView(rootLayout)

        if (pendingUrl != null) {
            val pUrl = pendingUrl!!
            val pTitle = pendingTitle ?: ""
            if (pendingIsWeb) {
                loadWeb(pUrl, pTitle)
            } else {
                playVideoMuted(pUrl, pTitle)
            }
            pendingUrl = null
        }

        scope.launch {
            safetyManager.isParked.collectLatest { isParked ->
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Safety: isParked=$isParked")
                }
                safetyBarrierView?.visibility = if (isParked) View.GONE else View.VISIBLE
                parkBadge?.text = if (isParked) tr("● PARK MODU") else tr("● SÜRÜŞ MODU (SESLİ)")
                parkBadge?.setTextColor(
                    if (isParked) Color.parseColor("#10B981") else Color.parseColor("#F59E0B")
                )
                // Sürüşte presentation videosunu duraklat (servis sesi devam eder)
                if (!isParked) {
                    try { webView?.onPause() } catch (_: Exception) {}
                } else if (webView?.visibility == View.VISIBLE) {
                    try { webView?.onResume() } catch (_: Exception) {}
                }
            }
        }
    }

    /** Sessiz video (ses servisten gelir, çift ses olmaz). */
    fun playVideoMuted(streamUrl: String, title: String) {
        if (playerView == null || exoPlayer == null) {
            pendingUrl = streamUrl
            pendingTitle = title
            pendingIsWeb = false
            return
        }
        titleTextView?.text = title
        audioOnlyView?.visibility = View.GONE
        webView?.apply {
            visibility = View.GONE
            stopLoading()
            onPause()
            loadUrl("about:blank")
        }
        playerView?.visibility = View.VISIBLE
        exoPlayer?.let { player ->
            player.volume = 0f
            player.setMediaItem(MediaItem.fromUri(Uri.parse(streamUrl)))
            player.prepare()
            player.play()
        }
    }

    // Eski isimle uyumluluk: sessize yönlendir
    fun playVideo(streamUrl: String, title: String) = playVideoMuted(streamUrl, title)

    fun loadWeb(url: String, title: String) {
        if (webView == null) {
            pendingUrl = url
            pendingTitle = title
            pendingIsWeb = true
            return
        }
        titleTextView?.text = title
        audioOnlyView?.visibility = View.GONE
        playerView?.visibility = View.GONE
        try { exoPlayer?.stop() } catch (_: Exception) {}
        webView?.apply {
            if (this.url != url) {
                visibility = View.INVISIBLE
            }
            onResume()
            loadUrl(url)
        }
    }

    fun showAudioOnly(title: String) {
        titleTextView?.text = title
        audioOnlyTitle?.text = title
        try { exoPlayer?.stop() } catch (_: Exception) {}
        webView?.apply {
            visibility = View.GONE
            stopLoading()
            onPause()
            loadUrl("about:blank")
        }
        playerView?.visibility = View.GONE
        audioOnlyView?.visibility = View.VISIBLE
    }

    override fun onStop() {
        super.onStop()
        scope.cancel()
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
    }

    override fun dismiss() {
        scope.cancel()
        try { exoPlayer?.stop() } catch (_: Exception) {}
        try { exoPlayer?.release() } catch (_: Exception) {}
        exoPlayer = null
        try {
            webView?.stopLoading()
            webView?.onPause()
            (webView?.parent as? ViewGroup)?.removeView(webView)
            webView?.removeAllViews()
            webView?.destroy()
        } catch (_: Exception) {}
        webView = null
        try {
            super.dismiss()
        } catch (e: Exception) {
            Log.w(TAG, "super.dismiss error: ${e.message}")
        }
    }
}
