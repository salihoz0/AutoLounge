package com.autohub.park

import android.app.Activity
import com.autohub.park.i18n.tr
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.autohub.park.data.Bookmark
import com.autohub.park.data.BookmarkViewModel
import com.autohub.park.data.CarSetupChecker
import com.autohub.park.data.CarConfig
import com.autohub.park.data.NewsChannel
import com.autohub.park.data.NewsChannelsRepository
import com.autohub.park.ui.*
import com.autohub.park.ui.browser.ParkBrowser
import com.autohub.park.ui.components.CarDashboardContent
import com.autohub.park.ui.components.PaywallDialog
import com.autohub.park.ui.components.MembershipScreen
import com.autohub.park.ui.components.AddAppModal
import com.autohub.park.ui.components.CarKeyboardDock
import com.autohub.park.ui.components.CarSetupBanner
import com.autohub.park.ui.components.CarSetupGuideDialog
import com.autohub.park.ui.components.rememberCarSetupStatus
import com.autohub.park.ui.components.LiveTvPlayer
import com.autohub.park.data.AutoLoungeSyncBridge
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookieManager.getInstance().setAcceptCookie(true)
        setContent {
            val vm: BookmarkViewModel = hiltViewModel()
            val config by vm.config.collectAsState()
            AutoHubTheme(carTheme = config.theme, themeMode = config.themeMode) {
                MainPhoneScreen(vm = vm)
            }
        }
    }
}

