package com.autohub.park.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Servisteki tek player'in o an çaldığı yayın bilgisi.
 * Telefon mini player bunu dinler (aynı proses).
 */
object NowPlayingStore {
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _subtitle = MutableStateFlow("")
    val subtitle: StateFlow<String> = _subtitle.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    fun update(title: String, subtitle: String, playing: Boolean) {
        _title.value = title
        _subtitle.value = subtitle
        _isPlaying.value = playing
    }

    fun clear() {
        _title.value = ""
        _subtitle.value = ""
        _isPlaying.value = false
    }
}
