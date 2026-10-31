package com.autolounge.park.ui

import androidx.compose.foundation.Canvas
import com.autolounge.park.i18n.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AutoLounge Luxury Cockpit Tachometer & Play Emblem (Option 1A)
 * Sweeping clockwise left-to-right power arc and central multimedia lounge core.
 */
@Composable
fun AppLogo(
    size: Int = 42,
    accentColor: Color = AutoLoungePalette.EmeraldNeon,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.28f).dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF161C28),
                        Color(0xFF0E121B),
                        Color(0xFF07090F)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color(0x5594A3B8),
                        accentColor.copy(alpha = 0.4f),
                        Color(0x2238BDF8)
                    )
                ),
                RoundedCornerShape((size * 0.28f).dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size((size * 0.74f).dp)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f
            val strokeW = w * 0.11f
            val arcPadding = strokeW / 2f + w * 0.04f
            val arcSize = Size(w - arcPadding * 2, h - arcPadding * 2)
            val arcTopLeft = Offset(arcPadding, arcPadding)

            // 1. Inactive Base Tachometer Arc (270 degrees sweep, dark titanium)
            drawArc(
                color = Color(0xFF1E2738),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // 2. Active Power Arc (Sweeping Clockwise Left-to-Right: 135 deg to 335 deg)
            drawArc(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF38BDF8),
                        Color(0xFF34D399),
                        Color(0xFF10B981)
                    ),
                    start = Offset(0f, h),
                    end = Offset(w, 0f)
                ),
                startAngle = 135f,
                sweepAngle = 200f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )

            // 3. Peak Active Indicator Bead (at ~335 degrees / right apex)
            val r = (w - arcPadding * 2) / 2f
            val angleRad = Math.toRadians(335.0)
            val beadX = (cx + r * Math.cos(angleRad)).toFloat()
            val beadY = (cy + r * Math.sin(angleRad)).toFloat()
            drawCircle(
                color = Color.White,
                radius = strokeW * 0.28f,
                center = Offset(beadX, beadY)
            )

            // 4. Center Hub Disc
            drawCircle(
                color = Color(0xFF121824),
                radius = w * 0.26f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xFF2B364D),
                radius = w * 0.26f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )

            // 5. Center Floating Media Play Core
            val play = Path().apply {
                val pw = w * 0.22f
                val ph = h * 0.24f
                val left = cx - pw * 0.40f
                val right = cx + pw * 0.60f
                val top = cy - ph * 0.50f
                val bottom = cy + ph * 0.50f
                moveTo(left, top)
                lineTo(right, cy)
                lineTo(left, bottom)
                close()
            }
            drawPath(
                path = play,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF34D399), Color(0xFF059669)),
                    start = Offset(cx - w * 0.1f, cy - h * 0.1f),
                    end = Offset(cx + w * 0.1f, cy + h * 0.1f)
                )
            )
        }
    }
}

@Composable
fun CockpitPill(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AutoLoungePalette.EmeraldNeon.copy(alpha = 0.12f))
            .border(1.dp, AutoLoungePalette.EmeraldNeon.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(AutoLoungePalette.EmeraldNeon)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            tr("CANLI KOKPİT"),
            color = AutoLoungePalette.EmeraldNeon,
            fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp,
            letterSpacing = 0.8.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun YouTubeLogo(modifier: Modifier = Modifier, sizeDp: Int = 32) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = com.autolounge.park.R.drawable.ic_youtube),
        contentDescription = "YouTube",
        modifier = modifier.size(sizeDp.dp)
    )
}
