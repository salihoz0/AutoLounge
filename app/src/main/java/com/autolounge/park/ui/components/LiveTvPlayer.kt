package com.autohub.park.ui.components

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.autohub.park.audio.LivePlayerFactory
import com.autohub.park.audio.LivePlayerFactory.liveIf
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.autohub.park.data.NewsChannel
import com.autohub.park.data.NewsChannelsRepository
import com.autohub.park.ui.AutoLoungePalette
import kotlinx.coroutines.delay
import com.autohub.park.i18n.tr

/**
 * AutoLounge Mobil Canlı TV Oynatıcısı.
 * Web sitelerine gitmeden, kanalların doğrudan HLS/DASH video akışını
 * yerel donanım hızlandırmalı ExoPlayer motoruyla tam ekran oynatır.
 */
@OptIn(UnstableApi::class)
@Composable
fun LiveTvPlayer(
    channel: NewsChannel,
    isParked: Boolean = true,
    isPassengerBypass: Boolean = false,
    onPassengerBypass: (() -> Unit)? = null,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val streamUrl = remember(channel) {
        channel.audioStreamUrl ?: channel.url
    }

    val isRadio = remember(channel) {
        channel.category.equals("Radyo", ignoreCase = true) ||
        channel.kind == "radio" ||
        channel.title.contains("FM", ignoreCase = true) ||
        channel.title.contains("Radyo", ignoreCase = true)
    }

    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    // Geri tuşu ile oynatıcıyı kapatma
    BackHandler(onBack = onClose)

    // Kontrollerin 4 saniye sonra otomatik gizlenmesi (radyo modunda açık kalır)
    LaunchedEffect(controlsVisible, isPlaying) {
        if (!isRadio && controlsVisible && isPlaying) {
            delay(4000)
            controlsVisible = false
        }
    }

    // ExoPlayer Yaşam Döngüsü (Audio Focus & Ducking aktif)
    DisposableEffect(streamUrl) {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 10000,
                /* maxBufferMs = */ 40000,
                /* bufferForPlaybackMs = */ 1200,
                /* bufferForPlaybackAfterRebufferMs = */ 2000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(5000, true)
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(if (isRadio) C.AUDIO_CONTENT_TYPE_MUSIC else C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(LivePlayerFactory.mediaSourceFactory(context))
            .setAudioAttributes(audioAttributes, true)
            .setLoadControl(loadControl)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build().apply {
                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(streamUrl))
                    .liveIf(LivePlayerFactory.isLiveUrl(streamUrl))
                    .build()
                setMediaItem(mediaItem)
                playWhenReady = true
                prepare()
            }

        var autoRetried = false
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    autoRetried = false
                    hasError = false
                    errorMessage = null
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    player.seekToDefaultPosition()
                    player.prepare()
                    return
                }
                if (!autoRetried && LivePlayerFactory.isNetworkError(error.errorCode)) {
                    autoRetried = true
                    player.prepare()
                    return
                }
                hasError = true
                isBuffering = false
                errorMessage = tr("Yayın başlatılamadı (%s). Tekrar deneyin.", error.errorCodeName)
            }
        }

        player.addListener(listener)
        exoPlayer = player

        onDispose {
            player.removeListener(listener)
            player.stop()
            player.release()
            exoPlayer = null
        }
    }

    val isLockoutActive = !isRadio && !isParked && !isPassengerBypass

    // Sürüş güvenliği: hareket halindeyse ve yolcu onayı yoksa video izini kapat, sesi sürdür
    LaunchedEffect(isLockoutActive, exoPlayer) {
        exoPlayer?.let { p ->
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, isLockoutActive)
                .build()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isRadio) controlsVisible = !controlsVisible
            }
    ) {
        if (isRadio) {
            // ==========================================
            // 📻 CANLI RADYO / MP3 SES OYNATICI ARAYÜZÜ
            // ==========================================
            val infiniteTransition = rememberInfiniteTransition(label = "radio_wave")
            val wavePhase by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "wave"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0F172A),
                                AutoLoungePalette.ObsidianVoid,
                                Color(0xFF080B11)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Radyo Logosu / Dönen Plak Halesi
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(170.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        AutoLoungePalette.EmeraldNeon.copy(alpha = 0.22f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF141A24),
                            border = BorderStroke(2.dp, AutoLoungePalette.EmeraldNeon.copy(alpha = 0.6f)),
                            modifier = Modifier.size(136.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val logo = NewsChannelsRepository.logoFor(channel.title) ?: channel.logoUrl
                                if (!logo.isNullOrBlank()) {
                                    AsyncImage(
                                        model = logo,
                                        contentDescription = channel.title,
                                        modifier = Modifier.size(86.dp).clip(CircleShape)
                                    )
                                } else {
                                    Text("📻", fontSize = 48.sp)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = channel.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(Modifier.height(6.dp))

                    Surface(
                        color = AutoLoungePalette.EmeraldNeon.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AutoLoungePalette.EmeraldNeon.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = tr("● CANLI RADYO • MP3/AAC HD"),
                            color = AutoLoungePalette.EmeraldNeon,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(28.dp))

                    // Animasyonlu Ekolayzer Dalgaları
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.height(36.dp)
                    ) {
                        val heights = listOf(0.4f, 0.85f, 0.55f, 1.0f, 0.7f, 0.9f, 0.5f, 0.75f)
                        heights.forEachIndexed { i, h ->
                            val dynamicH = (h * (if (isPlaying) wavePhase * 0.5f + 0.5f else 0.2f)).coerceIn(0.15f, 1f)
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .width(4.5.dp)
                                    .fillMaxHeight(dynamicH)
                                    .background(
                                        if (i % 2 == 0) AutoLoungePalette.EmeraldNeon else Color(0xFF38BDF8),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // Radyo Kontrolleri (Play/Pause, Ses)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = {
                                val next = !isMuted
                                isMuted = next
                                exoPlayer?.volume = if (next) 0f else 1f
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        ) {
                            Icon(
                                if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = tr("Ses"),
                                tint = Color.White
                            )
                        }

                        Spacer(Modifier.width(20.dp))

                        // Ana Oynat / Duraklat Butonu
                        IconButton(
                            onClick = {
                                exoPlayer?.let { p ->
                                    if (p.isPlaying) p.pause() else p.play()
                                }
                            },
                            modifier = Modifier
                                .size(68.dp)
                                .background(AutoLoungePalette.EmeraldNeon, CircleShape)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) tr("Duraklat") else tr("Oynat"),
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(Modifier.width(20.dp))

                        IconButton(
                            onClick = {
                                exoPlayer?.seekToDefaultPosition()
                                exoPlayer?.prepare()
                                exoPlayer?.play()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = tr("Yenile"), tint = Color.White)
                        }
                    }
                }
            }
        } else {
            // Video Yüzeyi (ExoPlayer)
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = exoPlayer
                        useController = false
                        this.resizeMode = resizeMode
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                update = { pView ->
                    pView.player = exoPlayer
                    pView.resizeMode = resizeMode
                },
                modifier = Modifier.fillMaxSize()
            )

            // Sürüş Güvenliği Bariyeri (Yolcuyum & Tamam butonları)
            if (isLockoutActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AutoLoungePalette.ObsidianVoid.copy(alpha = 0.95f)),
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
                            text = tr("Lütfen sürüş esnasında sürücü olarak video izlemeyiniz.\nTrafik ve can güvenliğiniz her şeyden önemlidir.\nCanlı ses kesintisiz olarak çalmaya devam ediyor."),
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
                            Button(
                                onClick = { /* Bilgilendirme kabul edildi, video kapalı ses açık */ },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2430)),
                                shape = RoundedCornerShape(22.dp),
                                border = BorderStroke(1.dp, Color(0xFF333D52)),
                                modifier = Modifier.height(46.dp)
                            ) {
                                Text(tr("Tamam (Ses Devam Etsin)"), color = Color(0xFFE2E8F0), fontSize = 12.5.sp)
                            }
                            Spacer(Modifier.width(12.dp))
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

        // Yükleniyor / Arabelleğe Alınıyor Göstergesi
        if (isBuffering && !hasError) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xAA0B0D13),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = AutoLoungePalette.EmeraldNeon,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = tr("Yayın bağlanıyor..."),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Hata Bildirimi ve Yeniden Dene Butonu
        if (hasError) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xDD161820),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44EF4444)),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⚠️", fontSize = 32.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: tr("Yayın alınamadı"),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        IconButton(
                            onClick = {
                                hasError = false
                                isBuffering = true
                                exoPlayer?.apply {
                                    setMediaItem(MediaItem.fromUri(Uri.parse(streamUrl)))
                                    prepare()
                                    play()
                                }
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AutoLoungePalette.EmeraldNeon.copy(alpha = 0.15f))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = tr("Yeniden Dene"), tint = AutoLoungePalette.EmeraldNeon)
                                Spacer(Modifier.width(8.dp))
                                Text(tr("Yeniden Dene"), color = AutoLoungePalette.EmeraldNeon, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- ÜST KONTROL ÇUBUĞU (FROSTED GLASS) ---
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xE60B0D13),
                                Color(0x990B0D13),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Sol: Kapatma Butonu ve Kanal Bilgisi
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = Color(0x331F242E),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            IconButton(onClick = onClose) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = tr("Kapat"),
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        // Kanal Logosu
                        val logoUrl = remember(channel) {
                            NewsChannelsRepository.logoFor(channel.title) ?: channel.logoUrl
                        }
                        if (!logoUrl.isNullOrBlank()) {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(36.dp)
                            ) {
                                AdaptiveLogo(
                                    model = logoUrl,
                                    contentDescription = channel.title,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = channel.title,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(8.dp))
                                // Canlı Yayın İndikatörü
                                Surface(
                                    color = Color(0x22EF4444),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66EF4444))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEF4444))
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = tr("CANLI"),
                                            color = Color(0xFFEF4444),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${tr(channel.countryName)} • ${tr(channel.category)}",
                                color = AutoLoungePalette.TextDim,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Sağ: Boyutlandırma / Fit Butonu
                    IconButton(
                        onClick = {
                            resizeMode = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x331F242E))
                            .size(38.dp)
                    ) {
                        Text(
                            text = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "16:9"
                                AspectRatioFrameLayout.RESIZE_MODE_FILL -> tr("DOLU")
                                else -> "FIT"
                            },
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // --- ALT KONTROL ÇUBUĞU (PLAY/PAUSE & SES) ---
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0x990B0D13),
                                Color(0xE60B0D13)
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sessize Al / Aç
                    IconButton(
                        onClick = {
                            val newMuted = !isMuted
                            isMuted = newMuted
                            exoPlayer?.volume = if (newMuted) 0f else 1f
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x331F242E))
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = tr("Ses"),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(Modifier.width(28.dp))

                    // Oynat / Duraklat
                    IconButton(
                        onClick = {
                            exoPlayer?.let { p ->
                                if (p.isPlaying) p.pause() else p.play()
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(AutoLoungePalette.EmeraldNeon)
                            .size(54.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) tr("Duraklat") else tr("Oynat"),
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(Modifier.width(28.dp))

                    // Yeniden Yükle / Senkronize Et
                    IconButton(
                        onClick = {
                            isBuffering = true
                            hasError = false
                            exoPlayer?.apply {
                                stop()
                                setMediaItem(MediaItem.fromUri(Uri.parse(streamUrl)))
                                prepare()
                                play()
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x331F242E))
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = tr("Yenile"),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
