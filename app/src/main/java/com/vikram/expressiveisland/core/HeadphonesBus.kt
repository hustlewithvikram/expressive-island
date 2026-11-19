package com.vikram.expressiveisland.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HeadphonesState(
    val name: String?,
    val type: Int,
    val leftBattery: Int? = null,
    val rightBattery: Int? = null,
    val caseBattery: Int? = null,
)

object HeadphonesBus {

    private val _state = MutableStateFlow<HeadphonesState?>(null)

    val state: StateFlow<HeadphonesState?> = _state.asStateFlow()

    fun update(state: HeadphonesState?) {
        _state.value = state
    }
}