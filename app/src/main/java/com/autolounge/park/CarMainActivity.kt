package com.autohub.park

import android.os.Bundle
import android.webkit.CookieManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.autohub.park.data.BookmarkViewModel
import com.autohub.park.data.NewsChannel
import com.autohub.park.data.NewsChannelsRepository
import com.autohub.park.ui.AutoHubTheme
import com.autohub.park.ui.AutoLoungePalette
import com.autohub.park.ui.browser.ParkBrowser
import com.autohub.park.ui.components.CarDashboardContent
import com.autohub.park.ui.components.LiveTvPlayer
import com.autohub.park.ui.components.PaywallDialog
import com.autohub.park.i18n.I18n
import com.autohub.park.i18n.tr
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CarMainActivity : ComponentActivity() {

    private val vm: BookmarkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        I18n.init(this)
        CookieManager.getInstance().setAcceptCookie(true)

        setContent {
            val config by vm.config.collectAsState()
            AutoHubTheme(carTheme = config.theme, themeMode = config.themeMode) {
                CarScreenRoot(vm = vm)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        vm.usageTracker.setCarScreenActive(true)
        com.autohub.park.data.AutoLoungeSyncBridge.setCarAppAlive(true)
        com.autohub.park.data.CarSetupChecker.markCarSessionSeen(this)
    }

    override fun onPause() {
        super.onPause()
        vm.usageTracker.setCarScreenActive(false)
        com.autohub.park.data.AutoLoungeSyncBridge.setCarAppAlive(false)
    }
}


@Composable
fun CarScreenRoot(vm: BookmarkViewModel) {
    val context = LocalContext.current
    val items by vm.items.collectAsState()
    val config by vm.config.collectAsState()
    val remainingSeconds by vm.remainingSeconds.collectAsState()
    val isPremium by vm.isPremium.collectAsState()
    val isParked by vm.safetyManager.isParked.collectAsState()
    val isPassengerBypass by vm.safetyManager.passengerBypassActive.collectAsState()
    val isCarAudioActive by vm.usageTracker.isAudioStreamingActive.collectAsState()

    var activeBrowserUrl by remember { mutableStateOf<String?>(null) }
    var activeBrowserTitle by remember { mutableStateOf<String?>(null) }
    var activeBrowserIsAppMode by remember { mutableStateOf(false) }
    var activeLiveTvChannel by remember { mutableStateOf<NewsChannel?>(null) }
    var showPaywallDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        com.autohub.park.data.AutoLoungeSyncBridge.openOnPhoneFlow.collect { cmd ->
            when (cmd.kind) {
                "web" -> {
                    activeBrowserUrl = cmd.url.ifBlank { "https://www.youtube.com" }
                    activeBrowserTitle = cmd.title.ifBlank { "AutoLounge" }
                    activeBrowserIsAppMode = true
                    activeLiveTvChannel = null
                }
                "assistant" -> com.autohub.park.ui.AssistantLauncher.launch(context)
                "dashboard" -> {
                    activeBrowserUrl = null
                    activeLiveTvChannel = null
                }
                else -> {}
            }
        }
    }

    // VIP veya deneme süresi varsa sürüş güvenlik kilidini otomatik bypass yap
    LaunchedEffect(isPremium, remainingSeconds) {
        if (isPremium || remainingSeconds > 0) {
            vm.safetyManager.setPassengerBypass(true)
        }
    }

    LaunchedEffect(Unit) {
        com.autohub.park.data.AutoLoungeSyncBridge.openOnCarFlow.collect { req ->
            when (req.kind) {
                "web" -> {
                    if (req.url.isNotBlank()) {
                        activeBrowserUrl = req.url
                        activeBrowserTitle = req.title.ifBlank { "AutoLounge" }
                        activeBrowserIsAppMode = true
                        activeLiveTvChannel = null
                    }
                }
                "live_tv" -> {
                    val stream = req.streamUrl ?: req.url
                    val matched = NewsChannelsRepository.allChannels.firstOrNull {
                        it.audioStreamUrl == stream || it.url == stream || it.title.equals(req.title, ignoreCase = true)
                    } ?: NewsChannel(
                        id = "car_tv",
                        title = req.title.ifBlank { tr("Canlı TV") },
                        url = stream,
                        audioStreamUrl = stream,
                        countryCode = "TR",
                        countryName = "Canlı Yayın",
                        category = "Televizyon"
                    )
                    activeLiveTvChannel = matched
                    activeBrowserUrl = null
                }
                "dashboard" -> {
                    activeBrowserUrl = null
                    activeLiveTvChannel = null
                }
            }
        }
    }

    if (activeLiveTvChannel != null) {
        LiveTvPlayer(
            channel = activeLiveTvChannel!!,
            isParked = isParked,
            isPassengerBypass = isPassengerBypass,
            onPassengerBypass = { vm.safetyManager.setPassengerBypass(true) },
            onClose = { activeLiveTvChannel = null }
        )
        return
    }

    if (activeBrowserUrl != null) {
        ParkBrowser(
            initialUrl = activeBrowserUrl ?: "https://www.google.com",
            isAppMode = activeBrowserIsAppMode,
            appName = activeBrowserTitle,
            isParked = isParked,
            isPassengerBypass = isPassengerBypass,
            onPassengerBypass = { vm.safetyManager.setPassengerBypass(true) },
            isCarAudioActive = isCarAudioActive,
            onClose = { activeBrowserUrl = null },
            onAddBookmark = { title, url -> vm.add(title, url) },
            acceptRemoteText = true
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(AutoLoungePalette.ObsidianVoid)
    ) {
        CarDashboardContent(
            config = config,
            bookmarks = items,
            remainingSeconds = remainingSeconds,
            isPremium = isPremium,
            onOpenUrl = { url, title, isApp ->
                if (isPremium || remainingSeconds > 0) {
                    activeBrowserUrl = url
                    activeBrowserTitle = title
                    activeBrowserIsAppMode = isApp
                } else {
                    showPaywallDialog = true
                }
            },
            onOpenPaywall = { showPaywallDialog = true },
            faviconProvider = { vm.favicon(it) },
            onReorder = { vm.reorderCards(it) },
            onDeleteBookmark = { vm.delete(it) },
            onAddBookmark = { title, url -> vm.add(title, url) },
            onToggleAppVisibility = { key, show -> vm.toggleCard(key, show) },
            onLaunchAssistant = { com.autohub.park.ui.AssistantLauncher.launch(context) },
            // Canlı TV kanalları web sitesi yerine doğrudan yerleşik oynatıcıda açılır
            onOpenLiveTv = { ch ->
                if (isPremium || remainingSeconds > 0) {
                    activeLiveTvChannel = ch
                    activeBrowserUrl = null
                } else {
                    showPaywallDialog = true
                }
            },
            isCarScreen = true
        )
    }

    if (showPaywallDialog) {
        PaywallDialog(
            remainingSeconds = remainingSeconds,
            accent = config.theme.primary,
            onActivate = { key, callback -> vm.activateLicense(key, callback) },
            onDismiss = { showPaywallDialog = false }
        )
    }
}
