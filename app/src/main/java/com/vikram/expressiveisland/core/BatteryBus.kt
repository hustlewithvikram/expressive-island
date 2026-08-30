package com.vikram.expressiveisland.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatteryState(
    val percentage: Int,
    val isCharging: Boolean,
)

object BatteryBus {

    private val _state = MutableStateFlow<BatteryState?>(null)

    val state: StateFlow<BatteryState?> = _state.asStateFlow()

    fun update(state: BatteryState?) {
        _state.value = state
    }
}