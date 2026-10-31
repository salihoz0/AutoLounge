package com.autohub.park.ui.components

import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.autohub.park.ui.AutoLoungePalette
import com.autohub.park.i18n.tr

/**
 * Cihazın yerel depolamasından (galeri / dosya yöneticisi) seçilen
 * video ve fotoğrafları tam ekran kokpit ortamında oynatıcı.
 */
@OptIn(UnstableApi::class)
@Composable
fun LocalMediaViewer(
    mediaUri: Uri,
    isParked: Boolean = true,
    isPassengerBypass: Boolean = false,
    onPassengerBypass: (() -> Unit)? = null,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val mimeType = remember(mediaUri) { contentResolver.getType(mediaUri) ?: "" }
    val isVideo = remember(mimeType) { mimeType.startsWith("video/") || mediaUri.toString().contains("video") }

    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(mediaUri) {
        if (isVideo) {
            val player = ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(mediaUri))
                prepare()
                playWhenReady = true
            }
            exoPlayer = player
        }

        onDispose {
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isVideo) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = exoPlayer
                        useController = true
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Driving safety barrier for local video
            if (!isParked && !isPassengerBypass) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xF00A0B0E)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(28.dp)
                    ) {
                        Text("🛡️", fontSize = 32.sp)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = tr("Sürüş Güvenliği Uyarısı"),
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = tr("Lütfen sürüş esnasında video izlemeyiniz.\nSes çalmaya devam ediyor."),
                            color = AutoLoungePalette.TextDim,
                            fontSize = 13.5.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { /* Dinleme modu */ },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2430)),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
                            ) {
                                Text(tr("Tamam (Ses Devam Etsin)"), color = Color(0xFFE2E8F0))
                            }
                            Spacer(Modifier.width(12.dp))
                            Button(
                                onClick = { onPassengerBypass?.invoke() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
                            ) {
                                Text(tr("Yolcuyum (Videoyu Aç)"), color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // Görsel / Fotoğraf Gösterici
            AsyncImage(
                model = mediaUri,
                contentDescription = tr("Yerel Fotoğraf"),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        }

        // Kapat Butonu
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopEnd)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = tr("Kapat"),
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
