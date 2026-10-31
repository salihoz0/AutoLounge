package com.autohub.park.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autohub.park.i18n.tr
import com.autohub.park.ui.AutoLoungePalette

/**
 * Google Gemini AI Assistant Integration
 * Dual-Mode: 100% Adaptive to Light & Dark luxury cockpits
 */
@Composable
fun GeminiCard(
    onOpenGeminiWeb: (url: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = AutoLoungePalette.IsDark

    fun launchDeviceGeminiAssistant() {
        val intent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            onOpenGeminiWeb("https://gemini.google.com/app")
        }
    }

    val cardBackground = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color(0xFF161536),
                Color(0xFF0F172A),
                Color(0xFF0B0F19)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF8FAFC),
                Color(0xFFEEF2FF)
            )
        )
    }

    val cardBorder = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color(0xFF818CF8).copy(alpha = 0.5f),
                Color(0xFF38BDF8).copy(alpha = 0.3f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color(0xFF818CF8).copy(alpha = 0.4f),
                Color(0xFFCBD5E1)
            )
        )
    }

    val accentIndigo = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onOpenGeminiWeb("https://gemini.google.com/app") },
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(cardBackground)
                .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Row: Icon + Title + Voice Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Gemini Icon Badge
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            accentIndigo,
                                            Color(0xFF38BDF8)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "Gemini",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Google Gemini AI",
                                    color = AutoLoungePalette.TextPure,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = accentIndigo.copy(alpha = if (isDark) 0.2f else 0.1f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.5.dp,
                                        accentIndigo.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Text(
                                        tr("Kendi Hesabın"),
                                        color = accentIndigo,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(3.dp))

                            Text(
                                tr("Araç içi akıllı sesli ve yazılı yapay zeka asistanı"),
                                color = AutoLoungePalette.TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    // Voice Command Floating Pill
                    Surface(
                        onClick = { launchDeviceGeminiAssistant() },
                        shape = CircleShape,
                        color = accentIndigo.copy(alpha = if (isDark) 0.18f else 0.12f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            accentIndigo.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = tr("Sesli Komut"),
                                tint = accentIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Bottom Quick Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { launchDeviceGeminiAssistant() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentIndigo,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Sesli Konuş"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onOpenGeminiWeb("https://gemini.google.com/app") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AutoLoungePalette.TextPure
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Web'de Aç"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
