package com.autolounge.park.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CarUsageTracker @Inject constructor() {
    private val _isCarScreenActive = MutableStateFlow(false)
    val isCarScreenActive: StateFlow<Boolean> = _isCarScreenActive.asStateFlow()

    private val _isAudioStreamingActive = MutableStateFlow(false)
    val isAudioStreamingActive: StateFlow<Boolean> = _isAudioStreamingActive.asStateFlow()

    fun setCarScreenActive(active: Boolean) {
        _isCarScreenActive.value = active
    }

    fun setAudioStreamingActive(active: Boolean) {
        _isAudioStreamingActive.value = active
    }
}
