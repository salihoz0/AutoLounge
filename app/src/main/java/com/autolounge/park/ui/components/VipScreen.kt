package com.autohub.park.ui.components

import android.content.ActivityNotFoundException
import com.autohub.park.i18n.tr
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autohub.park.BuildConfig
import com.autohub.park.data.LicenseManager
import com.autohub.park.data.NewsChannelsRepository
import com.autohub.park.data.UsageSecurityManager
import com.autohub.park.ui.AutoLoungePalette
import java.util.Locale

/** Ödeme sitede yapılır; uygulama yalnızca yaklaşık fiyatı gösterir (kesin fiyat ülkeye göre sitede). */
private val EU_COUNTRIES = setOf(
    "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU", "IE",
    "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE"
)

private fun displayPrice(): String {
    val country = NewsChannelsRepository.deviceCountry().takeIf { it != "GLOBAL" }
        ?: Locale.getDefault().country.uppercase(Locale.ROOT)
    return when {
        country == "TR" -> "199 ₺"
        country in EU_COUNTRIES -> "€11.99"
        else -> "$12.99"
    }
}

private fun openPurchasePage(context: Context) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PURCHASE_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: ActivityNotFoundException) {
    }
}

private fun maskKey(key: String): String =
    if (key.length > 4) "ALNG-••••-••••-" + key.takeLast(4) else key

/**
 * VIP Lisans sekmesi: durum → satın alma → anahtar etkinleştirme → destek.
 */
