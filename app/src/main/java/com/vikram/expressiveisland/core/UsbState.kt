package com.vikram.expressiveisland.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Best-effort details for the single active USB connection.
 * Device-mode (phone connected to a computer) metadata is intentionally limited
 * to the USB functions Android exposes through its system USB-state broadcast.
 */
data class UsbState(
    val name: String,
    val deviceType: String,
    val manufacturer: String? = null,
    val deviceId: Int? = null,
    val vendorId: Int? = null,
    val productId: Int? = null,
    val connectionMode: String,
    val isPcConnection: Boolean = false,
)

object UsbBus {
    private val _state = MutableStateFlow<UsbState?>(null)
    val state: StateFlow<UsbState?> = _state.asStateFlow()

    fun update(state: UsbState?) {
        _state.value = state
    }
}
