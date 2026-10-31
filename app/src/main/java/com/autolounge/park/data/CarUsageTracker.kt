package com.autolounge.park.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Otomotiv Deneme Süresi Kullanım Takipçisi.
 * Kural: Süre sadece Araç Ekranı (CarMainActivity) aktifken veya
 * arka planda ses (ParkAudioService) çalarken eksilir.
 * Normal telefon ayarlarında gezinirken süre harcanmaz.
 */
@Singleton
class CarUsageTracker @Inject constructor(
    private val usageManager: UsageSecurityManager,
    private val licenseManager: LicenseManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null

    private val _isCarScreenActive = MutableStateFlow(false)
    val isCarScreenActive: StateFlow<Boolean> = _isCarScreenActive.asStateFlow()

    private val _isAudioStreamingActive = MutableStateFlow(false)
    val isAudioStreamingActive: StateFlow<Boolean> = _isAudioStreamingActive.asStateFlow()

    init {
        startUsageTicker()
    }

    fun setCarScreenActive(active: Boolean) {
        _isCarScreenActive.value = active
    }

    fun setAudioStreamingActive(active: Boolean) {
        _isAudioStreamingActive.value = active
    }

    private fun startUsageTicker() {
        // Open-source: Sınırsız sürüm, kullanım sayacı devre dışı
    }
}
