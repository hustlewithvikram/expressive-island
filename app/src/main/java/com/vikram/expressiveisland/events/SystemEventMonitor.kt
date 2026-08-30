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
import android.net.wifi.WifiManager
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.vikram.expressiveisland.core.ChargingBus
import com.vikram.expressiveisland.core.ChargingState
import com.vikram.expressiveisland.core.CutoutSignal
import com.vikram.expressiveisland.core.IslandEventBus
import com.vikram.expressiveisland.core.SystemEventType
import com.vikram.expressiveisland.core.WifiBus
import com.vikram.expressiveisland.core.WifiState
import kotlin.math.abs
import android.net.LinkProperties
import android.net.wifi.WifiInfo
import android.util.Log
import com.vikram.expressiveisland.core.BatteryBus
import com.vikram.expressiveisland.core.BatteryState
import com.vikram.expressiveisland.core.HeadphonesBus
import com.vikram.expressiveisland.core.HeadphonesState
import kotlin.math.roundToInt

/**
 * Listens for device-level events and republishes them on [IslandEventBus].
 *
 * Charging has a live state of its own through [ChargingBus], just like music uses [NowPlayingBus].
 * The system event only tells the island that charging started/stopped; the bus carries the
 * continuously changing battery details used by the expanded charging UI.
 */
class SystemEventMonitor(private val context: Context) {

    private val wifiManager =
        context.applicationContext.getSystemService<WifiManager>()

    private val audioManager =
        context.getSystemService<AudioManager>()

    private val batteryManager =
        context.getSystemService<BatteryManager>()

    private val connectivityManager =
        context.getSystemService<ConnectivityManager>()

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

                Intent.ACTION_SCREEN_OFF -> 
                    emit(SystemEventType.DEVICE_LOCKED)

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
            addedDevices
                .firstOrNull { it.isHeadphone }
                ?.let { device ->
                    HeadphonesBus.update(
                        HeadphonesState(
                            name = device.productName?.toString(),
                            type = device.type,
                        )
                    )

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

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                Log.d(
                    "WifiDebug",
                    "onAvailable() network=$network"
                )

                emit(SystemEventType.WIFI_CONNECTED)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities,
            ) {
                Log.d(
                    "WifiDebug",
                    "onCapabilitiesChanged() network=$network"
                )

                updateWifiState(
                    network,
                    capabilities,
                )
            }

            override fun onLinkPropertiesChanged(
                network: Network,
                linkProperties: LinkProperties,
            ) {
                Log.d(
                    "WifiDebug",
                    "onLinkPropertiesChanged() network=$network"
                )

                updateWifiState(
                    network,
                    connectivityManager
                        ?.getNetworkCapabilities(network),
                )
            }

            override fun onLost(network: Network) {
                Log.d(
                    "WifiDebug",
                    "onLost() network=$network"
                )

                WifiBus.update(null)

                emit(SystemEventType.WIFI_DISCONNECTED)
            }
        }

    private fun updateWifiState(
        network: Network,
        capabilities: NetworkCapabilities?,
    ) {
        Log.d(
            "WifiDebug",
            "========== updateWifiState START =========="
        )

        try {
            val connectivity = connectivityManager

            if (connectivity == null) {
                Log.e("WifiDebug", "FAIL: connectivityManager == null")
                return
            }

            Log.d(
                "WifiDebug",
                "capabilities=$capabilities"
            )

            if (capabilities == null) {
                Log.e(
                    "WifiDebug",
                    "FAIL: capabilities == null"
                )
                return
            }

            val isWifi =
                capabilities.hasTransport(
                    NetworkCapabilities.TRANSPORT_WIFI
                )

            Log.d(
                "WifiDebug",
                "isWifi=$isWifi"
            )

            if (!isWifi) {
                Log.e(
                    "WifiDebug",
                    "FAIL: network is not WIFI"
                )
                return
            }

            val transportInfo =
                capabilities.transportInfo

            Log.d(
                "WifiDebug",
                "transportInfo=$transportInfo"
            )

            val wifiInfo =
                transportInfo as? WifiInfo

            Log.d(
                "WifiDebug",
                "wifiInfo=$wifiInfo"
            )

            if (wifiInfo == null) {
                Log.e(
                    "WifiDebug",
                    "FAIL: transportInfo is not WifiInfo"
                )
                return
            }

            Log.d(
                "WifiDebug",
                "SSID=${wifiInfo.ssid}"
            )

            Log.d(
                "WifiDebug",
                "RSSI=${wifiInfo.rssi}"
            )

            Log.d(
                "WifiDebug",
                "linkSpeed=${wifiInfo.linkSpeed}"
            )

            Log.d(
                "WifiDebug",
                "frequency=${wifiInfo.frequency}"
            )

            val linkProperties =
                connectivity.getLinkProperties(network)

            Log.d(
                "WifiDebug",
                "linkProperties=$linkProperties"
            )

            val ipAddress = linkProperties
                ?.linkAddresses
                ?.firstOrNull { linkAddress ->
                    linkAddress.address.hostAddress
                        ?.contains(":") == false
                }
                ?.address
                ?.hostAddress

            Log.d(
                "WifiDebug",
                "ipAddress=$ipAddress"
            )

            val signalLevel =
                WifiManager.calculateSignalLevel(
                    wifiInfo.rssi,
                    5,
                )

            val ssid = wifiInfo.ssid
                ?.removePrefix("\"")
                ?.removeSuffix("\"")
                ?.takeUnless {
                    it.isBlank() ||
                            it == "<unknown ssid>"
                }

            val state = WifiState(
                ssid = ssid,
                signalLevel = signalLevel,
                rssi = wifiInfo.rssi.takeIf {
                    it != -127
                },
                linkSpeedMbps =
                    wifiInfo.linkSpeed.takeIf {
                        it > 0
                    },
                frequencyMhz =
                    wifiInfo.frequency.takeIf {
                        it > 0
                    },
                ipAddress = ipAddress,
                isMetered =
                    capabilities
                        .hasCapability(
                            NetworkCapabilities
                                .NET_CAPABILITY_NOT_METERED
                        )
                        .not(),
            )

            Log.d(
                "WifiDebug",
                "CREATED STATE=$state"
            )

            WifiBus.update(state)

            Log.d(
                "WifiDebug",
                "WifiBus AFTER UPDATE=${WifiBus.state.value}"
            )

            Log.d(
                "WifiDebug",
                "========== updateWifiState SUCCESS =========="
            )

        } catch (e: SecurityException) {

            Log.e(
                "WifiDebug",
                "SECURITY EXCEPTION",
                e,
            )

            WifiBus.update(null)

        } catch (e: Exception) {

            Log.e(
                "WifiDebug",
                "GENERAL EXCEPTION",
                e,
            )

            WifiBus.update(null)
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
        WifiBus.update(null)
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

        val percentage = if (level >= 0 && scale > 0) {
            ((level * 100f) / scale).roundToInt()
        } else {
            -1
        }

        if (percentage >= 0) {
            BatteryBus.update(
                BatteryState(
                    percentage = percentage.coerceIn(0, 100),
                    isCharging = isCharging,
                )
            )
        }

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

            addAction(Intent.ACTION_SCREEN_OFF)
            
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