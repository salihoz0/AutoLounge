package com.autolounge.park.ui.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.autolounge.park.i18n.tr
import com.autolounge.park.ui.AutoLoungePalette

private const val MOBILE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

internal const val CONSENT_AUTO_ACCEPT_JS = """
(function() {
  if (window.__autolounge_consent_accepted) return 'already_accepted';
  try {
    var sels = [
      'button[jsname="b3VHJd"]',
      'form[action*="consent"] button',
      '#onetrust-accept-btn-handler',
      '#accept-all-button',
      '[data-testid="uc-accept-all-button"]',
      '.qc-cmp2-summary-buttons button[mode="primary"]',
      'button[aria-label*="Accept all" i]',
      'button[aria-label*="Tümünü kabul et" i]',
      'button[aria-label*="Accept the use" i]',
      'button[title*="Accept all" i]',
      '.cc-btn.cc-allow',
      '#truste-consent-button',
      'ytm-consent-bump-v2-renderer button',
      'ytd-consent-bump-v2-lightbox button'
    ];
    for (var i = 0; i < sels.length; i++) {
      var el = document.querySelector(sels[i]);
      if (el && typeof el.click === 'function') {
        el.click();
        window.__autolounge_consent_accepted = true;
        return 'sel:' + sels[i];
      }
    }
    var dialogs = document.querySelectorAll('[role="dialog"], [aria-modal="true"], [class*="consent" i], [class*="cookie" i], [id*="consent" i], [id*="cookie" i]');
    var container = dialogs.length > 0 ? dialogs[0] : document;
    if (container) {
      var btns = container.querySelectorAll('button, [role="button"]');
      var n = Math.min(btns.length, 30);
      for (var j = 0; j < n; j++) {
        var b = btns[j];
        if (b.closest && (b.closest('.html5-video-player') || b.closest('#movie_player') || b.closest('video') || b.closest('nav') || b.closest('header'))) {
          continue;
        }
        var t = (b.textContent || '').trim().toLowerCase();
        if (/^(tamam|kabul|kabul et|tümünü kabul et|hepsini kabul et|accept|accept all|i agree|agree|ok|onayla)$/.test(t)) {
          b.click();
          window.__autolounge_consent_accepted = true;
          return 'txt:' + t;
        }
      }
    }
  } catch (e) {}
  return null;
})();
"""

internal const val SPA_SYNC_JS = """
(function() {
  if (window._autoLoungeBridgeInjected) {
    if (typeof window._autoLoungeNotify === 'function') {
      window._autoLoungeNotify();
    }
    return;
  }
  window._autoLoungeBridgeInjected = true;

  var lastReportedUrl = window.location.href;
  var lastReportedTitle = document.title || '';

  function getBestTitle() {
    var title = document.title || '';
    if (!title || title.trim().toLowerCase() === 'youtube') {
      var h1 = document.querySelector('h1.slim-video-information-title, .slim-video-metadata-title, ytm-slim-owner-renderer h2, .yt-core-attributed-string, [class*="video-title"]');
      if (h1 && h1.textContent && h1.textContent.trim()) {
        title = h1.textContent.trim();
      }
    }
    return title;
  }

  function notifyUrl() {
    try {
      var current = window.location.href;
      var title = getBestTitle();
      if (window.AutoLoungeBridge && typeof window.AutoLoungeBridge.onSpaUrlChanged === 'function') {
        window.AutoLoungeBridge.onSpaUrlChanged(current, title);
        lastReportedUrl = current;
        lastReportedTitle = title;
      }
    } catch (e) {}
  }
  window._autoLoungeNotify = notifyUrl;

  var origPushState = history.pushState;
  if (origPushState) {
    history.pushState = function() {
      var ret = origPushState.apply(this, arguments);
      notifyUrl();
      setTimeout(notifyUrl, 300);
      setTimeout(notifyUrl, 1000);
      return ret;
    };
  }

  var origReplaceState = history.replaceState;
  if (origReplaceState) {
    history.replaceState = function() {
      var ret = origReplaceState.apply(this, arguments);
      notifyUrl();
      setTimeout(notifyUrl, 300);
      setTimeout(notifyUrl, 1000);
      return ret;
    };
  }

  window.addEventListener('popstate', notifyUrl);
  window.addEventListener('hashchange', notifyUrl);
  window.addEventListener('yt-navigate-finish', function() {
    notifyUrl();
    setTimeout(notifyUrl, 300);
    setTimeout(notifyUrl, 1000);
  });
  window.addEventListener('yt-page-data-updated', notifyUrl);
  document.addEventListener('yt-navigate-finish', notifyUrl);
  document.addEventListener('yt-page-data-updated', notifyUrl);

  try {
    var titleEl = document.querySelector('title');
    if (titleEl && window.MutationObserver) {
      new MutationObserver(function() {
        notifyUrl();
      }).observe(titleEl, { subtree: true, characterData: true, childList: true });
    }
  } catch (e) {}

  setInterval(function() {
    var cur = window.location.href;
    var curTitle = getBestTitle();
    if (cur !== lastReportedUrl || curTitle !== lastReportedTitle) {
      notifyUrl();
    }
  }, 500);

  notifyUrl();
})();
"""

