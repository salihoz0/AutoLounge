package com.autolounge.park.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.autolounge.park.data.AppCatalogRepository
import com.autolounge.park.data.Bookmark
import com.autolounge.park.data.BrandLogos
import com.autolounge.park.data.CatalogItem
import com.autolounge.park.i18n.tr
import com.autolounge.park.ui.AutoLoungePalette

@Composable
fun AddAppModal(
    currentBookmarks: List<Bookmark>,
    faviconProvider: (String) -> String,
    onAddCatalogItem: (CatalogItem) -> Unit,
    onRemoveCatalogItem: (String) -> Unit,
    onAddCustomUrl: (title: String, url: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Tümü") }
    var searchQuery by remember { mutableStateOf("") }
    var showCustomUrlTab by remember { mutableStateOf(false) }

    // Özel URL Form State
    var customTitle by remember { mutableStateOf("") }
    var customUrl by remember { mutableStateOf("https://") }
    var customError by remember { mutableStateOf("") }

    val addedUrls = remember(currentBookmarks) {
        currentBookmarks.map { it.url.trim().trimEnd('/').lowercase() }.toSet()
    }

    val filteredItems = remember(selectedCategory, searchQuery) {
        AppCatalogRepository.items.filter { item ->
            val matchCategory = selectedCategory == "Tümü" || item.category == selectedCategory
            val matchQuery = searchQuery.isBlank() || 
                item.title.contains(searchQuery, ignoreCase = true) || 
                item.subtitle.contains(searchQuery, ignoreCase = true)
            matchCategory && matchQuery
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F1117),
            border = BorderStroke(1.dp, Color(0xFF262A36)),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // --- BAŞLIK & KAPATMA ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (showCustomUrlTab) tr("Özel Web Sitesi Ekle") else tr("Uygulama & Kanal Ekle"),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (showCustomUrlTab) tr("Herhangi bir web adresini masaüstü kartı yapın") else tr("Popüler yayın servisleri ve canlı kanallar"),
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2230))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = tr("Kapat"),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // --- SEKME GEÇİŞİ (Katalog vs Özel URL) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF161922))
                        .padding(4.dp)
                ) {
                    TabPillButton(
                        text = tr("Hazır Katalog (%d)", AppCatalogRepository.items.size),
                        isSelected = !showCustomUrlTab,
                        onClick = { showCustomUrlTab = false },
                        modifier = Modifier.weight(1f)
                    )
                    TabPillButton(
                        text = tr("+ Özel Link Ekle"),
                        isSelected = showCustomUrlTab,
                        onClick = { showCustomUrlTab = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(14.dp))

                if (!showCustomUrlTab) {
                    // --- ARAMA KUTUSU ---
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(tr("Kanal, dizi, video veya radyo ara..."), color = Color(0xFF64748B), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, tint = Color(0xFF94A3B8))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF141722),
                            unfocusedContainerColor = Color(0xFF141722),
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF262A36),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // --- KATEGORİ HAPLARI ---
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(AppCatalogRepository.categories) { cat ->
                            val isSelected = cat == selectedCategory
                            Surface(
                                onClick = { selectedCategory = cat },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF10B981) else Color(0xFF1A1D28),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF10B981) else Color(0xFF2B3042)
                                )
                            ) {
                                Text(
                                    text = tr(cat),
                                    color = if (isSelected) Color(0xFF041E15) else Color(0xFFCBD5E1),
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // --- LİSTE ---
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            val isAdded = addedUrls.contains(item.url.trim().trimEnd('/').lowercase())

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF141722),
                                border = BorderStroke(1.dp, if (isAdded) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFF222634))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Favicon / Logo
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (BrandLogos.uriFor(item.id) != null) Color(0xFFF8FAFC)
                                                    else Color(0xFF1E2230)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AdaptiveLogo(
                                                model = BrandLogos.uriFor(item.id)
                                                    ?: BrandLogos.findLogoByUrlOrTitle(item.title, item.url)
                                                    ?: faviconProvider(item.url),
                                                contentDescription = null,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }

                                        Spacer(Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = tr(item.title),
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (item.badge != null) {
                                                    Spacer(Modifier.width(8.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(
                                                                if (item.badge == "Canlı") Color(0xFFEF4444).copy(alpha = 0.2f)
                                                                else Color(0xFF10B981).copy(alpha = 0.2f)
                                                            )
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = tr(item.badge),
                                                            color = if (item.badge == "Canlı") Color(0xFFF87171) else Color(0xFF34D399),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.ExtraBold
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = tr(item.subtitle),
                                                color = Color(0xFF94A3B8),
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    // Ekle / Kaldır Butonu
                                    Surface(
                                        onClick = {
                                            if (isAdded) {
                                                onRemoveCatalogItem(item.url)
                                            } else {
                                                onAddCatalogItem(item)
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isAdded) Color(0xFF1B2A22) else Color(0xFF10B981),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isAdded) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF10B981)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                if (isAdded) Icons.Default.Check else Icons.Default.Add,
                                                contentDescription = null,
                                                tint = if (isAdded) Color(0xFF10B981) else Color(0xFF041E15),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = if (isAdded) tr("Ekli") else tr("Ekle"),
                                                color = if (isAdded) Color(0xFF10B981) else Color(0xFF041E15),
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // --- ÖZEL URL EKLEME FORMU ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it; customError = "" },
                            label = { Text(tr("Uygulama veya Site Adı")) },
                            placeholder = { Text(tr("Örn: Kick Yayınları, Borsa Portalı")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = customTextFieldColors()
                        )

                        OutlinedTextField(
                            value = customUrl,
                            onValueChange = { customUrl = it; customError = "" },
                            label = { Text(tr("Web Adresi (URL)")) },
                            placeholder = { Text("https://example.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = customTextFieldColors()
                        )

                        if (customError.isNotEmpty()) {
                            Text(
                                text = customError,
                                color = Color(0xFFEF4444),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = {
                                val cleanUrl = customUrl.trim()
                                val cleanTitle = customTitle.trim()
                                if (cleanTitle.isBlank()) {
                                    customError = tr("Lütfen bir isim belirleyin")
                                    return@Button
                                }
                                if (!cleanUrl.startsWith("https://")) {
                                    customError = tr("Adres https:// ile başlamalıdır")
                                    return@Button
                                }
                                onAddCustomUrl(cleanTitle, cleanUrl)
                                customTitle = ""
                                customUrl = "https://"
                                showCustomUrlTab = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color(0xFF041E15)
                            )
                        ) {
                            Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tr("Kokpit Masaüstüne Ekle"), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.weight(1f))

                        Text(
                            text = tr("İpucu: Eklediğiniz web siteleri araç ekranında tam ekran uygulama olarak açılır. Reklam engelleme ve donanım hızlandırma otomatik aktiftir."),
                            color = Color(0xFF64748B),
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabPillButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFF1E2433) else Color.Transparent,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color(0xFF141722),
    unfocusedContainerColor = Color(0xFF141722),
    focusedBorderColor = Color(0xFF10B981),
    unfocusedBorderColor = Color(0xFF262A36),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = Color(0xFF10B981),
    unfocusedLabelColor = Color(0xFF94A3B8)
)
