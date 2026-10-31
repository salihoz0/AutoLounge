package com.autohub.park.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.autohub.park.data.CarSetupChecker
import com.autohub.park.data.CarSetupChecker.InstallSource
import com.autohub.park.i18n.tr
import com.autohub.park.ui.AutoLoungePalette

private const val KING_INSTALLER_URL = "https://github.com/fcaronte/KingInstaller/releases"
private const val SHIZUKU_URL = "https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"

/** Kurulum durumunu ekrana dönüşte (ayarlardan geri gelince) yeniden okur. */
@Composable
fun rememberCarSetupStatus(): State<CarSetupChecker.Status> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(CarSetupChecker.check(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) state.value = CarSetupChecker.check(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return state
}

/**
 * Kokpit sekmesinin üstünde, araç ekranı henüz hiç açılmadıysa görünen uyarı şeridi.
 */
@Composable
fun CarSetupBanner(accent: Color, onOpenGuide: () -> Unit, onHide: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AutoLoungePalette.CarbonElevated,
        border = BorderStroke(1.dp, AutoLoungePalette.AmberSignal.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpenGuide() }
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = AutoLoungePalette.AmberSignal, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Araç ekranı kurulumu"), color = AutoLoungePalette.TextPure, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    tr("Arabada açılmıyor veya \"yeni mesaj gelmedi\" mi görüyorsunuz? Telefonunuzu kontrol edelim."),
                    color = AutoLoungePalette.TextMuted, fontSize = 12.sp, lineHeight = 16.sp
                )
            }
            TextButton(onClick = onOpenGuide) { Text(tr("Kontrol et"), color = accent, fontWeight = FontWeight.Bold) }
            IconButton(onClick = onHide, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = tr("Gizle"), tint = AutoLoungePalette.TextDim, modifier = Modifier.size(16.dp))
            }
        }
    }
}

private enum class CheckState { OK, WARN, FAIL }

/**
 * Telefona göre adım adım Android Auto kurulum rehberi.
 */