/**
 * Yatay/alçak ekranlarda (araç) YouTube mobil oynatıcısını görünür yüksekliğe sığdırır;
 * aksi halde 16:9 oynatıcı ekrandan taşar ve aşağı kaydırmak gerekir. 48px = YouTube üst barı.
 */
internal const val YOUTUBE_LANDSCAPE_FIT_JS = """
(function() {
  if (document.getElementById('al-yt-fit')) return;
  var s = document.createElement('style');
  s.id = 'al-yt-fit';
  s.textContent = '@media (orientation: landscape) {' +
    '.player-size { padding-bottom: 0 !important; height: calc(100vh - 48px) !important; max-width: 100vw !important; }' +
    '#player-container-id, .player-container { height: calc(100vh - 48px) !important; }' +
    '#movie_player video { width: 100% !important; height: calc(100vh - 48px) !important; left: 0 !important; top: 0 !important; object-fit: contain !important; }' +
    '}';
  (document.head || document.documentElement).appendChild(s);
})();
"""

private fun hasMicPermission(context: Context): Boolean =
    androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

/** Telefondan gelen metni sayfadaki odaklı (yoksa ilk) yazı alanına ekler; React/Vue alanlarıyla da çalışır. */
internal fun remoteTextInjectionJs(text: String): String {
    val quoted = org.json.JSONObject.quote(text)
    return "(function(){try{var t=$quoted;var el=document.activeElement;" +
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
        "return 'ok';}catch(err){return 'err:'+err;}})()"
}