@Composable
fun MembershipScreen(
    remainingSeconds: Int,
    isPremium: Boolean,
    license: LicenseManager.LicenseInfo?,
    accent: Color,
    onActivate: (key: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onDeactivate: (onResult: (Boolean, String) -> Unit) -> Unit,
    onResendKey: (email: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onSimulatePremium: (Boolean) -> Unit,
    onResetTrial: () -> Unit
) {
    val context = LocalContext.current
    var showDeactivateConfirm by remember { mutableStateOf(false) }
    var showResendDialog by remember { mutableStateOf(false) }
    var deactivateMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { StatusCard(remainingSeconds, isPremium, license, onRemove = { showDeactivateConfirm = true }) }

        deactivateMessage?.let { msg ->
            item { Text(msg, color = AutoLoungePalette.TextMuted, fontSize = 13.sp) }
        }

        if (!isPremium) {
            item { PurchaseCard(accent = accent, onBuy = { openPurchasePage(context) }) }
            item {
                VipCard {
                    SectionTitle(Icons.Default.Key, tr("Lisans anahtarım var"), accent)
                    Text(
                        tr("Satın aldıktan sonra e-postanıza gelen anahtarı girin."),
                        color = AutoLoungePalette.TextMuted, fontSize = 13.sp
                    )
                    LicenseKeyForm(accent = accent, onActivate = onActivate)
                    TextButton(onClick = { showResendDialog = true }, contentPadding = PaddingValues(0.dp)) {
                        Text(tr("Anahtarımı kaybettim"), color = accent, fontSize = 13.sp)
                    }
                }
            }
        }

        item { SupportCard(accent = accent) }

        if (BuildConfig.DEBUG) {
            item {
                VipCard {
                    Text(tr("Geliştirici testleri (yalnızca debug)"), color = AutoLoungePalette.TextDim, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onSimulatePremium(!isPremium) }, modifier = Modifier.weight(1f)) {
                            Text(if (isPremium) tr("Test VIP kapat") else tr("Test VIP aç"), fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = onResetTrial, modifier = Modifier.weight(1f)) {
                            Text(tr("Denemeyi sıfırla"), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showDeactivateConfirm) {
        AlertDialog(
            onDismissRequest = { showDeactivateConfirm = false },
            containerColor = AutoLoungePalette.CarbonElevated,
            title = { Text(tr("Lisansı bu cihazdan kaldır"), color = AutoLoungePalette.TextPure, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    tr("Cihaz hakkınız boşalır ve anahtarı başka bir telefonda kullanabilirsiniz. Bu cihazda VIP özellikleri kapanır."),
                    color = AutoLoungePalette.TextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeactivateConfirm = false
                        onDeactivate { _, msg -> deactivateMessage = msg }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AutoLoungePalette.CrimsonWarning)
                ) { Text(tr("Kaldır"), color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeactivateConfirm = false }) { Text(tr("Vazgeç"), color = AutoLoungePalette.TextDim) }
            }
        )
    }

    if (showResendDialog) {
        ResendKeyDialog(accent = accent, onResend = onResendKey, onDismiss = { showResendDialog = false })
    }
}

@Composable
private fun StatusCard(
    remainingSeconds: Int,
    isPremium: Boolean,
    license: LicenseManager.LicenseInfo?,
    onRemove: () -> Unit
) {
    val trialTotal = UsageSecurityManager.TRIAL_DURATION_SECONDS
    val minutesLeft = (remainingSeconds + 59) / 60
    val (color, title, subtitle) = when {
        isPremium -> Triple(
            AutoLoungePalette.AcidMint,
            tr("VIP aktif"),
            if (license != null) tr("Ömür boyu lisans • %s", maskKey(license.key)) else tr("Test VIP (geliştirici sürümü)")
        )
        remainingSeconds > 0 -> Triple(
            AutoLoungePalette.AmberSignal,
            tr("Deneme sürümü • %d dk kaldı", minutesLeft),
            tr("Deneme süresi yalnızca video ve canlı yayın izlerken azalır.")
        )
        else -> Triple(
            AutoLoungePalette.CrimsonWarning,
            tr("Deneme süresi bitti"),
            tr("Kullanmaya devam etmek için VIP lisansı alın veya anahtarınızı girin.")
        )
    }

    VipCard(borderColor = color.copy(alpha = 0.45f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isPremium) Icons.Default.Verified else Icons.Default.Timer,
                contentDescription = null, tint = color, modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(title, color = AutoLoungePalette.TextPure, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Text(subtitle, color = AutoLoungePalette.TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
        if (!isPremium) {
            LinearProgressIndicator(
                progress = { (remainingSeconds.toFloat() / trialTotal).coerceIn(0f, 1f) },
                color = color,
                trackColor = AutoLoungePalette.SurfaceCard,
                modifier = Modifier.fillMaxWidth().height(6.dp)
            )
        }
        if (isPremium && license != null) {
            TextButton(onClick = onRemove, contentPadding = PaddingValues(0.dp)) {
                Text(tr("Lisansı bu cihazdan kaldır"), color = AutoLoungePalette.TextDim, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PurchaseCard(accent: Color, onBuy: () -> Unit) {
    VipCard(borderColor = AutoLoungePalette.AmberSignal.copy(alpha = 0.45f)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("AutoLounge VIP", color = AutoLoungePalette.TextPure, fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.weight(1f))
            Text(displayPrice(), color = AutoLoungePalette.AmberSignal, fontWeight = FontWeight.Black, fontSize = 22.sp)
        }
        Text(tr("Tek seferlik ödeme • Abonelik yok"), color = AutoLoungePalette.TextDim, fontSize = 12.sp)
        Spacer(Modifier.height(2.dp))
        listOf(
            tr("Süre sınırı olmadan tüm uygulamalar, YouTube ve canlı TV"),
            tr("Bir anahtar 2 cihazda kullanılabilir"),
            tr("Gelecek tüm güncellemeler dahil"),
            tr("Telefon değiştirince lisansı yeni cihaza taşıyabilirsiniz")
        ).forEach { line ->
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Check, contentDescription = null, tint = AutoLoungePalette.AcidMint, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                Spacer(Modifier.width(8.dp))
                Text(line, color = AutoLoungePalette.TextPure, fontSize = 14.sp, lineHeight = 19.sp)
            }
        }
        Button(
            onClick = onBuy,
            colors = ButtonDefaults.buttonColors(containerColor = AutoLoungePalette.AmberSignal),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(tr("Satın al"), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Text(
            tr("Ödeme güvenli ödeme sayfasında yapılır (Türkiye: Shopier, diğer ülkeler: Lemon Squeezy). Lisans anahtarı e-postanıza gelir."),
            color = AutoLoungePalette.TextDim, fontSize = 12.sp, lineHeight = 16.sp
        )
    }
}

/** Anahtar girişi + Etkinleştir; VIP sekmesi ve deneme bitti penceresinde ortak. */
@Composable
fun LicenseKeyForm(
    accent: Color,
    onActivate: (key: String, onResult: (Boolean, String) -> Unit) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var key by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    fun submit() {
        if (key.isBlank() || loading) return
        focusManager.clearFocus()
        loading = true
        message = null
        onActivate(key) { ok, msg ->
            loading = false
            message = ok to msg
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = key,
            onValueChange = { key = it.uppercase().filter { c -> c.isLetterOrDigit() || c == '-' }.take(24) },
            placeholder = { Text("ALNG-XXXX-XXXX-XXXX", fontFamily = FontFamily.Monospace) },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, color = AutoLoungePalette.TextPure),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
                autoCorrect = false
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, cursorColor = accent),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { submit() },
            enabled = key.isNotBlank() && !loading,
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Text(tr("Etkinleştir"), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
        message?.let { (ok, msg) ->
            Text(
                msg,
                color = if (ok) AutoLoungePalette.AcidMint else AutoLoungePalette.CrimsonWarning,
                fontSize = 13.sp, lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ResendKeyDialog(
    accent: Color,
    onResend: (email: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AutoLoungePalette.CarbonElevated,
        title = { Text(tr("Anahtarı yeniden gönder"), color = AutoLoungePalette.TextPure, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("Satın alırken kullandığınız e-posta adresini girin."), color = AutoLoungePalette.TextMuted, fontSize = 13.sp)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    singleLine = true,
                    placeholder = { Text("ornek@eposta.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, cursorColor = accent),
                    modifier = Modifier.fillMaxWidth()
                )
                result?.let { (ok, msg) ->
                    Text(msg, color = if (ok) AutoLoungePalette.AcidMint else AutoLoungePalette.CrimsonWarning, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    loading = true
                    onResend(email) { ok, msg -> loading = false; result = ok to msg }
                },
                enabled = email.contains('@') && !loading,
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) { Text(if (loading) tr("Gönderiliyor…") else tr("Gönder"), color = Color.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Kapat"), color = AutoLoungePalette.TextDim) }
        }
    )
}

@Composable
private fun SupportCard(accent: Color) {
    val context = LocalContext.current
    VipCard {
        SectionTitle(Icons.Default.SupportAgent, tr("Yardım"), accent)
        Text(
            tr("Ödeme, anahtar veya cihaz değişikliğiyle ilgili sorunlarda bize yazın."),
            color = AutoLoungePalette.TextMuted, fontSize = 13.sp
        )
        TextButton(
            onClick = {
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${BuildConfig.SUPPORT_EMAIL}"))
                            .putExtra(Intent.EXTRA_SUBJECT, tr("AutoLounge destek"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: ActivityNotFoundException) {
                }
            },
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(BuildConfig.SUPPORT_EMAIL, color = accent, fontSize = 14.sp)
        }
    }
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = AutoLoungePalette.TextPure, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun VipCard(
    borderColor: Color = AutoLoungePalette.SurfaceBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AutoLoungePalette.CarbonElevated,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

/**
 * Deneme süresi bitince (veya kilitli içerik açılınca) çıkan pencere.
 */
@Composable
fun PaywallDialog(
    remainingSeconds: Int,
    accent: Color,
    onActivate: (key: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showKeyForm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AutoLoungePalette.CarbonElevated,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                if (remainingSeconds > 0) "AutoLounge VIP" else tr("Deneme süresi bitti"),
                color = AutoLoungePalette.TextPure, fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (remainingSeconds > 0) tr("Denemede %d dakikanız kaldı. VIP ile süre sınırı olmadan kullanın.", (remainingSeconds + 59) / 60)
                    else tr("60 dakikalık deneme süreniz doldu. Kullanmaya devam etmek için VIP lisansı alın — tek seferlik %s.", displayPrice()),
                    color = AutoLoungePalette.TextMuted, fontSize = 14.sp, lineHeight = 19.sp
                )
                if (showKeyForm) {
                    LicenseKeyForm(accent = accent, onActivate = { key, cb ->
                        onActivate(key) { ok, msg ->
                            cb(ok, msg)
                            if (ok) onDismiss()
                        }
                    })
                } else {
                    TextButton(onClick = { showKeyForm = true }, contentPadding = PaddingValues(0.dp)) {
                        Text(tr("Lisans anahtarım var"), color = accent)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { openPurchasePage(context) },
                colors = ButtonDefaults.buttonColors(containerColor = AutoLoungePalette.AmberSignal)
            ) { Text(tr("Satın al"), color = Color.Black, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Kapat"), color = AutoLoungePalette.TextDim) }
        }
    )
}
