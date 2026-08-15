package com.vikram.expressiveisland.events

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.vikram.expressiveisland.core.ChargingBus
import com.vikram.expressiveisland.core.ChargingState
import com.vikram.expressiveisland.core.CutoutSignal
import com.vikram.expressiveisland.core.IslandEventBus
import com.vikram.expressiveisland.core.SystemEventType
import kotlin.math.abs

/**
 * Listens for device-level events and republishes them on [IslandEventBus].
 *
 * Charging has a live state of its own through [ChargingBus], just like music uses [NowPlayingBus].
 * The system event only tells the island that charging started/stopped; the bus carries the
 * continuously changing battery details used by the expanded charging UI.
 */
class SystemEventMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService<ConnectivityManager>()

    private val audioManager =
        context.getSystemService<AudioManager>()

    private val batteryManager =
        context.getSystemService<BatteryManager>()

    @Volatile
    private var isLowBatteryState = false

    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {

            when (intent.action) {

                Intent.ACTION_POWER_CONNECTED -> {
                    isLowBatteryState = false

                    updateChargingState(intent)

                    emit(SystemEventType.CHARGING_STARTED)
                }

                Intent.ACTION_POWER_DISCONNECTED -> {
                    ChargingBus.update(null)

                    emit(SystemEventType.CHARGING_STOPPED)
                }

                Intent.ACTION_BATTERY_CHANGED -> {
                    updateChargingState(intent)
                }

                Intent.ACTION_BATTERY_LOW -> {
                    if (!isLowBatteryState) {
                        isLowBatteryState = true
                        emit(SystemEventType.BATTERY_LOW)
                    }
                }

                Intent.ACTION_BATTERY_OKAY -> {
                    isLowBatteryState = false
                }

                Intent.ACTION_USER_PRESENT ->
                    emit(SystemEventType.DEVICE_UNLOCKED)

                UsbManager.ACTION_USB_DEVICE_ATTACHED ->
                    emit(SystemEventType.USB_MOUNTED)

                UsbManager.ACTION_USB_DEVICE_DETACHED ->
                    emit(SystemEventType.USB_UNMOUNTED)
            }
        }
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {

        override fun onAudioDevicesAdded(
            addedDevices: Array<out AudioDeviceInfo>
        ) {
            if (addedDevices.any { it.isHeadphone }) {
                emit(SystemEventType.HEADPHONES_CONNECTED)
            }
        }

        override fun onAudioDevicesRemoved(
            removedDevices: Array<out AudioDeviceInfo>
        ) {
            if (removedDevices.any { it.isHeadphone }) {
                emit(SystemEventType.HEADPHONES_DISCONNECTED)
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            emit(SystemEventType.WIFI_CONNECTED)
        }

        override fun onLost(network: Network) {
            emit(SystemEventType.WIFI_DISCONNECTED)
        }
    }

    fun start() {

        ContextCompat.registerReceiver(
            context,
            broadcastReceiver,
            buildIntentFilter(),
            ContextCompat.RECEIVER_EXPORTED,
        )

        audioManager?.registerAudioDeviceCallback(
            audioDeviceCallback,
            null,
        )

        connectivityManager?.registerNetworkCallback(
            wifiRequest(),
            networkCallback,
        )

        // Populate charging state immediately if the monitor starts while
        // the device is already plugged in.
        context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )?.let { intent ->
            updateChargingState(intent)
        }
    }

    fun stop() {

        runCatching {
            context.unregisterReceiver(broadcastReceiver)
        }

        audioManager?.unregisterAudioDeviceCallback(
            audioDeviceCallback
        )

        connectivityManager?.unregisterNetworkCallback(
            networkCallback
        )

        ChargingBus.update(null)
    }

    private fun updateChargingState(intent: Intent) {

        val status = intent.getIntExtra(
            BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN,
        )

        val isCharging =
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

        if (!isCharging) {
            ChargingBus.update(null)
            return
        }

        val level = intent.getIntExtra(
            BatteryManager.EXTRA_LEVEL,
            -1,
        )

        val scale = intent.getIntExtra(
            BatteryManager.EXTRA_SCALE,
            100,
        )

        val batteryPercent =
            if (level >= 0 && scale > 0) {
                ((level * 100f) / scale)
                    .toInt()
                    .coerceIn(0, 100)
            } else {
                -1
            }

        val voltageMv =
            intent.getIntExtra(
                BatteryManager.EXTRA_VOLTAGE,
                -1,
            ).takeIf { it > 0 }

        val temperatureTenthsC =
            intent.getIntExtra(
                BatteryManager.EXTRA_TEMPERATURE,
                -1,
            ).takeIf { it > 0 }

        val currentUa =
            batteryManager
                ?.getIntProperty(
                    BatteryManager.BATTERY_PROPERTY_CURRENT_NOW,
                )
                ?.toLong()
                ?.takeIf { it != 0L }

        val powerWatts =
            if (voltageMv != null && currentUa != null) {
                abs(
                    voltageMv.toFloat() *
                            currentUa.toFloat()
                ) / 1_000_000_000f
            } else {
                null
            }

        val timeToFullMs =
            batteryManager
                ?.computeChargeTimeRemaining()
                ?.takeIf { it > 0L }

        val pluggedType =
            intent.getIntExtra(
                BatteryManager.EXTRA_PLUGGED,
                0,
            )

        ChargingBus.update(
            ChargingState(
                batteryPercent = batteryPercent,
                isCharging = true,
                pluggedType = pluggedType,
                voltageMv = voltageMv,
                currentUa = currentUa,
                powerWatts = powerWatts,
                temperatureTenthsC = temperatureTenthsC,
                timeToFullMs = timeToFullMs,
            )
        )
    }

    private fun emit(type: SystemEventType) {
        IslandEventBus.emit(
            CutoutSignal.System(type)
        )
    }

    private fun buildIntentFilter() =
        IntentFilter().apply {

            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)

            // Live battery updates while charging.
            addAction(Intent.ACTION_BATTERY_CHANGED)

            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)

            addAction(Intent.ACTION_USER_PRESENT)

            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }

    private fun wifiRequest() =
        NetworkRequest.Builder()
            .addTransportType(
                NetworkCapabilities.TRANSPORT_WIFI
            )
            .build()

    private val AudioDeviceInfo.isHeadphone: Boolean
        get() = type in HEADPHONE_TYPES

    private companion object {

        val HEADPHONE_TYPES = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_USB_HEADSET,
        )
    }
}