private class AutoLoungeJsBridge(
    private val webViewProvider: () -> WebView?,
    private val onUrlChanged: (String, String?) -> Unit
) {
    @JavascriptInterface
    fun onSpaUrlChanged(url: String?, title: String?) {
        if (!url.isNullOrBlank() && (url.startsWith("http://") || url.startsWith("https://"))) {
            Handler(Looper.getMainLooper()).post {
                val currentWvUrl = webViewProvider()?.url
                if (currentWvUrl != null && isValidSpaTransition(currentWvUrl, url)) {
                    onUrlChanged(url, title)
                }
            }
        }
    }

    private fun isValidSpaTransition(currentUrl: String, newUrl: String): Boolean {
        return try {
            val currentUri = Uri.parse(currentUrl)
            val newUri = Uri.parse(newUrl)
            currentUri.scheme.equals(newUri.scheme, ignoreCase = true) &&
                currentUri.host.equals(newUri.host, ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ParkBrowser(
    initialUrl: String = "https://m.youtube.com",
    isAppMode: Boolean = false,
    appName: String? = null,
    isParked: Boolean = true,
    isPassengerBypass: Boolean = false,
    onPassengerBypass: (() -> Unit)? = null,
    isCarAudioActive: Boolean = false,
    onClose: (() -> Unit)? = null,
    onAddBookmark: ((title: String, url: String) -> Unit)? = null,
    onPageUrlChanged: ((url: String, title: String?) -> Unit)? = null,
    acceptRemoteText: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Optimize YouTube URL for mobile webview
    val sanitizedUrl = remember(initialUrl) {
        var u = initialUrl
        if (u.contains("youtu.be/")) {
            val videoId = u.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
            u = "https://m.youtube.com/watch?v=$videoId"
        } else if (u.contains("youtube.com") && !u.contains("m.youtube.com")) {
            u = u.replace("www.youtube.com", "m.youtube.com")
        }
        u
    }

    // Hide address bar in app and video modes
    val isCleanVideoMode = remember(sanitizedUrl, isAppMode) {
        isAppMode || sanitizedUrl.contains("youtube.com")
    }

    var currentUrl by remember { mutableStateOf(sanitizedUrl) }
    var inputQuery by remember { mutableStateOf(sanitizedUrl) }
    var pageTitle by remember { mutableStateOf(appName ?: "AutoLounge Web") }
    var isLoading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    // Default to desktop user agent for general browsing, mobile for video
    var isDesktopMode by remember { mutableStateOf(!isCleanVideoMode) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var bookmarkAddedRecently by remember { mutableStateOf(false) }
    var isUrlFieldFocused by remember { mutableStateOf(false) }
    var driverAcknowledged by remember { mutableStateOf(false) }

    LaunchedEffect(isParked) {
        if (isParked) {
            driverAcknowledged = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    webViewInstance?.onPause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    webViewInstance?.onResume()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webViewInstance?.run {
                stopLoading()
                onPause()
                removeJavascriptInterface("AutoLoungeBridge")
                webChromeClient = null
                webViewClient = WebViewClient()
                loadUrl("about:blank")
                clearHistory()
                (parent as? ViewGroup)?.removeView(this)
                removeAllViews()
                destroy()
            }
            webViewInstance = null
        }
    }

    // Web sitesinin mikrofon isteği (ör. YouTube sesli arama) için bekleyen izin talebi
    var pendingMicRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingMicRequest?.let { req ->
            try {
                if (granted) req.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) else req.deny()
            } catch (_: Exception) {}
        }
        pendingMicRequest = null
        if (!granted) {
            Toast.makeText(context, tr("Mikrofon izni verilmedi. Telefon Ayarlar → Uygulamalar → AutoLounge → İzinler'den açabilirsiniz."), Toast.LENGTH_LONG).show()
        }
    }

    // Telefondaki klavye dock'undan gönderilen metni bu sayfadaki odaklı alana yaz (araç ekranı)
    if (acceptRemoteText) {
        LaunchedEffect(Unit) {
            com.autolounge.park.data.AutoLoungeSyncBridge.remoteTextFlow.collect { text ->
                webViewInstance?.evaluateJavascript(remoteTextInjectionJs(text), null)
            }
        }
    }

    // Clear previous page canvas on URL change to prevent visual ghosting
    LaunchedEffect(sanitizedUrl) {
        val wv = webViewInstance ?: return@LaunchedEffect
        if (wv.url != sanitizedUrl && sanitizedUrl.isNotBlank()) {
            wv.stopLoading()
            wv.loadUrl(sanitizedUrl)
        }
        currentUrl = sanitizedUrl
        if (!isUrlFieldFocused) inputQuery = sanitizedUrl
    }

    // Pause web playback when external car audio session activates
    LaunchedEffect(isCarAudioActive) {
        if (isCarAudioActive) {
            webViewInstance?.evaluateJavascript(
                "(function(){try{document.querySelectorAll('video,audio').forEach(function(v){v.pause();});}catch(e){}})();",
                null
            )
        }
    }

    // Sürüş güvenliği: Araç hareket ettiğinde ve yolcu modu kapalıyken tam ekranı kapat ve videoyu gizle, ses akmaya devam etsin
    val isLockoutActive = !isParked && !isPassengerBypass
    LaunchedEffect(isLockoutActive) {
        if (isLockoutActive) {
            if (customView != null) {
                customViewCallback?.onCustomViewHidden()
                customView = null
                customViewCallback = null
            }
            webViewInstance?.evaluateJavascript(
                "(function(){try{document.querySelectorAll('video').forEach(function(v){v.style.visibility='hidden';});}catch(e){}})();",
                null
            )
        } else {
            webViewInstance?.evaluateJavascript(
                "(function(){try{document.querySelectorAll('video').forEach(function(v){v.style.visibility='visible';});}catch(e){}})();",
                null
            )
        }
    }

    // Speech-to-text search trigger
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputQuery = spokenText
                val searchUrl = "https://www.google.com/search?q=" + java.net.URLEncoder.encode(spokenText, "UTF-8")
                currentUrl = searchUrl
                webViewInstance?.loadUrl(searchUrl)
            }
        }
    }

    fun executeNavigation(query: String) {
        val clean = query.trim()
        val target = if (clean.startsWith("http://") || clean.startsWith("https://")) {
            clean
        } else if (clean.contains(".") && !clean.contains(" ")) {
            "https://$clean"
        } else {
            "https://www.google.com/search?q=" + java.net.URLEncoder.encode(clean, "UTF-8")
        }
        currentUrl = target
        inputQuery = target
        keyboardController?.hide()
        webViewInstance?.run {
            stopLoading()
            loadUrl(target)
        }
    }

    // System back handler: Fullscreen video -> Web history -> Close container
    BackHandler {
        if (customView != null) {
            customViewCallback?.onCustomViewHidden()
            customView = null
        } else if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onClose?.invoke()
        }
    }

    // Immersive mode in fullscreen video playback
    DisposableEffect(customView) {
        val activity = context as? Activity
        val window = activity?.window
        if (window != null && customView != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // HTML5 custom view container
    if (customView != null && isParked) {
        AndroidView(
            factory = {
                FrameLayout(it).apply {
                    setBackgroundColor(android.graphics.Color.BLACK)
                    addView(
                        customView,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    val topBarHeight = if (isCleanVideoMode) 52.dp else 56.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Primary WebView viewport
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Hardware acceleration for smooth GPU rendering
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    setBackgroundColor(android.graphics.Color.BLACK)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        allowFileAccess = false
                        allowContentAccess = false
                        setGeolocationEnabled(false)
                        offscreenPreRaster = true

                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        safeBrowsingEnabled = true

                        // Chrome mobile user agent for video platforms
                        userAgentString = if (isCleanVideoMode) MOBILE_USER_AGENT else {
                            if (isDesktopMode) DESKTOP_USER_AGENT else MOBILE_USER_AGENT
                        }
                    }

                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    // Görünürken render sürecine öncelik ver; arka planda sistem geri alabilsin
                    setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true)

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                            // Reklam/izleyici istekleri hiç ağa çıkmadan boş yanıtla kapatılır
                            if (request != null && !request.isForMainFrame && WebContentBlocker.isBlocked(request.url)) {
                                return WebContentBlocker.emptyResponse()
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            super.onReceivedError(view, request, error)
                            // Bağlantı koptuysa ana sayfayı bir kez önbellekten açmayı dene
                            if (request?.isForMainFrame == true && view != null &&
                                view.settings.cacheMode != WebSettings.LOAD_CACHE_ELSE_NETWORK &&
                                (error?.errorCode ?: 0) in setOf(ERROR_HOST_LOOKUP, ERROR_CONNECT, ERROR_TIMEOUT)
                            ) {
                                view.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                                view.reload()
                                view.postDelayed({ view.settings.cacheMode = WebSettings.LOAD_DEFAULT }, 5000)
                            }
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val targetUri = request?.url ?: return false
                            val url = targetUri.toString()
                            val scheme = targetUri.scheme?.lowercase() ?: return false

                            // Retain standard HTTP/HTTPS schemes within WebView
                            if (scheme == "http" || scheme == "https") {
                                return false
                            }

                            // Enforce driving safety: block external activity intents while moving
                            if (!isParked) {
                                return true
                            }

                            // Yerel dosya, script ve tehlikeli protokolleri engelle
                            if (scheme in setOf("file", "content", "javascript", "data", "about")) {
                                return true
                            }

                            return try {
                                val intent = if (scheme == "intent") {
                                    Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                                        // Prevent intent redirection and privilege escalation
                                        addCategory(Intent.CATEGORY_BROWSABLE)
                                        component = null
                                        selector = null
                                        flags = 0
                                    }
                                } else {
                                    // Allow only verified external URL schemes
                                    if (scheme !in setOf("tel", "mailto", "market", "geo")) {
                                        return true
                                    }
                                    Intent(Intent.ACTION_VIEW, targetUri).apply {
                                        addCategory(Intent.CATEGORY_BROWSABLE)
                                        flags = 0
                                    }
                                }

                                if (intent.action != null && intent.action != Intent.ACTION_VIEW) {
                                    return true
                                }

                                // Fallback handling for browser intents
                                val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                if (!fallbackUrl.isNullOrBlank() && (fallbackUrl.startsWith("http://") || fallbackUrl.startsWith("https://"))) {
                                    view?.loadUrl(fallbackUrl)
                                    return true
                                }

                                val actContext = view?.context ?: return true
                                if (intent.resolveActivity(actContext.packageManager) != null) {
                                    actContext.startActivity(intent)
                                }
                                true
                            } catch (_: Exception) {
                                true
                            }
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                            url?.let {
                                currentUrl = it
                                if (!isUrlFieldFocused) inputQuery = it
                                if (it.startsWith("http")) onPageUrlChanged?.invoke(it, view?.title)
                            }
                            view?.evaluateJavascript(SPA_SYNC_JS, null)
                        }

                        // Track pushState history events in Single Page Applications
                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            super.doUpdateVisitedHistory(view, url, isReload)
                            url?.let {
                                if (it.startsWith("http")) {
                                    currentUrl = it
                                    if (!isUrlFieldFocused) inputQuery = it
                                    onPageUrlChanged?.invoke(it, view?.title)
                                }
                            }
                            view?.evaluateJavascript(SPA_SYNC_JS, null)
                        }

                        override fun onPageCommitVisible(view: WebView?, url: String?) {
                            isLoading = false
                            view?.evaluateJavascript(SPA_SYNC_JS, null)
                            view?.evaluateJavascript(CONSENT_AUTO_ACCEPT_JS, null)
                            // Reklam kutuları sayfa ilk göründüğü anda gizlensin (yanıp sönmesin)
                            if (WebContentBlocker.enabled.value) {
                                view?.evaluateJavascript(WebContentBlocker.COSMETIC_JS, null)
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            canGoBack = view?.canGoBack() == true
                            canGoForward = view?.canGoForward() == true
                            pageTitle = view?.title ?: appName ?: "AutoLounge"
                            CookieManager.getInstance().flush()

                            // Initialize SPA navigation and URL observation script
                            view?.evaluateJavascript(SPA_SYNC_JS, null)
                            if (url?.contains("youtube.com") == true) {
                                view?.evaluateJavascript(YOUTUBE_LANDSCAPE_FIT_JS, null)
                            }
                            if (WebContentBlocker.enabled.value) {
                                view?.evaluateJavascript(WebContentBlocker.COSMETIC_JS, null)
                            }
                            view?.postDelayed({
                                try { view?.evaluateJavascript(SPA_SYNC_JS, null) } catch (_: Exception) {}
                            }, 800)

                            // Hide video elements when vehicle is in motion while keeping audio active
                            if (!isParked) {
                                view?.evaluateJavascript(
                                    "(function(){try{document.querySelectorAll('video').forEach(function(v){v.style.visibility='hidden';});}catch(e){}})();",
                                    null
                                )
                            }

                            // Suppress mobile app installation prompt dialog
                            if (url?.contains("youtube.com") == true) {
                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                        try {
                                            var upsell = document.querySelector('ytm-upsell-dialog-renderer');
                                            if (upsell) upsell.style.display = 'none';
                                        } catch(e) {}
                                    })();
                                    """.trimIndent(),
                                    null
                                )
                            }

                            // Auto-dismiss consent and cookie banners
                            view?.evaluateJavascript(CONSENT_AUTO_ACCEPT_JS, null)
                            view?.postDelayed({
                                try { view?.evaluateJavascript(CONSENT_AUTO_ACCEPT_JS, null) } catch (_: Exception) {}
                            }, 1200)
                            view?.postDelayed({
                                try { view?.evaluateJavascript(CONSENT_AUTO_ACCEPT_JS, null) } catch (_: Exception) {}
                            }, 2500)
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            title?.let { pageTitle = it }
                        }

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

                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            if (!isParked && !isPassengerBypass) {
                                Toast.makeText(context, tr("Sürüş güvenliği: Video yolcu onayı bekliyor"), Toast.LENGTH_SHORT).show()
                                callback?.onCustomViewHidden()
                                return
                            }
                            customView = view
                            customViewCallback = callback
                        }

                        override fun onPermissionRequest(request: PermissionRequest?) {
                            val req = request ?: return
                            try {
                                val wantsMic = PermissionRequest.RESOURCE_AUDIO_CAPTURE in req.resources
                                if (wantsMic && !hasMicPermission(context)) {
                                    // Sesli arama vb.: sistem izin penceresini aç. Araç ekranında (Android 15+)
                                    // bu pencere telefonda çıkar; kullanıcıyı oraya yönlendir.
                                    pendingMicRequest = req
                                    Toast.makeText(context, tr("Mikrofon izni için telefonunuzdaki pencereyi onaylayın"), Toast.LENGTH_LONG).show()
                                    micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                    return
                                }
                                val allowed = req.resources.filter {
                                    it == PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID ||
                                        (it == PermissionRequest.RESOURCE_AUDIO_CAPTURE && wantsMic)
                                }
                                if (allowed.isNotEmpty()) {
                                    req.grant(allowed.toTypedArray())
                                } else {
                                    req.deny()
                                }
                            } catch (_: Exception) {
                                try { req.deny() } catch (_: Exception) {}
                            }
                        }

                        override fun onHideCustomView() {
                            customView = null
                            customViewCallback?.onCustomViewHidden()
                            customViewCallback = null
                        }
                    }

                    addJavascriptInterface(
                        AutoLoungeJsBridge(
                            webViewProvider = { webViewInstance },
                            onUrlChanged = { url, title ->
                                currentUrl = url
                                if (!isUrlFieldFocused) inputQuery = url
                                if (!title.isNullOrBlank()) pageTitle = title
                                onPageUrlChanged?.invoke(url, title)
                            }
                        ),
                        "AutoLoungeBridge"
                    )

                    loadUrl(currentUrl)
                    webViewInstance = this
                }
            },
            update = { v ->
                val visibilityValue = if (isParked || isPassengerBypass) "visible" else "hidden"
                v.evaluateJavascript(
                    "(function(){try{document.querySelectorAll('video, iframe').forEach(function(v){v.style.visibility='$visibilityValue';});}catch(e){}})();",
                    null
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = topBarHeight)
                .navigationBarsPadding()
        )

        // Linear page load progress indicator
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = topBarHeight)
                    .height(2.5.dp)
                    .align(Alignment.TopCenter),
                color = AutoLoungePalette.EmeraldNeon,
                trackColor = Color.Transparent
            )
        }

        // Top control overlay
        if (isCleanVideoMode) {
            // Video mode: frosted glass top bar
            Surface(
                color = Color(0xDE0B0D13), // Translucent obsidian background
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(topBarHeight)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back button
                        Surface(
                            color = Color(0x1F242836),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (webViewInstance?.canGoBack() == true) {
                                        webViewInstance?.goBack()
                                    } else {
                                        onClose?.invoke()
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = tr("Geri / Kapat"),
                                    tint = AutoLoungePalette.TextPure,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        // Center cockpit status badge and title
                        Surface(
                            color = Color(0x14FFFFFF),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(horizontal = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isLoading) AutoLoungePalette.AmberSignal else AutoLoungePalette.AcidMint)
                                )
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    text = tr(if (!appName.isNullOrBlank()) appName else pageTitle).uppercase(),
                                    color = AutoLoungePalette.TextPure,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Action controls (Refresh & Close)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Yenile Butonu
                            Surface(
                                color = Color(0x1F242836),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                IconButton(
                                    onClick = { webViewInstance?.reload() },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = tr("Yenile"),
                                        tint = AutoLoungePalette.TextPure,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Kapat Butonu
                            if (onClose != null) {
                                Surface(
                                    color = Color(0x1F242836),
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    IconButton(
                                        onClick = onClose,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = tr("Kapat"),
                                            tint = AutoLoungePalette.TextPure,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = Color.White.copy(alpha = 0.08f)
                    )
                }
            }
        } else {
            // General browsing mode: address navigation bar
            Surface(
                color = AutoLoungePalette.CarbonElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(topBarHeight)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.06f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            IconButton(
                                onClick = { if (webViewInstance?.canGoBack() == true) webViewInstance?.goBack() },
                                enabled = canGoBack,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = tr("Geri"),
                                    tint = if (canGoBack) AutoLoungePalette.TextPure else AutoLoungePalette.TextDim,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AutoLoungePalette.SurfaceCard)
                                .border(1.dp, AutoLoungePalette.SurfaceBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    if (currentUrl.startsWith("https://")) Icons.Default.Lock else Icons.Default.Search,
                                    contentDescription = null,
                                    tint = if (currentUrl.startsWith("https://")) AutoLoungePalette.EmeraldNeon else AutoLoungePalette.TextDim,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                androidx.compose.foundation.text.BasicTextField(
                                    value = inputQuery,
                                    onValueChange = { inputQuery = it },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { executeNavigation(inputQuery) }),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = AutoLoungePalette.TextPure,
                                        fontSize = 13.5.sp
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .onFocusChanged { isUrlFieldFocused = it.isFocused }
                                )
                            }
                        }

                        Surface(
                            color = Color.White.copy(alpha = 0.06f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            IconButton(
                                onClick = { webViewInstance?.reload() },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = tr("Yenile"),
                                    tint = AutoLoungePalette.TextPure,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        if (onClose != null) {
                            Surface(
                                color = Color.White.copy(alpha = 0.06f),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                IconButton(
                                    onClick = onClose,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = tr("Kapat"),
                                        tint = AutoLoungePalette.TextPure,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = AutoLoungePalette.SurfaceBorder)
                }
            }
        }

        // Driving safety barrier layer
        if (!isParked && !isPassengerBypass) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF00A0B0E))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(28.dp)
                ) {
                    Surface(
                        color = Color(0x22F59E0B),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color(0x66F59E0B)),
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🛡️", fontSize = 28.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = tr("Sürüş Güvenliği Uyarısı"),
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (driverAcknowledged) {
                            tr("Sürüş güvenliği modu devrede.\nCanlı ses kesintisiz olarak çalmaya devam ediyor.")
                        } else {
                            tr("Lütfen sürüş esnasında sürücü olarak video izlemeyiniz.\nTrafik ve can güvenliğiniz her şeyden önemlidir.\nCanlı ses kesintisiz olarak çalmaya devam ediyor.")
                        },
                        color = AutoLoungePalette.TextDim,
                        fontSize = 13.5.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!driverAcknowledged) {
                            Button(
                                onClick = { driverAcknowledged = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2430)),
                                shape = RoundedCornerShape(22.dp),
                                border = BorderStroke(1.dp, Color(0xFF333D52)),
                                modifier = Modifier.height(46.dp)
                            ) {
                                Text(tr("Tamam (Ses Devam Etsin)"), color = Color(0xFFE2E8F0), fontSize = 12.5.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                        }
                        Button(
                            onClick = { onPassengerBypass?.invoke() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Text(tr("Yolcuyum (Videoyu Aç)"), color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