@Composable
fun CarSetupGuideDialog(accent: Color, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val status by rememberCarSetupStatus()
    var unknownConfirmed by remember(status) { mutableStateOf(status.unknownSourcesConfirmed) }
    var micGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val micLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { micGranted = it }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = AutoLoungePalette.ObsidianVoid,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(tr("Araç Ekranı Kurulumu"), color = AutoLoungePalette.TextPure, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            Text(
                                tr("Android %s • %s", status.androidVersion, brandLabel(status.brand)),
                                color = AutoLoungePalette.TextDim, fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = tr("Kapat"), tint = AutoLoungePalette.TextPure)
                        }
                    }
                }

                if (status.carSessionSeen) {
                    item {
                        GuideCard(accent = AutoLoungePalette.AcidMint) {
                            Text(tr("✓ Kurulum çalışıyor"), color = AutoLoungePalette.AcidMint, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                tr("AutoLounge araç ekranında en az bir kez açıldı. Uygulamayı güncellerken aynı yöntemle kurmaya devam edin."),
                                color = AutoLoungePalette.TextMuted, fontSize = 13.sp, lineHeight = 18.sp
                            )
                        }
                    }
                }

                // --- 1. Android Auto ---
                item {
                    CheckRow(
                        number = 1,
                        title = "Android Auto",
                        state = if (status.androidAutoInstalled) CheckState.OK else CheckState.FAIL,
                        detail = if (status.androidAutoInstalled) tr("Yüklü (sürüm %s)", status.androidAutoVersion ?: "?")
                        else tr("Android Auto yüklü değil ya da devre dışı.")
                    ) {
                        if (!status.androidAutoInstalled) {
                            GuideButton(tr("Play Store'da aç"), Icons.Default.Download, accent) {
                                CarSetupChecker.openAndroidAutoInPlayStore(context)
                            }
                        }
                    }
                }

                // --- 2. Kurulum yöntemi ---
                item {
                    val (state, detail) = when {
                        status.usesParkedApps -> CheckState.OK to tr("Android %s: Ek bir yükleyiciye gerek yok. Android Auto uygulamayı doğrudan açar; yalnızca 3. adımdaki \"Bilinmeyen kaynaklar\" ayarı gerekli.", status.androidVersion)
                        status.installSource == InstallSource.PLAY -> CheckState.OK to tr("Uygulama \"Google Play tarafından yüklendi\" görünüyor. Android Auto tam ekranı kabul eder.")
                        else -> CheckState.FAIL to tr("Android %s sürümünde Android Auto, uygulamanın \"Google Play tarafından yüklendi\" görünmesini istiyor. Aşağıdaki yöntemle yeniden kurun.", status.androidVersion)
                    }
                    CheckRow(number = 2, title = tr("Kurulum yöntemi"), state = state, detail = detail) {
                        if (state != CheckState.OK) {
                            InstallMethods(status = status, accent = accent)
                        }
                    }
                }

                // --- 3. Bilinmeyen kaynaklar ---
                item {
                    CheckRow(
                        number = 3,
                        title = tr("Android Auto \"Bilinmeyen kaynaklar\""),
                        state = if (unknownConfirmed) CheckState.OK else CheckState.WARN,
                        detail = tr("Bu ayar telefondan okunamıyor; açtıktan sonra aşağıdan işaretleyin.")
                    ) {
                        StepList(
                            listOf(
                                tr("Android Auto ayarlarını açın (%s).", CarSetupChecker.androidAutoSettingsPath(status.brand)),
                                tr("En alta inip \"Sürüm\" satırına art arda 10 kez dokunun ve \"Tamam\" deyin."),
                                tr("Sağ üstteki ⋮ menüsünden \"Geliştirici ayarları\"nı açın."),
                                tr("Listenin altındaki \"Bilinmeyen kaynaklar\" kutusunu işaretleyin.")
                            )
                        )
                        GuideButton(tr("Android Auto ayarlarını aç"), Icons.Default.Settings, accent) {
                            CarSetupChecker.openAndroidAutoSettings(context)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    unknownConfirmed = !unknownConfirmed
                                    CarSetupChecker.setUnknownSourcesConfirmed(context, unknownConfirmed)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = unknownConfirmed,
                                onCheckedChange = {
                                    unknownConfirmed = it
                                    CarSetupChecker.setUnknownSourcesConfirmed(context, it)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = accent)
                            )
                            Text(tr("Bilinmeyen kaynakları açtım"), color = AutoLoungePalette.TextPure, fontSize = 13.sp)
                        }
                    }
                }

                // --- 4. Mikrofon izni ---
                item {
                    CheckRow(
                        number = 4,
                        title = tr("Mikrofon izni (sesli arama)"),
                        state = if (micGranted) CheckState.OK else CheckState.WARN,
                        detail = if (micGranted) tr("Verildi. Araçta YouTube ve sitelerde sesli arama kullanabilirsiniz.")
                        else tr("Araç ekranında izin penceresi açılamaz; izni şimdi telefondan verin.")
                    ) {
                        if (!micGranted) {
                            GuideButton(tr("Mikrofon iznini ver"), Icons.Default.Mic, accent) {
                                micLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }
                }

                // --- 5. Araca bağlanma ---
                item {
                    CheckRow(
                        number = 5,
                        title = tr("Araca bağlanın"),
                        state = if (status.carSessionSeen) CheckState.OK else CheckState.WARN,
                        detail = if (status.carSessionSeen) tr("Araç ekranı başarıyla açıldı.")
                        else tr("Henüz araç ekranında açılmadı.")
                    ) {
                        StepList(
                            listOf(
                                tr("Kurulumdan sonra telefonu araçtan çıkarıp yeniden bağlayın; Android Auto uygulama listesini bağlanırken yeniler."),
                                tr("Hâlâ görünmüyorsa telefonu bir kez yeniden başlatın."),
                                tr("Kablosuz adaptör (AAWireless vb.) kullanıyorsanız adaptörün geliştirici modunu da kontrol edin.")
                            )
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun InstallMethods(status: CarSetupChecker.Status, accent: Color) {
    val context = LocalContext.current
    Spacer(Modifier.height(4.dp))
    if (!status.needsModernInstaller) {
        Text(tr("Android 13 ve altı için önerilen:"), color = AutoLoungePalette.TextPure, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        StepList(
            listOf(
                tr("KingInstaller'ı indirip açın."),
                tr("\"Select file\" ile AutoLounge APK'sını seçin."),
                tr("\"Install as Google Play Store\" ile kurun.")
            )
        )
    } else {
        Text(tr("Android %s için önerilen:", status.androidVersion), color = AutoLoungePalette.TextPure, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        StepList(
            listOf(
                tr("Shizuku uygulamasını kurun ve kablosuz hata ayıklama ile başlatın."),
                tr("KingInstaller'ın en güncel sürümünü (1.7 veya üstü) indirin."),
                tr("KingInstaller'da \"Shizuku Trick\" seçeneğini açıp AutoLounge APK'sını kurun."),
                tr("Xiaomi / Redmi / POCO cihazlarda Shizuku çoğu zaman çalışmaz; bu cihazlarda yalnızca root yöntemi güvenilirdir.")
            )
        )
        GuideButton(tr("Shizuku'yu aç"), Icons.Default.Download, accent) { CarSetupChecker.openUrl(context, SHIZUKU_URL) }
    }
    GuideButton(tr("KingInstaller'ı indir"), Icons.Default.Download, accent) { CarSetupChecker.openUrl(context, KING_INSTALLER_URL) }
    Text(
        tr("İpucu: Telefonunuzu Android 15 veya üstüne güncellerseniz bu adıma gerek kalmaz."),
        color = AutoLoungePalette.TextDim, fontSize = 12.sp, lineHeight = 16.sp
    )
}

@Composable
private fun CheckRow(
    number: Int,
    title: String,
    state: CheckState,
    detail: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val color = when (state) {
        CheckState.OK -> AutoLoungePalette.AcidMint
        CheckState.WARN -> AutoLoungePalette.AmberSignal
        CheckState.FAIL -> AutoLoungePalette.CrimsonWarning
    }
    GuideCard(accent = color) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(26.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    CheckState.OK -> Icon(Icons.Default.Check, contentDescription = tr("Tamam"), tint = color, modifier = Modifier.size(16.dp))
                    CheckState.WARN -> Text("$number", color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    CheckState.FAIL -> Icon(Icons.Default.PriorityHigh, contentDescription = tr("Sorun"), tint = color, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(title, color = AutoLoungePalette.TextPure, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Text(detail, color = AutoLoungePalette.TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
        content()
    }
}

@Composable
private fun GuideCard(accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AutoLoungePalette.CarbonElevated,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun StepList(steps: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        steps.forEachIndexed { i, step ->
            Row {
                Text("${i + 1}.", color = AutoLoungePalette.TextDim, fontSize = 13.sp, modifier = Modifier.width(20.dp))
                Text(step, color = AutoLoungePalette.TextPure, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun GuideButton(label: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = accent, fontWeight = FontWeight.SemiBold)
    }
}

private fun brandLabel(brand: CarSetupChecker.PhoneBrand): String = when (brand) {
    CarSetupChecker.PhoneBrand.SAMSUNG -> "Samsung"
    CarSetupChecker.PhoneBrand.PIXEL -> "Google Pixel"
    CarSetupChecker.PhoneBrand.XIAOMI -> "Xiaomi / Redmi / POCO"
    CarSetupChecker.PhoneBrand.OTHER -> android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
}
