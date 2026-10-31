package com.autohub.park.ui.components

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.autohub.park.data.AutoLoungeSyncBridge
import com.autohub.park.data.Bookmark
import com.autohub.park.data.BrandLogos
import com.autohub.park.data.CarConfig
import com.autohub.park.data.NewsChannel
import com.autohub.park.data.NewsChannelsRepository
import com.autohub.park.data.NowPlayingStore
import com.autohub.park.ui.AppFontFamily
import com.autohub.park.ui.AutoLoungePalette
import com.autohub.park.ui.YouTubeLogo
import com.autohub.park.i18n.I18n
import com.autohub.park.i18n.tr

data class LauncherItem(
    val id: String,
    val title: String,
    val icon: ImageVector? = null,
    val iconUrl: String? = null,
    val color: Color,
    val targetUrl: String,
    val isAppMode: Boolean = true,
    val isDeletable: Boolean = false,
    val bookmarkId: Long? = null
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CarDashboardContent(
    config: CarConfig,
    bookmarks: List<Bookmark>,
    remainingSeconds: Int,
    isPremium: Boolean,
    onOpenUrl: (url: String, title: String?, isAppMode: Boolean) -> Unit,
    onOpenPaywall: () -> Unit,
    faviconProvider: (String) -> String,
    onReorder: ((List<String>) -> Unit)? = null,
    onDeleteBookmark: ((Long) -> Unit)? = null,
    onAddBookmark: ((String, String) -> Unit)? = null,
    onToggleAppVisibility: ((appId: String, visible: Boolean) -> Unit)? = null,
    onOpenLocalMedia: (() -> Unit)? = null,
    onLaunchAssistant: (() -> Unit)? = null,
    onOpenLiveTv: ((NewsChannel) -> Unit)? = null,
    isCarScreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    val screenWidthDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    var isEditMode by remember { mutableStateOf(false) }
    var showChannelsSheet by remember { mutableStateOf(false) }
    var showAddAppModal by remember { mutableStateOf(false) }

    val allItems = remember(config, bookmarks) {
        val list = mutableListOf<LauncherItem>()

        if (config.showYoutube) {
            list.add(
                LauncherItem(
                    id = "youtube",
                    title = "YouTube",
                    icon = Icons.Default.PlayArrow,
                    color = Color(0xFFEF4444),
                    targetUrl = "https://m.youtube.com",
                    isAppMode = true,
                    isDeletable = true
                )
            )
        }
        if (config.showBrowser) {
            list.add(
                LauncherItem(
                    id = "browser",
                    title = "Tarayıcı",
                    icon = Icons.Default.Language,
                    color = config.theme.primary,
                    targetUrl = "https://www.google.com",
                    isAppMode = false,
                    isDeletable = true
                )
            )
        }
        if (config.showGemini) {
            list.add(
                LauncherItem(
                    id = "gemini",
                    title = "Sesli Asistan",
                    icon = Icons.Default.AutoAwesome,
                    color = Color(0xFF818CF8),
                    targetUrl = "https://gemini.google.com/app",
                    isAppMode = true,
                    isDeletable = true
                )
            )
        }
        if (config.showQuickSearch) {
            list.add(
                LauncherItem(
                    id = "quick_search",
                    title = "Haber & Arama",
                    icon = Icons.Default.Search,
                    color = AutoLoungePalette.SapphireGlow,
                    targetUrl = "https://www.google.com/search?q=g%C3%BCncel+haberler",
                    isAppMode = false,
                    isDeletable = true
                )
            )
        }
        if (config.showIptv) {
            list.add(
                LauncherItem(
                    id = "iptv",
                    title = "Canlı TV",
                    icon = Icons.Default.LiveTv,
                    color = Color(0xFF10B981),
                    targetUrl = "https://www.trthaber.com/canli-yayin-izle.html",
                    isAppMode = true,
                    isDeletable = true
                )
            )
        }

        // Cihaz Depolaması / Galeri & Video
        list.add(
            LauncherItem(
                id = "local_gallery",
                title = "Galeri & Medya",
                icon = Icons.Default.VideoLibrary,
                color = AutoLoungePalette.AmberSignal,
                targetUrl = "local://gallery",
                isAppMode = true,
                isDeletable = false
            )
        )

        // Özel Yer İmleri & Katalogdan Eklenenler
        val existingUrls = list.map { it.targetUrl.trimEnd('/') }.toSet()
        bookmarks.forEach { b ->
            if (b.url.trimEnd('/') !in existingUrls) {
                list.add(
                    LauncherItem(
                        id = "bookmark_${b.id}",
                        title = b.title,
                        iconUrl = BrandLogos.findLogoByUrlOrTitle(b.title, b.url)
                            ?: NewsChannelsRepository.logoFor(b.title)
                            ?: faviconProvider(b.url),
                        color = Color(0xFF334155),
                        targetUrl = b.url,
                        isAppMode = true,
                        isDeletable = true,
                        bookmarkId = b.id
                    )
                )
            }
        }

        if (config.cardOrder.isNotEmpty()) {
            val orderMap = config.cardOrder.withIndex().associate { it.value to it.index }
            list.sortedBy { orderMap[it.id] ?: 999 }
        } else {
            list
        }
    }

    var orderedItems by remember(allItems) { mutableStateOf(allItems) }

    fun swapItems(fromIndex: Int, toIndex: Int) {
        if (fromIndex in orderedItems.indices && toIndex in orderedItems.indices && fromIndex != toIndex) {
            val updated = orderedItems.toMutableList()
            val temp = updated[fromIndex]
            updated[fromIndex] = updated[toIndex]
            updated[toIndex] = temp
            orderedItems = updated
            onReorder?.invoke(updated.map { it.id })
        }
    }

    val jiggleRotation = if (isEditMode) {
        val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
        val rot by infiniteTransition.animateFloat(
            initialValue = -1.2f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(140, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "rotation"
        )
        rot
    } else {
        0f
    }

    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember { (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1) }
    var currentVolume by remember {
        mutableStateOf(audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVolume * 0.7f).toInt())
    }
    val nowTitle by NowPlayingStore.title.collectAsState()
    val nowSub by NowPlayingStore.subtitle.collectAsState()
    val isPlaying by NowPlayingStore.isPlaying.collectAsState()

    fun launchQuickChannel(keyword: String) {
        val matched = NewsChannelsRepository.allChannels.firstOrNull {
            it.title.contains(keyword, ignoreCase = true)
        }
        if (matched != null) {
            if (onOpenLiveTv != null) {
                onOpenLiveTv(matched)
            } else {
                onOpenUrl(matched.audioStreamUrl ?: matched.url, matched.title, true)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AutoLoungePalette.ObsidianVoid),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. MEDIA LAUNCHER STRIP (HORIZONTAL CAPSULES) ---
        item(key = "media_capsules") {
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Triple("YOUTUBE", Color(0xFFFF0033), "https://m.youtube.com"),
                    Triple("CANLI TV", AutoLoungePalette.AcidMint, "tv://hub"),
                    Triple("TABİİ", Color(0xFF00E5FF), "https://www.tabii.com"),
                    Triple("NETFLIX", Color(0xFFE50914), "https://www.netflix.com"),
                    Triple("PRIME", Color(0xFF00A8E1), "https://www.primevideo.com"),
                    Triple("TWITCH", Color(0xFF9146FF), "https://www.twitch.tv"),
                    Triple("DISNEY+", Color(0xFF113CCF), "https://www.disneyplus.com"),
                    Triple("SPOTIFY", Color(0xFF1DB954), "https://open.spotify.com")
                ).forEach { (label, dotColor, target) ->
                    Surface(
                        onClick = {
                            if (target == "tv://hub") {
                                showChannelsSheet = true
                            } else {
                                onOpenUrl(target, label, true)
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = AutoLoungePalette.CarbonElevated,
                        border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                tr(label),
                                color = AutoLoungePalette.TextPure,
                                fontSize = 11.sp,
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }

        // Araç ekranı yatay ve alçak: büyük kartlar uygulamaları ekran dışına itiyor.
        // Canlı TV üst şeritte, ses ise aracın kendi kontrolünde; bu yüzden araçta gizlenir.
        if (!isCarScreen) {
        // --- 2. HERO CINEMA DECK (16:9 MEDIA CANVAS CARD) ---
        item(key = "hero_cinema_deck") {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = AutoLoungePalette.CarbonElevated,
                border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOpenUrl("https://m.youtube.com", "YouTube", true)
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    config.theme.primary.copy(alpha = 0.16f),
                                    AutoLoungePalette.CarbonElevated
                                ),
                                radius = 600f
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Meta Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AutoLoungePalette.SurfaceCard.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.SmartDisplay,
                                        contentDescription = null,
                                        tint = Color(0xFFFF0033),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        tr("YOUTUBE SİNEMA"),
                                        color = AutoLoungePalette.TextPure,
                                        fontSize = 10.sp,
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = config.theme.primary.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, config.theme.primary.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    tr("4K HDR • KOKPİT"),
                                    color = config.theme.primary,
                                    fontSize = 10.sp,
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Middle Content Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    if (nowTitle.isNotBlank()) nowTitle else tr("Araç İçi Sinema & Video Lounge"),
                                    color = AutoLoungePalette.TextPure,
                                    fontSize = 16.sp,
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    if (nowSub.isNotBlank()) nowSub else tr("YouTube, Canlı Yayınlar ve Dijital Medya"),
                                    color = AutoLoungePalette.TextDim,
                                    fontSize = 12.sp,
                                    fontFamily = AppFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Circular glowing play button
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(config.theme.primary, config.theme.secondary)
                                        )
                                    )
                                    .clickable {
                                        onOpenUrl("https://m.youtube.com", "YouTube", true)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = tr("Oynat"),
                                    tint = Color(0xFF07090E),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        // Bottom hairline progress indicator
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(AutoLoungePalette.SurfaceBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(if (isPlaying) 0.65f else 0.35f)
                                    .background(config.theme.primary)
                            )
                        }
                    }
                }
            }
        }

        // --- 3. 2-COLUMN SPLIT MODULE: CANLI TV & MASTER SES KONTROLÜ ---
        item(key = "split_tv_audio") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // TILE A: Canlı TV Hub
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AutoLoungePalette.CarbonElevated,
                    border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showChannelsSheet = true }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LiveTv,
                                    contentDescription = null,
                                    tint = config.theme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    tr("Canlı TV"),
                                    color = AutoLoungePalette.TextPure,
                                    fontSize = 13.sp,
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    tr("CANLI"),
                                    color = Color(0xFFEF4444),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // Equalizer & channel name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AutoLoungePalette.SurfaceCard)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                tr("TRT Haber HD"),
                                color = AutoLoungePalette.TextPure,
                                fontSize = 11.5.sp,
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 4.dp)
                            )
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                repeat(3) { i ->
                                    val h = listOf(6, 12, 8)[i]
                                    Box(
                                        Modifier
                                            .width(2.5.dp)
                                            .height(h.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(config.theme.primary)
                                    )
                                }
                            }
                        }

                        // 3 Quick Channel Switch Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("TRT", "BLOOM", "NTV").forEach { chName ->
                                Surface(
                                    onClick = { launchQuickChannel(if (chName == "BLOOM") "BLOOMBERG" else chName) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = AutoLoungePalette.SurfaceCard,
                                    border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = chName,
                                        color = AutoLoungePalette.TextDim,
                                        fontSize = 9.sp,
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // TILE B: Dahili Ses & Araç Çıkışı
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AutoLoungePalette.CarbonElevated,
                    border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.SurroundSound,
                                    contentDescription = null,
                                    tint = AutoLoungePalette.AmberSignal,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    tr("Ses Seviyesi"),
                                    color = AutoLoungePalette.TextPure,
                                    fontSize = 13.sp,
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            val pct = ((currentVolume.toFloat() / maxVolume.toFloat()) * 100).toInt()
                            Text(
                                "$pct%",
                                color = config.theme.primary,
                                fontSize = 11.sp,
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Real Interactive Volume Slider
                        Slider(
                            value = currentVolume.toFloat(),
                            onValueChange = { newVal ->
                                val v = newVal.toInt().coerceIn(0, maxVolume)
                                currentVolume = v
                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0)
                            },
                            valueRange = 0f..maxVolume.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = config.theme.primary,
                                activeTrackColor = config.theme.primary,
                                inactiveTrackColor = AutoLoungePalette.SurfaceBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                        )

                        // Mode indicator
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AutoLoungePalette.SurfaceCard)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                tr("Bluetooth / AUX"),
                                color = AutoLoungePalette.TextDim,
                                fontSize = 10.sp,
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = AutoLoungePalette.TextDim,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
        }

        // --- 4. UYGULAMALAR & YER İMLERİ TOOLBAR HEADER ---
        item(key = "apps_toolbar") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Apps,
                        contentDescription = null,
                        tint = config.theme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        tr("Uygulamalar"),
                        color = AutoLoungePalette.TextPure,
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.3.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "(${orderedItems.size})",
                        color = AutoLoungePalette.TextDim,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // "+ Ekle" button
                    Surface(
                        onClick = { showAddAppModal = true },
                        shape = RoundedCornerShape(12.dp),
                        color = AutoLoungePalette.CarbonElevated,
                        border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = config.theme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                tr("Ekle"),
                                color = AutoLoungePalette.TextPure,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // "Düzenle / Bitti" button
                    Surface(
                        onClick = { isEditMode = !isEditMode },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isEditMode) config.theme.primary else AutoLoungePalette.CarbonElevated,
                        border = BorderStroke(
                            1.dp,
                            if (isEditMode) config.theme.primary else AutoLoungePalette.SurfaceBorder
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = null,
                                tint = if (isEditMode) Color(0xFF07090E) else AutoLoungePalette.TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                if (isEditMode) tr("Bitti") else tr("Düzenle"),
                                color = if (isEditMode) Color(0xFF07090E) else AutoLoungePalette.TextPure,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // --- 5. CHUNKED OEM APPS & BOOKMARKS GRID ---
        // Araçta sütun sayısı ekran genişliğinden gelir (~130dp/simge): 800dp → 6, 1280dp → 9
        val columns = if (isCarScreen) (screenWidthDp / 130).coerceIn(4, 9) else config.gridColumns.coerceIn(3, 5)
        val chunkedItems = orderedItems.chunked(columns)
        chunkedItems.forEach { rowItems ->
            item(key = rowItems.map { it.id }.joinToString("_")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    rowItems.forEach { item ->
                        val index = orderedItems.indexOf(item)
                        Box(modifier = Modifier.weight(1f)) {
                            DraggableAppItem(
                                item = item,
                                index = index,
                                isEditMode = isEditMode,
                                rotation = if (isEditMode) jiggleRotation else 0f,
                                onSwapWith = { targetIndex -> swapItems(index, targetIndex) },
                                onDeleteOrHide = {
                                    if (item.bookmarkId != null) {
                                        onDeleteBookmark?.invoke(item.bookmarkId)
                                    } else {
                                        onToggleAppVisibility?.invoke(item.id, false)
                                    }
                                },
                                onClick = {
                                    if (!isEditMode) {
                                        if (item.id == "iptv") {
                                            showChannelsSheet = true
                                        } else if (item.id == "local_gallery") {
                                            onOpenLocalMedia?.invoke()
                                        } else if (item.id == "gemini") {
                                            onLaunchAssistant?.invoke()
                                        } else {
                                            val matchedBookmark = bookmarks.firstOrNull { it.id == item.bookmarkId || it.url == item.targetUrl }
                                            val isTv = matchedBookmark?.type == "tv" || AutoLoungeSyncBridge.isDirectMediaStream(item.targetUrl)
                                            if (isTv) {
                                                val matched = NewsChannelsRepository.allChannels.firstOrNull {
                                                    it.title.equals(item.title, ignoreCase = true) ||
                                                        it.url == item.targetUrl ||
                                                        it.audioStreamUrl == item.targetUrl
                                                } ?: NewsChannel(
                                                    id = item.id,
                                                    title = item.title,
                                                    url = item.targetUrl,
                                                    audioStreamUrl = if (AutoLoungeSyncBridge.isDirectMediaStream(item.targetUrl)) item.targetUrl else null,
                                                    countryCode = "TR",
                                                    countryName = "Türkiye",
                                                    category = "Canlı TV"
                                                )
                                                if (onOpenLiveTv != null) {
                                                    onOpenLiveTv(matched)
                                                } else {
                                                    onOpenUrl(item.targetUrl, item.title, item.isAppMode)
                                                }
                                            } else {
                                                onOpenUrl(item.targetUrl, item.title, item.isAppMode)
                                            }
                                        }
                                    }
                                },
                                onLongClick = { isEditMode = true }
                            )
                        }
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        item(key = "bottom_spacer") {
            Spacer(Modifier.height(if (isCarScreen) 8.dp else 72.dp))
        }
    }

    if (showChannelsSheet) {
        LiveTvChannelsSheet(
            bookmarks = bookmarks,
            onSelectChannel = { ch ->
                showChannelsSheet = false
                if (onOpenLiveTv != null) {
                    onOpenLiveTv(ch)
                } else {
                    onOpenUrl(ch.url, ch.title, true)
                }
            },
            onPinChannel = { title, url ->
                onAddBookmark?.invoke(title, url)
            },
            onDismiss = { showChannelsSheet = false }
        )
    }

    // Catalog and custom bookmark modal
    if (showAddAppModal) {
        AddAppModal(
            currentBookmarks = bookmarks,
            faviconProvider = faviconProvider,
            onAddCatalogItem = { catalogItem ->
                onAddBookmark?.invoke(catalogItem.title, catalogItem.url)
            },
            onRemoveCatalogItem = { targetUrl ->
                val targetClean = targetUrl.trimEnd('/')
                val existing = bookmarks.find { it.url.trimEnd('/') == targetClean }
                if (existing != null) {
                    onDeleteBookmark?.invoke(existing.id)
                }
            },
            onAddCustomUrl = { title, url ->
                onAddBookmark?.invoke(title, url)
            },
            onDismiss = { showAddAppModal = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableAppItem(
    item: LauncherItem,
    index: Int,
    isEditMode: Boolean,
    rotation: Float,
    onSwapWith: (Int) -> Unit,
    onDeleteOrHide: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var dragAccumulator by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .rotate(if (isEditMode) rotation else 0f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .pointerInput(isEditMode) {
                if (isEditMode) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragAccumulator += dragAmount

                            if (dragAccumulator.x > 60f) {
                                onSwapWith(index + 1)
                                dragAccumulator = Offset.Zero
                            } else if (dragAccumulator.x < -60f) {
                                onSwapWith(index - 1)
                                dragAccumulator = Offset.Zero
                            }
                        },
                        onDragEnd = { dragAccumulator = Offset.Zero },
                        onDragCancel = { dragAccumulator = Offset.Zero }
                    )
                }
            }
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Automotive squircle icon container with smoked glass finish
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                AutoLoungePalette.SurfaceCard,
                                AutoLoungePalette.CarbonElevated
                            )
                        )
                    )
                    .border(
                        1.2.dp,
                        if (isEditMode) AutoLoungePalette.AcidMint else AutoLoungePalette.SurfaceBorder,
                        RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (item.id == "youtube") {
                    YouTubeLogo(sizeDp = 30)
                } else if (!item.iconUrl.isNullOrBlank()) {
                    AdaptiveLogo(
                        model = item.iconUrl,
                        contentDescription = tr(item.title),
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else if (item.icon != null) {
                    Icon(
                        item.icon,
                        contentDescription = tr(item.title),
                        tint = item.color,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Text(
                        item.title.firstOrNull()?.uppercase() ?: "W",
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = item.color
                    )
                }
            }

            // High-legibility typography
            Text(
                text = tr(item.title),
                color = AutoLoungePalette.TextPure,
                fontSize = 12.5.sp,
                fontFamily = AppFontFamily,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
            )
        }

        // Edit mode delete/hide badge
        if (isEditMode && item.isDeletable) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(AutoLoungePalette.CrimsonWarning)
                    .clickable { onDeleteOrHide() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = tr("Kaldır"),
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
