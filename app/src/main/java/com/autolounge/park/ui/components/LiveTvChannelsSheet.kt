package com.autolounge.park.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autolounge.park.data.Bookmark
import com.autolounge.park.data.NewsChannel
import com.autolounge.park.data.NewsChannelsRepository
import com.autolounge.park.ui.AutoLoungePalette
import coil.compose.AsyncImage
import java.util.Locale
import com.autolounge.park.i18n.tr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTvChannelsSheet(
    bookmarks: List<Bookmark>,
    onSelectChannel: (NewsChannel) -> Unit,
    onPinChannel: (title: String, url: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val detectedCountry = remember { NewsChannelsRepository.detectUserCountry() }
    var selectedCountry by remember { mutableStateOf(detectedCountry) }
    var isSelectingCountryView by remember { mutableStateOf(false) }

    val pinnedUrls = remember(bookmarks) {
        bookmarks.map { it.url.trimEnd('/') }.toSet()
    }

    val countries = remember { NewsChannelsRepository.getSupportedCountries() }
    val currentChannels = remember(selectedCountry) {
        NewsChannelsRepository.getChannelsForCountry(selectedCountry)
    }

    var searchQuery by remember { mutableStateOf("") }

    var filteredChannels by remember { mutableStateOf(currentChannels) }
    LaunchedEffect(currentChannels, searchQuery) {
        if (searchQuery.isBlank()) {
            filteredChannels = currentChannels
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(200)
        filteredChannels = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            val inCountry = currentChannels.filter {
                it.title.lowercase(Locale.ROOT).contains(q) ||
                    it.category.lowercase(Locale.ROOT).contains(q)
            }
            inCountry.ifEmpty {
                NewsChannelsRepository.allChannels.filter {
                    it.title.lowercase(Locale.ROOT).contains(q) ||
                        it.category.lowercase(Locale.ROOT).contains(q)
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AutoLoungePalette.CarbonElevated,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF334155))
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            // --- HEADER & ÜLKE NAVİGASYONU ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSelectingCountryView) {
                        IconButton(
                            onClick = { isSelectingCountryView = false },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = tr("Geri"),
                                tint = AutoLoungePalette.TextPure,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            tr("Ülke & Bölge Seçimi"),
                            color = AutoLoungePalette.TextPure,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    } else {
                        // Sol Üstte Ülke Seçim Düğmesi
                        Surface(
                            onClick = { isSelectingCountryView = true },
                            shape = RoundedCornerShape(10.dp),
                            color = AutoLoungePalette.SurfaceCard,
                            border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Public,
                                    contentDescription = tr("Ülke"),
                                    tint = AutoLoungePalette.EmeraldNeon,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = countries.firstOrNull { it.first == selectedCountry }?.second?.let { tr(it) } ?: tr("Türkiye"),
                                    color = AutoLoungePalette.TextPure,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = AutoLoungePalette.TextDim,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = tr("Kapat"),
                            tint = AutoLoungePalette.TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // --- ARAMA ÇUBUĞU (KOKPİT STİLİ) ---
            if (!isSelectingCountryView) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AutoLoungePalette.SurfaceCard,
                    border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = tr("Kanal Ara"),
                            tint = AutoLoungePalette.TextDim,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    tr("Kanal ara (Show, TV8, TRT, Spor...)"),
                                    color = AutoLoungePalette.TextDim,
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = AutoLoungePalette.EmeraldNeon,
                                focusedTextColor = AutoLoungePalette.TextPure,
                                unfocusedTextColor = AutoLoungePalette.TextPure
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = tr("Temizle"),
                                    tint = AutoLoungePalette.TextDim,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- İÇERİK: ÜLKE SEÇİM LİSTESİ VEYA KANAL LİSTESİ ---
            if (isSelectingCountryView) {
                // Ülkeler Listesi
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .padding(bottom = 16.dp)
                ) {
                    items(countries) { (code, name) ->
                        val isSelected = selectedCountry == code
                        Surface(
                            onClick = {
                                selectedCountry = code
                                NewsChannelsRepository.saveSelectedCountry(code)
                                isSelectingCountryView = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) AutoLoungePalette.EmeraldNeon.copy(alpha = 0.15f) else AutoLoungePalette.SurfaceCard,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) AutoLoungePalette.EmeraldNeon else AutoLoungePalette.SurfaceBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Language,
                                        contentDescription = null,
                                        tint = if (isSelected) AutoLoungePalette.EmeraldNeon else AutoLoungePalette.TextDim,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        tr(name),
                                        color = if (isSelected) AutoLoungePalette.EmeraldNeon else AutoLoungePalette.TextPure,
                                        fontSize = 14.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = AutoLoungePalette.EmeraldNeon,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (filteredChannels.isEmpty()) {
                // Arama Sonucu Bulunamadı Boş Durum
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = AutoLoungePalette.TextDim,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            tr("'%s' ile eşleşen kanal bulunamadı", searchQuery),
                            color = AutoLoungePalette.TextDim,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                // Kanallar Listesi
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .padding(bottom = 16.dp)
                ) {
                    items(filteredChannels, key = { it.id }) { ch ->
                        val isPinned = ch.url.trimEnd('/') in pinnedUrls

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = AutoLoungePalette.SurfaceCard,
                            border = BorderStroke(1.dp, AutoLoungePalette.SurfaceBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Tıklayınca Oynat
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onDismiss()
                                            onSelectChannel(ch)
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val chLogo = remember(ch) { NewsChannelsRepository.logoModelFor(ch) }
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(if (chLogo != null) Color(0xFFF8FAFC) else Color.Transparent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (chLogo != null) {
                                            AdaptiveLogo(
                                                model = chLogo,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(3.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(Color(0xFF10B981), Color(0xFF047857))
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    if (ch.category.contains("Radyo", ignoreCase = true)) Icons.Default.Radio else Icons.Default.LiveTv,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = ch.title,
                                            color = AutoLoungePalette.TextPure,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${tr(ch.category)} • ${tr("Canlı Yayın")}",
                                            color = AutoLoungePalette.TextDim,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // Zarif Pin (Ana Ekrana Ekle) Butonu
                                IconButton(
                                    onClick = {
                                        if (!isPinned) {
                                            onPinChannel(ch.title, ch.url)
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        if (isPinned) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                                        contentDescription = if (isPinned) tr("Ana Ekranda Ekli") else tr("Ana Ekrana Ekle"),
                                        tint = if (isPinned) AutoLoungePalette.EmeraldNeon else AutoLoungePalette.TextDim,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
