package com.vikram.expressiveisland.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WifiState(
    val ssid: String?,
    val signalLevel: Int?,
    val rssi: Int?,
    val linkSpeedMbps: Int?,
    val frequencyMhz: Int?,
    val ipAddress: String?,
    val isMetered: Boolean?,
) {
    val band: String?
        get() = when {
            frequencyMhz == null -> null
            frequencyMhz in 2400..2500 -> "2.4 GHz"
            frequencyMhz in 4900..5900 -> "5 GHz"
            frequencyMhz >= 5900 -> "6 GHz"
            else -> null
        }
}

object WifiBus {

    private val _state = MutableStateFlow<WifiState?>(null)

    val state: StateFlow<WifiState?> = _state.asStateFlow()

    fun update(state: WifiState?) {
        _state.value = state
    }
}