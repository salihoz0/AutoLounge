package com.autolounge.park.data

import androidx.lifecycle.ViewModel
import com.autolounge.park.i18n.tr
import androidx.lifecycle.viewModelScope
import com.autolounge.park.ui.CarVisualTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookmarkViewModel @Inject constructor(
    private val dao: BookmarkDao,
    val usageManager: UsageSecurityManager,
    val licenseManager: LicenseManager,
    val configRepo: CarConfigRepository,
    val usageTracker: CarUsageTracker,
    val safetyManager: ParkSafetyManager
) : ViewModel() {

    val items: StateFlow<List<Bookmark>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val config: StateFlow<CarConfig> = configRepo.configFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CarConfig())

    val remainingSeconds: StateFlow<Int> = MutableStateFlow(86400)

    // Open Source: Tüm özellikler sınırsız ve açık
    val isPremium: StateFlow<Boolean> = MutableStateFlow(true)

    val license: StateFlow<LicenseManager.LicenseInfo?> = MutableStateFlow(null)

    init {

        // Seed default bookmarks and purge old placeholder example.com bookmarks
        viewModelScope.launch {
            val existing = dao.observeAll().first()
            existing.filter { it.url.contains("example.com") || it.title == "TV Listem" }.forEach {
                runCatching { dao.deleteById(it.id) }
            }
            if (dao.observeAll().first().isEmpty()) {
                DefaultBookmarks.forCountry(NewsChannelsRepository.deviceCountry()).forEach { runCatching { dao.insert(it) } }
            } else {
                val current = dao.observeAll().first()
                DefaultBookmarks.forCountry(NewsChannelsRepository.deviceCountry()).forEach { defaultBm ->
                    if (!current.any { it.url.trimEnd('/') == defaultBm.url.trimEnd('/') }) {
                        runCatching { dao.insert(defaultBm) }
                    }
                }
            }
        }
    }


    fun addCatalogItem(catalogItem: CatalogItem, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val existing = dao.observeAll().first()
            val cleanUrl = catalogItem.url.trim().trimEnd('/')
            if (!existing.any { it.url.trim().trimEnd('/').equals(cleanUrl, ignoreCase = true) }) {
                dao.insert(
                    Bookmark(
                        title = catalogItem.title,
                        url = cleanUrl,
                        type = if (catalogItem.category == "Canlı TV") "tv" else "site"
                    )
                )
            }
            onComplete()
        }
    }

    fun removeCatalogItem(url: String) {
        viewModelScope.launch {
            val cleanUrl = url.trim().trimEnd('/')
            val existing = dao.observeAll().first().firstOrNull { 
                it.url.trim().trimEnd('/').equals(cleanUrl, ignoreCase = true) 
            }
            if (existing != null) {
                dao.deleteById(existing.id)
            }
        }
    }

    fun activateLicense(key: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (usageManager.activateDebugMasterKey(key)) {
                onResult(true, tr("Test VIP etkinleştirildi (yalnızca geliştirici sürümü)."))
                return@launch
            }
            when (val r = licenseManager.activate(key)) {
                is LicenseManager.Result.Success -> onResult(true, tr("%s (%d/%d cihaz)", r.message, r.slotsUsed, r.maxDevices))
                is LicenseManager.Result.Failure -> onResult(false, r.message)
            }
        }
    }

    fun deactivateLicense(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val r = licenseManager.deactivate()) {
                is LicenseManager.Result.Success -> onResult(true, r.message)
                is LicenseManager.Result.Failure -> onResult(false, r.message)
            }
        }
    }

    fun resendLicenseKey(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val r = licenseManager.resendKey(email)) {
                is LicenseManager.Result.Success -> onResult(true, r.message)
                is LicenseManager.Result.Failure -> onResult(false, r.message)
            }
        }
    }

    fun add(title: String, url: String, onError: (String) -> Unit = {}) {
        val clean = url.trim().trimEnd('/')
        if (!clean.startsWith("https://")) { onError(tr("Sadece https:// ile başlayan siteler")); return }
        if (title.length > 24 || title.isBlank()) { onError(tr("İsim 1-24 karakter olmalıdır")); return }
        
        viewModelScope.launch {
            val existing = dao.observeAll().first()
            if (existing.any { it.url.trim().trimEnd('/').equals(clean, ignoreCase = true) }) {
                onError(tr("Bu site veya adres zaten kokpitte mevcut"))
                return@launch
            }
            runCatching { dao.insert(Bookmark(title = title.trim(), url = clean)) }
                .onFailure { onError(tr("Kaydedilemedi")) }
        }
    }

    fun updateBookmarkTitle(id: Long, newTitle: String, onError: (String) -> Unit = {}) {
        val clean = newTitle.trim()
        if (clean.isBlank() || clean.length > 24) {
            onError(tr("İsim 1-24 karakter olmalıdır"))
            return
        }
        viewModelScope.launch {
            runCatching { dao.updateTitle(id, clean) }
                .onFailure { onError(tr("Güncellenemedi")) }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            dao.deleteUser(id)
        }
    }

    fun setTheme(theme: CarVisualTheme) {
        viewModelScope.launch {
            configRepo.setTheme(theme)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            configRepo.setThemeMode(mode)
        }
    }


    fun toggleCard(cardKey: String, visible: Boolean) {
        viewModelScope.launch {
            configRepo.toggleCard(cardKey, visible)
        }
    }

    fun setGridColumns(cols: Int) {
        viewModelScope.launch {
            configRepo.setGridColumns(cols)
        }
    }

    fun reorderCards(order: List<String>) {
        viewModelScope.launch {
            configRepo.setCardOrder(order)
        }
    }


    fun simulatePremium(active: Boolean) {
        viewModelScope.launch {
            usageManager.setSimulatedPremium(active)
        }
    }

    fun resetTrial() {
        viewModelScope.launch {
            usageManager.resetTrialForTesting()
        }
    }

    fun resetAllToDefaults(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            configRepo.resetToDefaults()
            dao.deleteAllUserBookmarks()
            // Eksik varsayılan varsa garanti et
            val current = dao.observeAll().first()
            if (current.isEmpty()) {
                DefaultBookmarks.forCountry(NewsChannelsRepository.deviceCountry()).forEach { runCatching { dao.insert(it) } }
            }
            onComplete()
        }
    }


    fun favicon(url: String): String {
        val host = runCatching { android.net.Uri.parse(url).host ?: "" }.getOrDefault("")
        return "https://www.google.com/s2/favicons?domain=$host&sz=64"
    }
}
