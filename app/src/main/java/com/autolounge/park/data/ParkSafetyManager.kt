package com.autohub.park.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.autohub.park.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Play Otomotiv Güvenlik ve Park Dedektörü.
 * Kural: Araç hızı < 3 km/s ise veya el freni / park modu aktifse "PARK" kabul edilir ve video açılır.
 * Araç hareket ettiğinde sürüş güvenliği için video otomatik kararır, sadece ses devam eder.
 */
@Singleton
class ParkSafetyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "ParkSafetyManager"

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private var trackingActive = false
    private var lastLocation: Location? = null

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (forceParkedForTesting && BuildConfig.DEBUG) {
                _isParked.value = forceParkedValue
                return
            }

            // Use hardware speed if available, otherwise calculate from location delta
            val speedKmh = if (location.hasSpeed() && location.speed >= 0f) {
                (location.speed * 3.6f).coerceAtLeast(0f)
            } else {
                val prev = lastLocation
                if (prev != null && prev.time > 0 && location.time > prev.time) {
                    val distMeters = location.distanceTo(prev)
                    val timeSec = (location.time - prev.time) / 1000f
                    // GPS sapmalarını (jitter) filtrele: Mesafe GPS hata payından küçükse araç duruyordur
                    val accuracyThreshold = ((location.accuracy + prev.accuracy) / 2f).coerceAtLeast(6f)
                    if (distMeters < accuracyThreshold || distMeters < 3f) {
                        0f
                    } else if (timeSec in 0.5f..10f) {
                        ((distMeters / timeSec) * 3.6f).coerceAtLeast(0f)
                    } else {
                        _currentSpeedKmh.value
                    }
                } else {
                    _currentSpeedKmh.value
                }
            }
            lastLocation = location
            _currentSpeedKmh.value = speedKmh

            // Speeds above 3 km/h are considered actively in motion
            val parked = speedKmh < 3f
            // Yolcu modu aktif edildiğinde sürüş boyunca (kırmızı ışık veya duraklamalarda)
            // videonun kesilmemesi için bypass oturum süresince korunur.
            _isParked.value = parked
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Speed: $speedKmh km/h (hasSpeed=${location.hasSpeed()}), isParked: $parked, bypass=${_passengerBypassActive.value}")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(receiverContext: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // Araç ekranı aktifken telefon ekranı kapansa dahi GPS takibini sürdür
                    if (trackingActive && !AutoLoungeSyncBridge.carAppAliveFlow.value) {
                        runCatching { locationManager?.removeUpdates(locationListener) }
                    }
                }
                Intent.ACTION_SCREEN_ON -> if (trackingActive) {
                    requestUpdates()
                }
            }
        }
    }

    // Default state is parked
    private val _isParked = MutableStateFlow(true)
    val isParked: StateFlow<Boolean> = _isParked.asStateFlow()

    // Yolcu modu: Sürüş esnasında yolcu onay verirse video açılır, ancak araç her durduğunda sıfırlanır
    private val _passengerBypassActive = MutableStateFlow(false)
    val passengerBypassActive: StateFlow<Boolean> = _passengerBypassActive.asStateFlow()

    fun setPassengerBypass(enabled: Boolean) {
        _passengerBypassActive.value = enabled
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Passenger bypass set: $enabled")
        }
    }

    val isDrivingLockoutActive: Boolean
        get() = !_isParked.value && !_passengerBypassActive.value

    private val _currentSpeedKmh = MutableStateFlow(0f)
    val currentSpeedKmh: StateFlow<Float> = _currentSpeedKmh.asStateFlow()

    // Manuel test / simülasyon kilidi (varsayılan: gerçek GPS).
    // Testte park simüle etmek için setForceParked(true) çağırın.
    private var forceParkedForTesting = false
    private var forceParkedValue = true

    init {
        startGpsMonitoring()
    }

    fun setForceParked(forced: Boolean) {
        if (!BuildConfig.DEBUG) {
            Log.w(TAG, "Force parked simulation is strictly forbidden in release builds.")
            return
        }
        forceParkedForTesting = true
        forceParkedValue = forced
        _isParked.value = forced
    }

    fun clearForceParked() {
        if (!BuildConfig.DEBUG) return
        forceParkedForTesting = false
    }

    fun hasLocationPermission(): Boolean =
        context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    fun refreshMonitoring() {
        if (!trackingActive) {
            startGpsMonitoring()
        } else {
            requestUpdates()
        }
    }

    private fun requestUpdates() {
        val manager = locationManager ?: return
        if (!hasLocationPermission()) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Location permission not granted. GPS vehicle speed monitoring is inactive.")
            }
            return
        }
        runCatching {
            manager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0f,
                locationListener
            )
        }
        runCatching {
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                manager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    0f,
                    locationListener
                )
            }
        }
    }

    private fun startGpsMonitoring() {
        try {
            if (locationManager == null) return
            trackingActive = true
            requestUpdates()
            runCatching {
                ContextCompat.registerReceiver(
                    context,
                    screenStateReceiver,
                    IntentFilter().apply {
                        addAction(Intent.ACTION_SCREEN_OFF)
                        addAction(Intent.ACTION_SCREEN_ON)
                    },
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "GPS monitoring setup fallback: ${e.message}")
        }
    }

    private fun stopGpsMonitoring() {
        trackingActive = false
        runCatching { locationManager?.removeUpdates(locationListener) }
        runCatching { context.unregisterReceiver(screenStateReceiver) }
        _currentSpeedKmh.value = 0f
    }
}
