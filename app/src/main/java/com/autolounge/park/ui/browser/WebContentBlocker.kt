package com.autohub.park.ui.browser

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

/**
 * Reklam engelleyici (iki katman):
 *  1. Ağ: reklam/izleyici alan adlarına giden istekler hiç ağa çıkmadan boş yanıtla kapatılır.
 *     Liste: assets/adblock/domains.txt (StevenBlack/hosts, MIT, ~70 bin alan adı).
 *  2. Görsel: sayfada kalan boş reklam kutuları ve sponsorlu kartlar CSS ile gizlenir.
 */
object WebContentBlocker {

    private const val PREFS = "autolounge_adblock"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_TOTAL_BLOCKED = "total_blocked"

    /** Liste yüklenmeden önce de çalışan çekirdek reklam ağları. */
    private val coreHosts = setOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
        "google-analytics.com", "googletagservices.com", "adnxs.com", "criteo.com", "criteo.net",
        "taboola.com", "outbrain.com", "scorecardresearch.com", "amazon-adsystem.com", "adsrvr.org",
        "rubiconproject.com", "pubmatic.com", "openx.net", "casalemedia.com", "moatads.com",
        "popads.net", "propellerads.com", "adform.net", "smartadserver.com", "teads.tv"
    )

    /** Listede olsa bile asla engellenmez: video oynatma ve giriş için gerekli. */
    private val neverBlockSuffixes = setOf(
        "googlevideo.com", "ytimg.com", "youtube.com", "youtube-nocookie.com",
        "accounts.google.com", "www.google.com", "fonts.gstatic.com", "www.gstatic.com"
    )

    @Volatile private var listHosts: Set<String> = emptySet()
    @Volatile private var appContext: Context? = null

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _totalBlocked = MutableStateFlow(0L)
    /** Toplam engellenen istek sayısı (ayarlarda gösterilir). */
    val totalBlocked: StateFlow<Long> = _totalBlocked.asStateFlow()
    private val unsavedBlocked = java.util.concurrent.atomic.AtomicInteger(0)

    private val emptyResponseBody = ByteArray(0)

    /** Uygulama açılışında bir kez çağrılır; liste arka planda yüklenir. */
    fun init(context: Context) {
        if (appContext != null) return
        val ctx = context.applicationContext
        appContext = ctx
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _enabled.value = prefs.getBoolean(KEY_ENABLED, true)
        _totalBlocked.value = prefs.getLong(KEY_TOTAL_BLOCKED, 0L)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            listHosts = runCatching {
                ctx.assets.open("adblock/domains.txt").bufferedReader().useLines { lines ->
                    lines.filter { it.isNotBlank() && !it.startsWith("#") }.map { it.trim().lowercase() }.toHashSet()
                }
            }.getOrDefault(emptySet())
        }
    }

    fun setEnabled(context: Context, value: Boolean) {
        _enabled.value = value
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun isBlocked(uri: Uri?): Boolean {
        if (!_enabled.value) return false
        val host = uri?.host?.lowercase() ?: return false
        if (neverBlockSuffixes.any { host == it || host.endsWith(".$it") }) {
            // youtube.com altındaki reklam alt alan adları (ör. ads.youtube.com) yine engellenir
            return host in listHosts
        }
        // Alan adını ve tüm üst alan adlarını kontrol et: a.b.example.com → b.example.com → example.com
        var candidate = host
        while (true) {
            if (candidate in coreHosts || candidate in listHosts) {
                countBlocked()
                return true
            }
            val dot = candidate.indexOf('.')
            if (dot < 0 || candidate.indexOf('.', dot + 1) < 0) return false
            candidate = candidate.substring(dot + 1)
        }
    }

    private fun countBlocked() {
        _totalBlocked.update { it + 1 }
        // Diske her istekte değil, 50 istekte bir yaz
        if (unsavedBlocked.incrementAndGet() >= 50) {
            unsavedBlocked.set(0)
            appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                ?.edit()?.putLong(KEY_TOTAL_BLOCKED, _totalBlocked.value)?.apply()
        }
    }

    fun emptyResponse(): WebResourceResponse =
        WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(emptyResponseBody))

    /** Engellenen reklamlardan geriye kalan boş kutuları ve sponsorlu içerikleri gizler. */
    const val COSMETIC_JS = """
(function() {
  if (document.getElementById('al-adblock-css')) return;
  var s = document.createElement('style');
  s.id = 'al-adblock-css';
  s.textContent = [
    'ins.adsbygoogle', '[id^="google_ads_"]', '[id^="div-gpt-ad"]', '[data-google-query-id]',
    'iframe[src*="doubleclick.net"]', 'iframe[src*="googlesyndication"]', 'iframe[id^="google_ads_iframe"]',
    '.adsbygoogle', '.ad-container', '.ad-wrapper', '.ad-slot', '.ad-banner', '.advertisement',
    '[class*="sponsored-content"]', '.taboola', '[id^="taboola-"]', '.OUTBRAIN', '[data-widget-id^="outbrain"]',
    'ytm-promoted-sparkles-web-renderer', 'ytm-promoted-video-renderer', 'ytm-companion-ad-renderer',
    'ytm-companion-slot', 'ad-slot-renderer', 'ytm-ad-slot-renderer', 'ytd-ad-slot-renderer',
    'ytm-statement-banner-renderer', 'ytm-mealbar-promo-renderer', 'ytd-banner-promo-renderer'
  ].join(',') + '{display:none !important;}';
  (document.head || document.documentElement).appendChild(s);
})();
"""
}