enum class MainNavTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    COCKPIT("Kokpit", Icons.Default.Dashboard),
    CUSTOMIZER("Ayarlar", Icons.Default.Tune),
    MEMBERSHIP("VIP Lisans", Icons.Default.MilitaryTech)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPhoneScreen(vm: BookmarkViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val activity = context as? Activity

    val items by vm.items.collectAsState()
    val config by vm.config.collectAsState()
    val remainingSeconds by vm.remainingSeconds.collectAsState()
    val isPremium by vm.isPremium.collectAsState()
    val isCarAudioActive by vm.usageTracker.isAudioStreamingActive.collectAsState()
    val isParked by vm.safetyManager.isParked.collectAsState()
    val isPassengerBypass by vm.safetyManager.passengerBypassActive.collectAsState()

    var selectedTab by remember { mutableStateOf(MainNavTab.COCKPIT) }
    var activeBrowserUrl by remember { mutableStateOf<String?>(null) }
    var activeBrowserTitle by remember { mutableStateOf<String?>(null) }
    var activeBrowserIsAppMode by remember { mutableStateOf(false) }
    var activeLiveTvChannel by remember { mutableStateOf<NewsChannel?>(null) }
    var selectedLocalMediaUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var editingBookmark by remember { mutableStateOf<Bookmark?>(null) }
    var editTitleInput by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showPaywallDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var addError by remember { mutableStateOf("") }
    var lastSentCarUrl by remember { mutableStateOf<String?>(null) }
    var lastSentCarTitle by remember { mutableStateOf<String?>(null) }
    val carAlive by AutoLoungeSyncBridge.carAppAliveFlow.collectAsState()
    val carSetupStatus by rememberCarSetupStatus()
    var showCarSetupGuide by remember { mutableStateOf(false) }
    var setupBannerHidden by remember { mutableStateOf(CarSetupChecker.isBannerHidden(context)) }

    fun launchAssistant() {
        AssistantLauncher.launch(context)
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedLocalMediaUri = uri
            AutoLoungeSyncBridge.syncToCar(
                id = "local_media",
                title = tr("Yerel Medya"),
                subtitle = tr("Cihaz Dosyası"),
                webUrl = uri.toString()
            )
        }
    }

    // VIP veya deneme süresi varsa sürüş güvenlik kilidini otomatik bypass yap
    LaunchedEffect(isPremium, remainingSeconds) {
        if (isPremium || remainingSeconds > 0) {
            vm.safetyManager.setPassengerBypass(true)
        }
    }

    // Servisten gelen durdurma (USB çıktı / AA'dan Durdur): bilgi ver.
    LaunchedEffect(Unit) {
        AutoLoungeSyncBridge.stopRequestFlow.collect {
            Toast.makeText(context, tr("Araç sesi durduruldu"), Toast.LENGTH_SHORT).show()
        }
    }

    // Araç ekranından telefona gelen komutları dinle ve telefon arayüzünü senkronize et
    LaunchedEffect(Unit) {
        AutoLoungeSyncBridge.openOnPhoneFlow.collect { cmd ->
            when (cmd.kind) {
                "web" -> {
                    if (cmd.url.isNotBlank()) {
                        activeBrowserUrl = cmd.url
                        activeBrowserTitle = cmd.title.ifBlank { "AutoLounge" }
                        activeBrowserIsAppMode = true
                        activeLiveTvChannel = null
                    }
                }
                "live_tv" -> {
                    val stream = cmd.url
                    val matched = NewsChannelsRepository.allChannels.firstOrNull {
                        it.audioStreamUrl == stream || it.url == stream || it.title.equals(cmd.title, ignoreCase = true)
                    } ?: NewsChannel(
                        id = "car_synced_tv",
                        title = cmd.title.ifBlank { tr("Canlı TV") },
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
                    selectedLocalMediaUri = null
                }
                "assistant" -> launchAssistant()
            }
        }
    }

    // Yan sekmelerdeyken (Ayarlar/VIP) geri tuşuna basıldığında uygulamadan çıkmak yerine Ana Simülatöre dön
    BackHandler(enabled = selectedTab != MainNavTab.COCKPIT) {
        selectedTab = MainNavTab.COCKPIT
    }

    // Fullscreen local media player overlay
    if (selectedLocalMediaUri != null) {
        com.autohub.park.ui.components.LocalMediaViewer(
            mediaUri = selectedLocalMediaUri!!,
            isParked = isParked,
            isPassengerBypass = isPassengerBypass,
            onPassengerBypass = { vm.safetyManager.setPassengerBypass(true) },
            onClose = {
                selectedLocalMediaUri = null
                AutoLoungeSyncBridge.openOnCar(kind = "dashboard")
            }
        )
        return
    }

    // Fullscreen native live TV player overlay (Direct ExoPlayer HLS/DASH video stream)
    if (activeLiveTvChannel != null) {
        LiveTvPlayer(
            channel = activeLiveTvChannel!!,
            isParked = isParked,
            isPassengerBypass = isPassengerBypass,
            onPassengerBypass = { vm.safetyManager.setPassengerBypass(true) },
            onClose = {
                activeLiveTvChannel = null
                AutoLoungeSyncBridge.openOnCar(kind = "dashboard")
            }
        )
        return
    }

    // Fullscreen web browser viewport with driving safety barrier support
    if (activeBrowserUrl != null) {
        ParkBrowser(
            initialUrl = activeBrowserUrl ?: "https://www.google.com",
            isAppMode = activeBrowserIsAppMode,
            appName = activeBrowserTitle,
            isParked = isParked,
            isPassengerBypass = isPassengerBypass,
            onPassengerBypass = { vm.safetyManager.setPassengerBypass(true) },
            isCarAudioActive = isCarAudioActive,
            onClose = {
                activeBrowserUrl = null
                AutoLoungeSyncBridge.openOnCar(kind = "dashboard")
            },
            onAddBookmark = { title, url ->
                vm.add(title, url) { addError = it }
            },
            // Sync active mobile web URL to car display
            onPageUrlChanged = { url, title ->
                val titleToUse = title?.takeIf { it.isNotBlank() } ?: activeBrowserTitle ?: "AutoLounge"
                if ((url != lastSentCarUrl || titleToUse != lastSentCarTitle) && url.isNotBlank()) {
                    lastSentCarUrl = url
                    lastSentCarTitle = titleToUse
                    AutoLoungeSyncBridge.syncToCar(
                        id = url,
                        title = titleToUse,
                        subtitle = tr("Canlı Web & Video"),
                        webUrl = url
                    )
                    AutoLoungeSyncBridge.openOnCar(
                        kind = "web",
                        url = url,
                        title = titleToUse
                    )
                }
            }
        )
        return
    }

    Scaffold(
        containerColor = AutoLoungePalette.ObsidianVoid,
        topBar = {
            Surface(
                color = AutoLoungePalette.CarbonElevated.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Brand: Logo + AutoLounge Kokpit
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppLogo(size = 32, accentColor = config.theme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "AutoLounge",
                                color = AutoLoungePalette.TextPure,
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                letterSpacing = 0.5.sp,
                                maxLines = 1
                            )
                            Text(
                                tr("KOKPİT"),
                                color = AutoLoungePalette.TextDim,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                maxLines = 1
                            )
                        }
                    }

                    // Right Controls: Status + Sync + 5G + VIP
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Real Park Status indicator
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = AutoLoungePalette.SurfaceCard,
                            border = BorderStroke(
                                1.dp,
                                if (isParked) AutoLoungePalette.AcidMint.copy(alpha = 0.4f)
                                else AutoLoungePalette.CrimsonWarning.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isParked) AutoLoungePalette.AcidMint else AutoLoungePalette.CrimsonWarning)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    if (isParked) tr("PARK") else tr("SÜRÜŞ"),
                                    color = if (isParked) AutoLoungePalette.AcidMint else AutoLoungePalette.CrimsonWarning,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // VIP / Trial Pill
                        Box(Modifier.clickable { showPaywallDialog = true }) {
                            TrialPill(
                                remainingMinutes = remainingSeconds / 60,
                                isPremium = isPremium
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Floating Glass Capsule Dock
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = AutoLoungePalette.CarbonElevated.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder),
                    shadowElevation = 18.dp,
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MainNavTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            val activeColor = if (tab == MainNavTab.MEMBERSHIP) AutoLoungePalette.AmberSignal else config.theme.primary
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { selectedTab = tab }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    tab.icon,
                                    contentDescription = tr(tab.title),
                                    tint = if (isSelected) activeColor else AutoLoungePalette.TextDim,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    tr(tab.title).uppercase(),
                                    color = if (isSelected) activeColor else AutoLoungePalette.TextDim,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .background(AutoLoungePalette.ObsidianVoid)
        ) {
            when (selectedTab) {
                MainNavTab.COCKPIT -> Column(Modifier.fillMaxSize()) {
                    if (!carSetupStatus.carSessionSeen && !setupBannerHidden) {
                        CarSetupBanner(
                            accent = config.theme.primary,
                            onOpenGuide = { showCarSetupGuide = true },
                            onHide = {
                                setupBannerHidden = true
                                CarSetupChecker.hideBanner(context)
                            }
                        )
                    }
                    Box(Modifier.weight(1f)) {
                    SimulatorTab(
                        config = config,
                        bookmarks = items,
                        remainingSeconds = remainingSeconds,
                        isPremium = isPremium,
                        isCarAudioActive = isCarAudioActive,
                        onStopCarAudio = { AutoLoungeSyncBridge.requestStop() },
                        onOpenUrl = { url, title, isApp ->
                            if (isPremium || remainingSeconds > 0) {
                                val itemTitle = title ?: "AutoLounge"
                                if (AutoLoungeSyncBridge.isDirectMediaStream(url)) {
                                    val matched = NewsChannelsRepository.allChannels.firstOrNull {
                                        it.audioStreamUrl == url || it.url == url || it.title.equals(title, ignoreCase = true)
                                    } ?: NewsChannel(
                                        id = url,
                                        title = itemTitle,
                                        url = url,
                                        audioStreamUrl = url,
                                        countryCode = "TR",
                                        countryName = "Canlı Yayın",
                                        category = "Medya"
                                    )
                                    AutoLoungeSyncBridge.openOnCar(
                                        kind = "live_tv",
                                        url = url,
                                        title = matched.title,
                                        streamUrl = url
                                    )
                                    AutoLoungeSyncBridge.syncToCar(
                                        id = matched.id,
                                        title = matched.title,
                                        subtitle = tr("Canlı TV (%s)", tr(matched.category)),
                                        webUrl = url,
                                        streamUri = url,
                                        iconUri = NewsChannelsRepository.logoFor(matched.title)
                                    )
                                    if (carAlive) {
                                        activeLiveTvChannel = null
                                        activeBrowserUrl = null
                                        Toast.makeText(context, tr("Araç ekranına aktarıldı: %s", matched.title), Toast.LENGTH_SHORT).show()
                                    } else {
                                        activeLiveTvChannel = matched
                                        activeBrowserUrl = null
                                    }
                                } else {
                                    lastSentCarUrl = url
                                    AutoLoungeSyncBridge.syncToCar(
                                        id = url,
                                        title = itemTitle,
                                        subtitle = tr("Canlı Web & Yayın"),
                                        webUrl = url
                                    )
                                    AutoLoungeSyncBridge.openOnCar(
                                        kind = "web",
                                        url = url,
                                        title = itemTitle
                                    )
                                    if (carAlive) {
                                        activeBrowserUrl = null
                                        activeLiveTvChannel = null
                                        Toast.makeText(context, tr("Araç ekranına aktarıldı: %s", itemTitle), Toast.LENGTH_SHORT).show()
                                    } else {
                                        activeBrowserUrl = url
                                        activeBrowserTitle = itemTitle
                                        activeBrowserIsAppMode = isApp
                                        activeLiveTvChannel = null
                                    }
                                }
                            } else {
                                showPaywallDialog = true
                            }
                        },
                        // Handle Live TV / Radio stream launch natively on phone and sync to car
                        onOpenLiveTv = { ch ->
                            if (isPremium || remainingSeconds > 0) {
                                val stream = ch.audioStreamUrl ?: ch.url
                                val isDirectStream = AutoLoungeSyncBridge.isDirectMediaStream(stream)
                                if (isDirectStream) {
                                    AutoLoungeSyncBridge.openOnCar(
                                        kind = "live_tv",
                                        url = stream,
                                        title = ch.title,
                                        streamUrl = stream
                                    )
                                    AutoLoungeSyncBridge.syncToCar(
                                        id = ch.id,
                                        title = ch.title,
                                        subtitle = tr("Canlı TV (%s)", tr(ch.category)),
                                        webUrl = ch.url,
                                        streamUri = stream,
                                        iconUri = NewsChannelsRepository.logoFor(ch.title)
                                    )
                                    if (carAlive) {
                                        activeLiveTvChannel = null
                                        activeBrowserUrl = null
                                        Toast.makeText(context, tr("Araç ekranında oynatılıyor: %s", ch.title), Toast.LENGTH_SHORT).show()
                                    } else {
                                        activeLiveTvChannel = ch
                                        activeBrowserUrl = null
                                    }
                                } else {
                                    AutoLoungeSyncBridge.openOnCar(
                                        kind = "web",
                                        url = ch.url,
                                        title = ch.title
                                    )
                                    AutoLoungeSyncBridge.syncToCar(
                                        id = ch.id,
                                        title = ch.title,
                                        subtitle = tr("Canlı TV (%s)", tr(ch.category)),
                                        webUrl = ch.url,
                                        iconUri = NewsChannelsRepository.logoFor(ch.title)
                                    )
                                    if (carAlive) {
                                        activeBrowserUrl = null
                                        activeLiveTvChannel = null
                                        Toast.makeText(context, tr("Araç ekranında açılıyor: %s", ch.title), Toast.LENGTH_SHORT).show()
                                    } else {
                                        activeBrowserUrl = ch.url
                                        activeBrowserTitle = ch.title
                                        activeBrowserIsAppMode = true
                                        activeLiveTvChannel = null
                                    }
                                }
                            } else {
                                showPaywallDialog = true
                            }
                        },
                        onLaunchAssistant = { launchAssistant() },
                        onOpenPaywall = { showPaywallDialog = true },
                        faviconProvider = { vm.favicon(it) },
                        onReorder = { vm.reorderCards(it) },
                        onDeleteBookmark = { vm.delete(it) },
                        onAddBookmark = { title, url -> vm.add(title, url) { addError = it } },
                        onToggleAppVisibility = { appId, visible -> vm.toggleCard(appId, visible) },
                        onOpenLocalMedia = {
                            mediaPickerLauncher.launch("video/*,image/*")
                        }
                    )
                    }
                }
                MainNavTab.CUSTOMIZER -> {
                    CustomizerTab(
                        config = config,
                        bookmarks = items,
                        isPremium = isPremium,
                        remainingSeconds = remainingSeconds,
                        isPassengerBypass = isPassengerBypass,
                        onSetPassengerBypass = { active -> vm.safetyManager.setPassengerBypass(active) },
                        onSelectTheme = { theme -> vm.setTheme(theme) },
                        onSelectThemeMode = { mode -> vm.setThemeMode(mode) },
                        onToggleCard = { key, show -> vm.toggleCard(key, show) },
                        onSetColumns = { cols -> vm.setGridColumns(cols) },
                        onEditBookmark = { b ->
                            editingBookmark = b
                            editTitleInput = b.title
                        },
                        onDeleteBookmark = { id -> vm.delete(id) },
                        onAddNew = { showAddDialog = true },
                        onResetAll = { showResetDialog = true },
                        onOpenCarSetup = { showCarSetupGuide = true }
                    )
                }
                MainNavTab.MEMBERSHIP -> {
                    val license by vm.license.collectAsState()
                    MembershipScreen(
                        remainingSeconds = remainingSeconds,
                        isPremium = isPremium,
                        license = license,
                        accent = config.theme.primary,
                        onActivate = { key, callback -> vm.activateLicense(key, callback) },
                        onDeactivate = { callback -> vm.deactivateLicense(callback) },
                        onResendKey = { email, callback -> vm.resendLicenseKey(email, callback) },
                        onSimulatePremium = { vm.simulatePremium(it) },
                        onResetTrial = { vm.resetTrial() }
                    )
                }
            }

            // Remote keyboard input bridge for vehicle head unit
            if (selectedTab == MainNavTab.COCKPIT && carAlive) {
                CarKeyboardDock(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }
    }

    if (showAddDialog) {
        AddAppModal(
            currentBookmarks = items,
            faviconProvider = { vm.favicon(it) },
            onAddCatalogItem = { item ->
                vm.add(item.title, item.url) { addError = it }
            },
            onRemoveCatalogItem = { targetUrl ->
                val targetClean = targetUrl.trimEnd('/')
                val existing = items.find { it.url.trimEnd('/') == targetClean }
                if (existing != null) {
                    vm.delete(existing.id)
                }
            },
            onAddCustomUrl = { title, url ->
                vm.add(title, url) { addError = it }
            },
            onDismiss = {
                showAddDialog = false
                addError = ""
            }
        )
    }

    if (showCarSetupGuide) {
        CarSetupGuideDialog(accent = config.theme.primary, onDismiss = { showCarSetupGuide = false })
    }

    if (editingBookmark != null) {
        val b = editingBookmark!!
        EditBookmarkDialog(
            currentTitle = b.title,
            onDismiss = { editingBookmark = null },
            onSave = { newTitle ->
                vm.updateBookmarkTitle(b.id, newTitle) { addError = it }
                editingBookmark = null
            }
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

    // Factory reset confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = AutoLoungePalette.CarbonElevated,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    tr("Varsayılan Ayarlara Sıfırla"),
                    color = AutoLoungePalette.TextPure,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    tr("Tüm kokpit görünümü (Onyx Emerald teması, 3 sütunlu düzen) ve özel eklenen siteler sıfırlanacaktır. Emin misiniz?"),
                    color = AutoLoungePalette.TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.resetAllToDefaults {
                            showResetDialog = false
                            Toast.makeText(context, tr("Tüm ayarlar varsayılana sıfırlandı"), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AutoLoungePalette.CrimsonWarning),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(tr("Sıfırla"), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(tr("Vazgeç"), color = AutoLoungePalette.TextDim)
                }
            }
        )
    }
}

/**
 * Tab 1: Car Dashboard Simulator
 */
@Composable
fun SimulatorTab(
    config: CarConfig,
    bookmarks: List<Bookmark>,
    remainingSeconds: Int,
    isPremium: Boolean,
    onOpenUrl: (url: String, title: String?, isAppMode: Boolean) -> Unit,
    onOpenPaywall: () -> Unit,
    faviconProvider: (String) -> String,
    onReorder: (List<String>) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onAddBookmark: ((String, String) -> Unit)? = null,
    onToggleAppVisibility: ((String, Boolean) -> Unit)? = null,
    onOpenLocalMedia: (() -> Unit)? = null,
    onLaunchAssistant: (() -> Unit)? = null,
    onOpenLiveTv: ((NewsChannel) -> Unit)? = null,
    isCarAudioActive: Boolean = false,
    onStopCarAudio: (() -> Unit)? = null
) {
    val nowTitle by com.autohub.park.data.NowPlayingStore.title.collectAsState()
    val nowSub by com.autohub.park.data.NowPlayingStore.subtitle.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AutoLoungePalette.ObsidianVoid)
    ) {
        CarDashboardContent(
            config = config,
            bookmarks = bookmarks,
            remainingSeconds = remainingSeconds,
            isPremium = isPremium,
            onOpenUrl = onOpenUrl,
            onOpenPaywall = onOpenPaywall,
            faviconProvider = faviconProvider,
            onReorder = onReorder,
            onDeleteBookmark = onDeleteBookmark,
            onAddBookmark = onAddBookmark,
            onToggleAppVisibility = onToggleAppVisibility,
            onOpenLocalMedia = onOpenLocalMedia,
            onLaunchAssistant = onLaunchAssistant,
            onOpenLiveTv = onOpenLiveTv
        )

        // Modern mini player (sadece ses aktifken)
        if (isCarAudioActive) {
            Surface(
                color = AutoLoungePalette.CarbonElevated.copy(alpha = 0.97f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, config.theme.primary.copy(alpha = 0.35f)),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(config.theme.primary, config.theme.primary.copy(alpha = 0.55f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Radio, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            nowTitle.ifBlank { tr("Canlı Yayın") },
                            color = AutoLoungePalette.TextPure,
                            fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            nowSub.ifBlank { "AutoLounge" },
                            color = AutoLoungePalette.TextDim,
                            fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
                            repeat(4) { i ->
                                val h = listOf(4, 9, 6, 11)[i]
                                Box(
                                    Modifier.padding(end = 3.dp).width(3.dp).height(h.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(config.theme.primary)
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(tr("CANLI"), color = config.theme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(
                        onClick = { onStopCarAudio?.invoke() },
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = tr("Durdur"), tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Vehicle Cockpit Settings
 */
@Composable
fun CustomizerTab(
    config: CarConfig,
    bookmarks: List<Bookmark>,
    isPremium: Boolean = false,
    remainingSeconds: Int = 0,
    isPassengerBypass: Boolean = false,
    onSetPassengerBypass: (Boolean) -> Unit = {},
    onSelectTheme: (CarVisualTheme) -> Unit,
    onSelectThemeMode: (String) -> Unit,
    onToggleCard: (key: String, show: Boolean) -> Unit,
    onSetColumns: (Int) -> Unit,
    onEditBookmark: (Bookmark) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onAddNew: () -> Unit,
    onResetAll: () -> Unit,
    onOpenCarSetup: () -> Unit = {}
) {
    var lumenBrightness by remember { mutableStateOf(0.85f) }
    // VIP veya deneme süresi varsa bypass varsayılan AÇIK
    val hasAccess = isPremium || remainingSeconds > 0
    val isBypassActive = isPassengerBypass

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // --- 0. VISUAL FEEDBACK LAYER: KOKPİT PROFİLİ ---
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = AutoLoungePalette.CarbonElevated,
                border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = config.theme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                tr("Kokpit Profili"),
                                color = AutoLoungePalette.TextPure,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = AutoLoungePalette.SurfaceCard,
                            border = BorderStroke(1.dp, config.theme.primary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                "AutoLounge v4.2",
                                color = config.theme.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        tr("Donanım optimizasyonu, ekran ambiyansı ve sürüş kısıtlamaları yapılandırma paneli."),
                        color = AutoLoungePalette.TextDim,
                        fontSize = 11.5.sp
                    )
                }
            }
        }

        // --- 0.5 ARAÇ EKRANI KURULUM REHBERİ ---
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = AutoLoungePalette.CarbonElevated,
                border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onOpenCarSetup() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = config.theme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("Araç Ekranı Kurulum Rehberi"), color = AutoLoungePalette.TextPure, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            tr("Telefonunuza göre Android Auto ayarlarını kontrol edin"),
                            color = AutoLoungePalette.TextDim,
                            fontSize = 11.5.sp
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AutoLoungePalette.TextDim)
                }
            }
        }

        // --- 0.55 UYGULAMA DİLİ ---
        item {
            val langContext = LocalContext.current
            var langMenuOpen by remember { mutableStateOf(false) }
            val currentLang = com.autohub.park.i18n.I18n.languages
                .firstOrNull { it.code == com.autohub.park.i18n.I18n.lang }?.nativeLabel ?: "English"
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = AutoLoungePalette.CarbonElevated,
                border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { langMenuOpen = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = config.theme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(com.autohub.park.i18n.tr("Uygulama Dili"), color = AutoLoungePalette.TextPure, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(currentLang, color = AutoLoungePalette.TextDim, fontSize = 11.5.sp)
                    }
                    Box {
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AutoLoungePalette.TextDim)
                        DropdownMenu(expanded = langMenuOpen, onDismissRequest = { langMenuOpen = false }) {
                            com.autohub.park.i18n.I18n.languages.forEach { l ->
                                DropdownMenuItem(
                                    text = { Text(l.nativeLabel) },
                                    trailingIcon = if (l.code == com.autohub.park.i18n.I18n.lang) {
                                        { Icon(Icons.Default.Check, contentDescription = null) }
                                    } else null,
                                    onClick = {
                                        langMenuOpen = false
                                        com.autohub.park.i18n.I18n.setLang(langContext, l.code)
                                        // remember{} içinde hesaplanmış metinler de yeni dile geçsin
                                        (langContext as? android.app.Activity)?.recreate()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 0.6 REKLAM ENGELLEYİCİ ---
        item {
            val adContext = LocalContext.current
            val adBlockEnabled by com.autohub.park.ui.browser.WebContentBlocker.enabled.collectAsState()
            val adsBlocked by com.autohub.park.ui.browser.WebContentBlocker.totalBlocked.collectAsState()
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = AutoLoungePalette.CarbonElevated,
                border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = config.theme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("Reklam Engelleyici"), color = AutoLoungePalette.TextPure, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            if (adBlockEnabled) tr("Sitelerdeki reklam ve izleyiciler engelleniyor • Toplam %d engellendi", adsBlocked)
                            else tr("Kapalı. Bir site düzgün açılmıyorsa geçici olarak kapatabilirsiniz."),
                            color = AutoLoungePalette.TextDim,
                            fontSize = 11.5.sp
                        )
                    }
                    Switch(
                        checked = adBlockEnabled,
                        onCheckedChange = { com.autohub.park.ui.browser.WebContentBlocker.setEnabled(adContext, it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = config.theme.primary)
                    )
                }
            }
        }

        // --- 1. SECTION: KOKPİT AMBİYANS & GÖRÜNÜM ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BrightnessMedium, contentDescription = null, tint = config.theme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            tr("Kokpit Ambiyans & Görünüm"),
                            color = AutoLoungePalette.TextPure,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                    Text(
                        tr("SENSÖR HAZIR"),
                        color = AutoLoungePalette.AcidMint,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AutoLoungePalette.CarbonElevated,
                    border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // 3-Segmented Pill Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(30.dp))
                                .background(AutoLoungePalette.SurfaceCard)
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                Triple("system", tr("Sensör"), Icons.Default.HdrAuto),
                                Triple("dark", tr("Gece (Aktif)"), Icons.Default.DarkMode),
                                Triple("light", tr("Gündüz"), Icons.Default.LightMode)
                            ).forEach { (mode, label, icon) ->
                                val isSelected = config.themeMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(if (isSelected) config.theme.primary else Color.Transparent)
                                        .clickable { onSelectThemeMode(mode) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color(0xFF0E131D) else AutoLoungePalette.TextDim,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            label,
                                            color = if (isSelected) Color(0xFF0E131D) else AutoLoungePalette.TextMuted,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = config.theme.primary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                tr("Farlar yandığında otomatik olarak Gece Obsidian moduna geçer."),
                                color = AutoLoungePalette.TextDim,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // --- 2. SECTION: AYDINLATMA & TEMA AKSANI ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = config.theme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            tr("Aydınlatma & Tema Aksanı"),
                            color = AutoLoungePalette.TextPure,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                    Text(
                        "#00E5FF",
                        color = config.theme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AutoLoungePalette.CarbonElevated,
                    border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tr("AURA RENGİ"), color = AutoLoungePalette.TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(tr("%s (Aktif)", tr(config.theme.title)), color = config.theme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // 4 Metallic Accent Swatches
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(
                                Triple(CarVisualTheme.STEALTH_PLATINUM, tr("Platinum"), Color(0xFFE2E8F0)),
                                Triple(CarVisualTheme.ACID_MINT, tr("Cyan"), Color(0xFF00E5FF)),
                                Triple(CarVisualTheme.NORDIC_FROST, tr("Nordic"), Color(0xFF38BDF8)),
                                Triple(CarVisualTheme.WARM_AMBER, tr("Amber"), Color(0xFFF59E0B))
                            ).forEach { (th, name, color) ->
                                val isSelected = config.theme == th
                                Surface(
                                    onClick = { onSelectTheme(th) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) AutoLoungePalette.SurfaceCard else Color.Transparent,
                                    border = BorderStroke(1.dp, if (isSelected) color else AutoLoungePalette.SurfaceBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(2.dp, if (isSelected) Color.White else Color.Transparent, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0E131D), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            name,
                                            color = if (isSelected) color else AutoLoungePalette.TextDim,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // Lümen Yoğunluğu Scrubber
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WbIncandescent, contentDescription = null, tint = AutoLoungePalette.TextDim, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text(tr("LÜMEN YOĞUNLUĞU"), color = AutoLoungePalette.TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                }
                                Text("%${(lumenBrightness * 100).toInt()}", color = config.theme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = lumenBrightness,
                                onValueChange = { lumenBrightness = it },
                                colors = SliderDefaults.colors(
                                    thumbColor = config.theme.primary,
                                    activeTrackColor = config.theme.primary,
                                    inactiveTrackColor = AutoLoungePalette.SurfaceCard
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(tr("%0 (Kapalı)"), color = AutoLoungePalette.TextDim, fontSize = 9.sp)
                                Text(tr("%50 (Konfor)"), color = AutoLoungePalette.TextDim, fontSize = 9.sp)
                                Text(tr("%100 (Ultra Photon)"), color = AutoLoungePalette.TextDim, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- 3. SECTION: KOKPİT UYGULAMA YÖNETİCİSİ ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Apps, contentDescription = null, tint = config.theme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            tr("Kokpit Uygulama Yöneticisi"),
                            color = AutoLoungePalette.TextPure,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                    Text(tr("4 / 5 AKTİF"), color = AutoLoungePalette.TextDim, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AutoLoungePalette.CarbonElevated,
                    border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InteractiveCardToggleRow(
                            title = "YouTube Cinema",
                            subtitle = tr("4K HDR • Otomatik Tam Ekran"),
                            icon = Icons.Default.SmartDisplay,
                            iconColor = Color(0xFFEF4444),
                            checked = config.showYoutube,
                            onCheckedChange = { onToggleCard("youtube", it) }
                        )

                        InteractiveCardToggleRow(
                            title = tr("Canlı TV Hub"),
                            subtitle = tr("IPTV • DVB-T2 Senkronize"),
                            icon = Icons.Default.LiveTv,
                            iconColor = AutoLoungePalette.AcidMint,
                            checked = config.showIptv,
                            onCheckedChange = { onToggleCard("iptv", it) }
                        )

                        InteractiveCardToggleRow(
                            title = tr("Spotify & Müzik"),
                            subtitle = tr("Kayıpsız FLAC • Arka Plan Çalma"),
                            icon = Icons.Default.GraphicEq,
                            iconColor = Color(0xFF1DB954),
                            checked = true,
                            onCheckedChange = {}
                        )

                        InteractiveCardToggleRow(
                            title = "Web Lounge",
                            subtitle = tr("Chromium Tabanlı Tarayıcı"),
                            icon = Icons.Default.Language,
                            iconColor = config.theme.primary,
                            checked = config.showBrowser,
                            onCheckedChange = { onToggleCard("browser", it) }
                        )

                        InteractiveCardToggleRow(
                            title = tr("Telemetri & Motor Verileri"),
                            subtitle = tr("CAN-Bus • OBD-II Teşhis"),
                            icon = Icons.Default.Speed,
                            iconColor = AutoLoungePalette.TextDim,
                            checked = config.showQuickSearch,
                            onCheckedChange = { onToggleCard("quick_search", it) }
                        )

                        // Action Button: + Özel Servis Ekle
                        Surface(
                            onClick = onAddNew,
                            shape = RoundedCornerShape(12.dp),
                            color = AutoLoungePalette.SurfaceCard,
                            border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder),
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = null, tint = config.theme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    tr("Özel Servis Ekle (URL veya APK)"),
                                    color = config.theme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 4. SECTION: DRIVING SAFETY CARD (ECE R43 & DOT) ---
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = AutoLoungePalette.CarbonElevated,
                border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AutoLoungePalette.AcidMint.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = AutoLoungePalette.AcidMint, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(tr("Sürüş Güvenlik Kilidi"), color = AutoLoungePalette.TextPure, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                Text(tr("ECE R43 & DOT Uyumlu"), color = AutoLoungePalette.TextDim, fontSize = 10.5.sp)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = AutoLoungePalette.SurfaceCard,
                            border = BorderStroke(1.dp, AutoLoungePalette.AcidMint.copy(alpha = 0.4f))
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AutoLoungePalette.AcidMint))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    if (isBypassActive) tr("BYPASS") else if (hasAccess) tr("HAZIR") else tr("AKTİF"),
                                    color = if (isBypassActive) AutoLoungePalette.AmberSignal else AutoLoungePalette.AcidMint,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        tr("Trafik kuralları gereği araç hareket halindeyken (D ve R vitesleri) video ekranı kararır, ses arka planda kesintisiz devam eder."),
                        color = AutoLoungePalette.TextMuted,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )

                    // Bypass Row
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AutoLoungePalette.SurfaceCard,
                        border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LockReset, contentDescription = null, tint = AutoLoungePalette.AmberSignal, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(tr("Lounge Bypass Modu"), color = AutoLoungePalette.TextPure, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text(tr("VIP Yolcu Koltuğu (PIN Gerekir)"), color = AutoLoungePalette.TextDim, fontSize = 10.sp)
                                }
                            }
                            Button(
                                onClick = { onSetPassengerBypass(!isBypassActive) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isBypassActive) AutoLoungePalette.CrimsonWarning else AutoLoungePalette.CarbonElevated,
                                    contentColor = if (isBypassActive) Color.White else AutoLoungePalette.AmberSignal
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                border = BorderStroke(1.dp, if (isBypassActive) AutoLoungePalette.CrimsonWarning else AutoLoungePalette.AmberSignal.copy(alpha = 0.5f))
                            ) {
                                Text(if (isBypassActive) tr("BYPASS DEVREDE") else tr("KİLİDİ AÇ"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- 5. SECTION: ÖZEL SİTELER & YER İMLERİ ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    tr("ÖZEL SİTELER & YER İMLERİ (%d)", bookmarks.size),
                    color = AutoLoungePalette.TextDim,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Surface(
                    onClick = onAddNew,
                    shape = RoundedCornerShape(8.dp),
                    color = config.theme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, config.theme.primary.copy(alpha = 0.35f))
                ) {
                    Text(
                        tr("+ Ekle"),
                        color = config.theme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        items(bookmarks) { b ->
            Surface(
                color = AutoLoungePalette.CarbonElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(config.theme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(b.title.take(1).uppercase(), color = config.theme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(b.title, color = AutoLoungePalette.TextPure, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(b.url.replace("https://", ""), color = AutoLoungePalette.TextDim, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (!b.isDefault) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onEditBookmark(b) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = AutoLoungePalette.TextMuted, modifier = Modifier.size(15.dp))
                            }
                            IconButton(onClick = { onDeleteBookmark(b.id) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = AutoLoungePalette.CrimsonWarning, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // --- 6. TELEMETRIC STATUS CHIP FOOTER ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AutoLoungePalette.SurfaceCard)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(config.theme.primary))
                    Spacer(Modifier.width(6.dp))
                    Text(tr("CANBUS: 500 KBPS"), color = AutoLoungePalette.TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = AutoLoungePalette.TextDim, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(tr("OTA ŞİFRELİ (AES-256)"), color = AutoLoungePalette.TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- 7. RESET FACTORY DEFAULTS ---
        item {
            Surface(
                onClick = onResetAll,
                color = AutoLoungePalette.CarbonElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AutoLoungePalette.CrimsonWarning.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = AutoLoungePalette.CrimsonWarning, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Varsayılan Ayarlara Sıfırla"), color = AutoLoungePalette.CrimsonWarning, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun InteractiveCardToggleRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (checked) iconColor.copy(alpha = 0.15f)
                        else AutoLoungePalette.TextDim.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (checked) iconColor else AutoLoungePalette.TextDim,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    color = AutoLoungePalette.TextPure,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    subtitle,
                    color = AutoLoungePalette.TextDim,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = iconColor,
                uncheckedThumbColor = AutoLoungePalette.TextDim,
                uncheckedTrackColor = AutoLoungePalette.SurfaceBorder
            )
        )
    }
}

@Composable
fun AddSiteDialog(
    error: String,
    onSave: (title: String, url: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AutoLoungePalette.CarbonElevated,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                tr("Yeni Web Sitesi / TV Ekle"),
                color = AutoLoungePalette.TextPure,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(tr("Site / TV Adı (Örn: Netflix)")) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AutoLoungePalette.TextPure,
                        unfocusedTextColor = AutoLoungePalette.TextPure,
                        focusedBorderColor = AutoLoungePalette.EmeraldNeon,
                        unfocusedBorderColor = AutoLoungePalette.SurfaceBorder
                    )
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(tr("HTTPS URL Adresi")) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AutoLoungePalette.TextPure,
                        unfocusedTextColor = AutoLoungePalette.TextPure,
                        focusedBorderColor = AutoLoungePalette.EmeraldNeon,
                        unfocusedBorderColor = AutoLoungePalette.SurfaceBorder
                    )
                )
                if (error.isNotEmpty()) {
                    Text(error, color = AutoLoungePalette.CrimsonWarning, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, url) },
                colors = ButtonDefaults.buttonColors(containerColor = AutoLoungePalette.EmeraldNeon),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(tr("Kaydet"), color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("İptal"), color = AutoLoungePalette.TextDim)
            }
        }
    )
}

@Composable
fun EditBookmarkDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var title by remember { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AutoLoungePalette.CarbonElevated,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                tr("Yer İmi İsmini Düzenle"),
                color = AutoLoungePalette.TextPure,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(tr("Başlık")) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AutoLoungePalette.TextPure,
                        unfocusedTextColor = AutoLoungePalette.TextPure,
                        focusedBorderColor = AutoLoungePalette.EmeraldNeon,
                        unfocusedBorderColor = AutoLoungePalette.SurfaceBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title) },
                colors = ButtonDefaults.buttonColors(containerColor = AutoLoungePalette.EmeraldNeon),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(tr("Kaydet"), color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("İptal"), color = AutoLoungePalette.TextDim)
            }
        }
    )
}

@Composable
fun CompactWidgetChip(
    name: String,
    isActive: Boolean,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) color.copy(alpha = 0.14f) else AutoLoungePalette.CarbonElevated,
        border = BorderStroke(
            1.dp,
            if (isActive) color.copy(alpha = 0.6f) else AutoLoungePalette.SurfaceBorder
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) color else AutoLoungePalette.TextDim,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    name,
                    color = if (isActive) AutoLoungePalette.TextPure else AutoLoungePalette.TextDim,
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                )
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isActive) color else Color(0xFF475569))
            )
        }
    }
}

