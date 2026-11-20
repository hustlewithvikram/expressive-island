package com.vikram.expressiveisland.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ChargingState(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val pluggedType: Int,
    val voltageMv: Int?,
    val currentUa: Long?,
    val powerWatts: Float?,
    val temperatureTenthsC: Int?,
    val timeToFullMs: Long?,
) {
    val temperatureCelsius: Float?
        get() = temperatureTenthsC?.div(10f)

    val voltageVolts: Float?
        get() = voltageMv?.div(1000f)

    val timeToFullMinutes: Long?
        get() = timeToFullMs
            ?.takeIf { it > 0L }
            ?.div(60_000L)
}

object ChargingBus {

    private val _state = MutableStateFlow<ChargingState?>(null)
    private val _previewState = MutableStateFlow<ChargingState?>(null)

    val state: StateFlow<ChargingState?> = _state.asStateFlow()
    val previewState: StateFlow<ChargingState?> = _previewState.asStateFlow()

    fun update(state: ChargingState?) {
        _state.value = state
        // Real battery data always takes precedence over a settings-test preview.
        if (state != null) _previewState.value = null
    }

    fun showPreview(state: ChargingState) {
        _previewState.value = state
    }

    fun clearPreview() {
        _previewState.value = null
    }
}