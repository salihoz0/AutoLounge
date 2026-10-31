package com.autolounge.park.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.autolounge.park.ui.AutoLoungePalette
import java.util.concurrent.ConcurrentHashMap

/**
 * Marka logosu: şeffaf arka planlı ve çoğunlukla koyu renkli logolar (tabii, Apple TV+, Max...)
 * karanlık temada kaybolmasın diye arkalarına açık bir zemin ekler. Analiz logo başına bir kez yapılır.
 */
@Composable
fun AdaptiveLogo(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val isDarkTheme = AutoLoungePalette.IsDark
    val key = model?.toString()
    var needsBacking by remember(key) { mutableStateOf(key?.let { LogoToneCache[it] } ?: false) }

    val request = remember(key) {
        ImageRequest.Builder(context)
            .data(model)
            // Piksel okumak için yazılımsal bitmap gerekir
            .allowHardware(false)
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        onSuccess = { state ->
            if (key != null && !LogoToneCache.containsKey(key)) {
                val bitmap = (state.result.drawable as? BitmapDrawable)?.bitmap
                val dark = bitmap?.let(::isDarkOnTransparent) ?: false
                LogoToneCache[key] = dark
                needsBacking = dark
            }
        },
        modifier = if (isDarkTheme && needsBacking) {
            modifier
                .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(6.dp))
                .padding(3.dp)
        } else {
            modifier
        }
    )
}

private val LogoToneCache = ConcurrentHashMap<String, Boolean>()

/**
 * 32x32 örneklemde: arka planın en az %30'u şeffaf VE opak piksellerin en az yarısı koyuysa true.
 * Kendi dolu zemini olan (YouTube, Exxen) veya renkli (Netflix, CNN) logolar etkilenmez.
 */
private fun isDarkOnTransparent(source: Bitmap): Boolean {
    val sample = Bitmap.createScaledBitmap(source, 32, 32, true)
    var opaque = 0
    var dark = 0
    for (y in 0 until 32) {
        for (x in 0 until 32) {
            val c = sample.getPixel(x, y)
            if ((c ushr 24) <= 128) continue
            opaque++
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            if ((0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0 < 0.22) dark++
        }
    }
    if (sample !== source) sample.recycle()
    if (opaque == 0) return false
    val transparentRatio = 1.0 - opaque / 1024.0
    return transparentRatio >= 0.3 && dark.toDouble() / opaque >= 0.5
}
