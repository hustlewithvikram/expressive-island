package com.vikram.expressiveisland.events

import android.bluetooth.BluetoothDevice
import android.app.NotificationManager
import android.app.KeyguardManager
import android.bluetooth.BluetoothManager
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.nfc.NfcAdapter
import android.os.PowerManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.database.ContentObserver
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.view.Display
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.vikram.expressiveisland.permissions.Permissions
import com.vikram.expressiveisland.core.ChargingBus
import com.vikram.expressiveisland.core.ChargingState
import com.vikram.expressiveisland.core.CutoutSignal
import com.vikram.expressiveisland.core.IslandEventBus
import com.vikram.expressiveisland.core.SystemEventType
import com.vikram.expressiveisland.core.WifiBus
import com.vikram.expressiveisland.core.WifiState
import kotlin.math.abs
import android.net.LinkProperties
import android.net.NetworkCapabilities.TRANSPORT_VPN
import android.net.wifi.WifiInfo
import android.net.wifi.WifiConfiguration
import android.provider.Settings
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

    private val bluetoothManager =
        context.getSystemService<BluetoothManager>()

    private val notificationManager = context.getSystemService<NotificationManager>()
    private val powerManager = context.getSystemService<PowerManager>()
    private val keyguardManager = context.getSystemService<KeyguardManager>()
    private val cameraManager = context.getSystemService<CameraManager>()
    private val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var airplaneMode: Boolean? = null
    private var dndEnabled: Boolean? = null
    private var batterySaverEnabled: Boolean? = null
    private var nfcEnabled: Boolean? = null
    private var locationEnabled: Boolean? = null
    private var flashlightEnabled: Boolean? = null
    private var dataSaverEnabled: Boolean? = null
    private var autoRotateEnabled: Boolean? = null
    private var thermalWarningActive: Boolean? = null
    @Volatile private var deviceWasLocked = false
    private val activeTorchCameras = mutableSetOf<String>()

    private val flashlightCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            synchronized(activeTorchCameras) {
                if (enabled) activeTorchCameras.add(cameraId) else activeTorchCameras.remove(cameraId)
                updateFlashlightState(activeTorchCameras.isNotEmpty())
            }
        }
    }
    private val autoRotateObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean) = updateAutoRotateState()
    }
    private val thermalStatusListener = PowerManager.OnThermalStatusChangedListener { status ->
        updateThermalState(status)
    }
    private val locationObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean) = updateLocationState()
    }

    @Volatile
    private var defaultNetworkIsVpn = false
    private val vpnNetworks = mutableSetOf<Network>()
    @Volatile
    private var lastPublishedVpnState = false
    private val wifiNetworks = mutableSetOf<Network>()
    private val bluetoothDevices = mutableSetOf<String>()
    private val headphoneDevices = mutableSetOf<Int>()
    @Volatile
    private var isLowBatteryState = false

    @Volatile
    private var wasBatteryFull = false

    @Volatile
    private var wasHotspotEnabled = false

    @Volatile
    private var lastRingerMode = AudioManager.RINGER_MODE_NORMAL

    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {

            when (intent.action) {

                Intent.ACTION_POWER_CONNECTED -> {
                    isLowBatteryState = false
                    updateChargingState(intent)
                    emit(SystemEventType.CHARGING_STARTED)
                }

                Intent.ACTION_POWER_DISCONNECTED -> {
                    wasBatteryFull = false
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

                Intent.ACTION_SCREEN_OFF -> {
                    // Screen-off is not equivalent to a lock (e.g. timeout with no secure lock).
                    // Check after Android has had a moment to update keyguard state.
                    mainHandler.postDelayed({
                        val locked = keyguardManager?.isKeyguardLocked == true
                        if (locked && !deviceWasLocked) emit(SystemEventType.DEVICE_LOCKED)
                        deviceWasLocked = locked
                    }, 500L)
                }

                Intent.ACTION_USER_PRESENT -> {
                    if (deviceWasLocked) emit(SystemEventType.DEVICE_UNLOCKED)
                    deviceWasLocked = false
                }

                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    emit(SystemEventType.USB_MOUNTED)

                    if (isAdbConnection(intent)) {
                        emit(SystemEventType.ADB_CONNECTED)
                    }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    emit(SystemEventType.USB_UNMOUNTED)

                    if (isAdbConnection(intent)) {
                        emit(SystemEventType.ADB_DISCONNECTED)
                    }
                }

                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(
                        BluetoothDevice.EXTRA_DEVICE
                    )

                    device?.address?.let { address ->
                        if (bluetoothDevices.add(address)) {
                            emit(SystemEventType.BLUETOOTH_CONNECTED)
                        }
                    }
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(
                        BluetoothDevice.EXTRA_DEVICE
                    )

                    device?.address?.let { address ->
                        if (bluetoothDevices.remove(address)) {
                            emit(SystemEventType.BLUETOOTH_DISCONNECTED)
                        }
                    }
                }

                WIFI_DETAILS_REFRESH_ACTION,
                WifiManager.NETWORK_STATE_CHANGED_ACTION,
                WifiManager.WIFI_STATE_CHANGED_ACTION -> refreshCurrentWifiState()

                Intent.ACTION_AIRPLANE_MODE_CHANGED -> updateAirplaneModeState()
                NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> updateDndState()
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> updateBatterySaverState()
                NfcAdapter.ACTION_ADAPTER_STATE_CHANGED -> updateNfcState()
                LocationManager.MODE_CHANGED_ACTION -> updateLocationState()
                ConnectivityManager.ACTION_RESTRICT_BACKGROUND_CHANGED -> updateDataSaverState()

                "android.net.wifi.WIFI_AP_STATE_CHANGED" -> {
                    val state = intent.getIntExtra(
                        "wifi_state",
                        -1,
                    )

                    when (state) {
                        13 -> {
                            if (!wasHotspotEnabled) {
                                wasHotspotEnabled = true
                                emit(SystemEventType.HOTSPOT_ENABLED)
                            }
                        }

                        11 -> {
                            if (wasHotspotEnabled) {
                                wasHotspotEnabled = false
                                emit(SystemEventType.HOTSPOT_DISABLED)
                            }
                        }
                    }
                }

                AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                    val mode = intent.getIntExtra(
                        AudioManager.EXTRA_RINGER_MODE,
                        AudioManager.RINGER_MODE_NORMAL,
                    )

                    if (mode == lastRingerMode) {
                        return
                    }

                    lastRingerMode = mode

                    when (mode) {
                        AudioManager.RINGER_MODE_NORMAL -> {
                            emit(SystemEventType.RINGER_NORMAL)
                        }

                        AudioManager.RINGER_MODE_VIBRATE -> {
                            emit(SystemEventType.RINGER_VIBRATE)
                        }

                        AudioManager.RINGER_MODE_SILENT -> {
                            emit(SystemEventType.RINGER_SILENT)
                        }
                    }
                }
            }
        }
    }
    private val audioDeviceCallback = object : AudioDeviceCallback() {

        override fun onAudioDevicesAdded(
            addedDevices: Array<out AudioDeviceInfo>,
        ) {
            addedDevices
                .filter { it.isHeadphone }
                .forEach { device ->
                    if (headphoneDevices.add(device.id)) {
                        HeadphonesBus.update(
                            HeadphonesState(
                                name = device.productName?.toString(),
                                type = device.type,
                            )
                        )
                        // Bluetooth connection events are handled by BLUETOOTH_CONNECTED; avoid a duplicate headphone event.
                    }
                }
        }

        override fun onAudioDevicesRemoved(
            removedDevices: Array<out AudioDeviceInfo>,
        ) {
            removedDevices
                .filter { it.isHeadphone }
                .forEach { device -> headphoneDevices.remove(device.id) }
            if (headphoneDevices.isEmpty() && removedDevices.any { it.isHeadphone }) {
                HeadphonesBus.update(null)
                // The Bluetooth disconnect event is the single source of truth for this accessory.
            }
        }
    }

    private val wifiNetworkCallback: ConnectivityManager.NetworkCallback =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            object : ConnectivityManager.NetworkCallback(
                ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO,
            ) {
                override fun onAvailable(network: Network) {
                    if (wifiNetworks.add(network)) emit(SystemEventType.WIFI_CONNECTED)
                    updateWifiState(network, connectivityManager?.getNetworkCapabilities(network))
                }

                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    updateWifiState(network, capabilities)
                }

                override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                    updateWifiState(network, connectivityManager?.getNetworkCapabilities(network))
                }

                override fun onLost(network: Network) {
                    if (wifiNetworks.remove(network) && wifiNetworks.isEmpty()) {
                        WifiBus.update(null)
                        emit(SystemEventType.WIFI_DISCONNECTED)
                    }
                }
            }
        } else {
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (wifiNetworks.add(network)) emit(SystemEventType.WIFI_CONNECTED)
                    updateWifiState(network, connectivityManager?.getNetworkCapabilities(network))
                }

                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    updateWifiState(network, capabilities)
                }

                override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                    updateWifiState(network, connectivityManager?.getNetworkCapabilities(network))
                }

                override fun onLost(network: Network) {
                    if (wifiNetworks.remove(network) && wifiNetworks.isEmpty()) {
                        WifiBus.update(null)
                        emit(SystemEventType.WIFI_DISCONNECTED)
                    }
                }
            }
        }

    /**
     * Observe the device's active default network rather than requesting TRANSPORT_VPN
     * directly. Android's VPN network visibility can make transport-specific requests
     * unreliable for ordinary apps, while the default-network callback reports the
     * capabilities of the route currently used by the device.
     */
    private val defaultNetworkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities,
            ) {
                defaultNetworkIsVpn = capabilities.hasTransport(TRANSPORT_VPN)
                publishVpnState("default network capabilities changed")
            }

            override fun onLost(network: Network) {
                // A replacement default network reports its capabilities separately.
                Log.d("SystemEventMonitor", "Default network lost: $network")
            }
        }

    /**
     * Also observe VPN transports directly. clearCapabilities() is essential here:
     * NetworkRequest.Builder otherwise adds NOT_VPN by default, which excludes VPNs.
     * This catches VPN networks even when they are not the device's default route.
     */
    private val vpnNetworkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                vpnNetworks.add(network)
                publishVpnState("VPN network available: $network")
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities,
            ) {
                if (capabilities.hasTransport(TRANSPORT_VPN)) {
                    vpnNetworks.add(network)
                } else {
                    vpnNetworks.remove(network)
                }
                publishVpnState("VPN capabilities changed: $network")
            }

            override fun onLost(network: Network) {
                vpnNetworks.remove(network)
                publishVpnState("VPN network lost: $network")
            }
        }

    private fun publishVpnState(reason: String) {
        val isVpnActive = defaultNetworkIsVpn || vpnNetworks.isNotEmpty()
        if (lastPublishedVpnState == isVpnActive) return
        lastPublishedVpnState = isVpnActive
        Log.i(
            "SystemEventMonitor",
            "VPN state changed: active=$isVpnActive ($reason)",
        )
        emit(
            if (isVpnActive) SystemEventType.VPN_CONNECTED
            else SystemEventType.VPN_DISCONNECTED
        )
    }

    private fun updateWifiState(
        network: Network,
        capabilities: NetworkCapabilities?,
    ) {
        try {
            val connectivity = connectivityManager ?: return
            if (capabilities == null ||
                !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            ) return

            val wifiInfo = capabilities.transportInfo as? WifiInfo
                ?: runCatching { wifiManager?.connectionInfo }.getOrNull()
                ?: return
            val linkProperties = runCatching {
                connectivity.getLinkProperties(network)
            }.getOrNull()
            val ipAddress = linkProperties
                ?.linkAddresses
                ?.firstOrNull { it.address.hostAddress?.contains(":") == false }
                ?.address
                ?.hostAddress

            // FLAG_INCLUDE_LOCATION_INFO above allows Android 12+ callbacks to expose
            // location-sensitive Wi-Fi details when the user has granted permission.
            // Some devices still redact the SSID, so try WifiManager as a fallback.
            val ssid = resolveWifiSsid(wifiInfo)

            val managerInfo = runCatching { wifiManager?.connectionInfo }.getOrNull()
            val rssi = runCatching { wifiInfo.rssi }
                .getOrNull()
                ?.takeIf { it != -127 }
                ?: runCatching { managerInfo?.rssi }.getOrNull()?.takeIf { it != -127 }
            val signalLevel = rssi?.let {
                WifiManager.calculateSignalLevel(it, 5)
            }
            val linkSpeed = runCatching { wifiInfo.linkSpeed }
                .getOrNull()
                ?.takeIf { it > 0 }
            val frequency = runCatching { wifiInfo.frequency }
                .getOrNull()
                ?.takeIf { it > 0 }
                ?: runCatching { managerInfo?.frequency }.getOrNull()?.takeIf { it > 0 }

            WifiBus.update(
                WifiState(
                    ssid = ssid,
                    signalLevel = signalLevel,
                    rssi = rssi,
                    linkSpeedMbps = linkSpeed,
                    frequencyMhz = frequency,
                    ipAddress = ipAddress,
                    security = resolveWifiSecurity(wifiInfo, ssid),
                    isMetered = !capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_NOT_METERED
                    ),
                )
            )
        } catch (e: SecurityException) {
            Log.w("SystemEventMonitor", "Wi-Fi details are restricted; keeping connection visible", e)
            // Keep a connected state even when Android blocks access to detailed Wi-Fi info.
            WifiBus.update(
                WifiState(
                    ssid = null,
                    signalLevel = null,
                    rssi = null,
                    linkSpeedMbps = null,
                    frequencyMhz = null,
                    ipAddress = null,
                    isMetered = null,
                )
            )
        } catch (e: Exception) {
            Log.w("SystemEventMonitor", "Unable to read Wi-Fi state", e)
        }
    }

    /**
     * WifiInfo exposes the negotiated security type on Android 12+. Older releases do not
     * expose it through the same public API, so the UI does not guess a security label.
     */
    private fun resolveWifiSecurity(wifiInfo: WifiInfo, ssid: String?): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val type = runCatching { wifiInfo.currentSecurityType }.getOrNull()
            when (type) {
                0 -> return "Open"
                1 -> return "WEP"
                2, 3 -> return "WPA/WPA2"
                4, 5, 9 -> return "WPA3"
                6 -> return "OWE"
                7, 8 -> return "WAPI"
            }
        }

        // Android 10/11 do not expose WifiInfo.currentSecurityType. When location
        // permission allows configured-network access, infer the active profile's type.
        val normalizedSsid = ssid?.trim('"') ?: return null
        val configs = runCatching { wifiManager?.configuredNetworks }.getOrNull() ?: return null
        val active = configs.firstOrNull { it.SSID?.trim('"') == normalizedSsid } ?: return null
        val keys = active.allowedKeyManagement
        return when {
            keys.get(WifiConfiguration.KeyMgmt.SAE) -> "WPA3"
            keys.get(WifiConfiguration.KeyMgmt.WPA_PSK) ||
                keys.get(WifiConfiguration.KeyMgmt.WPA_EAP) ||
                keys.get(WifiConfiguration.KeyMgmt.IEEE8021X) -> "WPA/WPA2"
            active.wepKeys?.any { !it.isNullOrBlank() } == true -> "WEP"
            else -> "Open"
        }
    }

    private fun resolveWifiSsid(wifiInfo: WifiInfo): String? {
        if (!Permissions.isWifiSsidAccessGranted(context)) {
            Log.i("SystemEventMonitor", "Wi-Fi SSID permission not granted")
            return null
        }
        if (!Permissions.isLocationEnabledForWifi(context)) {
            Log.i("SystemEventMonitor", "Location services are disabled; Android may redact Wi-Fi SSID")
            return null
        }

        fun normalize(value: String?): String? {
            return value
                ?.removePrefix("\"")
                ?.removeSuffix("\"")
                ?.takeUnless {
                    it.isBlank() ||
                        it.equals("<unknown ssid>", ignoreCase = true) ||
                        it.equals("unknown ssid", ignoreCase = true)
                }
        }

        normalize(runCatching { wifiInfo.ssid }.getOrNull())?.let { return it }

        // Prefer WifiManager's active connection as a second source. Android itself
        // redacts this value when the required runtime permissions are missing.
        normalize(runCatching { wifiManager?.connectionInfo?.ssid }.getOrNull())?.let { return it }

        // Last compatibility fallback for older releases/OEM implementations.
        return normalize(runCatching {
            @Suppress("DEPRECATION")
            wifiManager?.connectionInfo?.ssid
        }.getOrNull())
    }

    private fun refreshCurrentWifiState() {
        val connectivity = connectivityManager ?: return
        val network = connectivity.activeNetwork ?: return
        val capabilities = runCatching { connectivity.getNetworkCapabilities(network) }.getOrNull()
        if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            updateWifiState(network, capabilities)
        }
    }

    private fun isAdbConnection(intent: Intent): Boolean {
        val device =
            intent.getParcelableExtra<android.hardware.usb.UsbDevice>(
                UsbManager.EXTRA_DEVICE
            ) ?: return false

        return device.interfaceCount > 0 &&
                (0 until device.interfaceCount).any { index ->
                    device.getInterface(index).interfaceClass == 255 &&
                            device.getInterface(index).interfaceSubclass == 66 &&
                            device.getInterface(index).interfaceProtocol == 1
                }
    }

    fun start() {

        airplaneMode = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        dndEnabled = notificationManager?.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        batterySaverEnabled = powerManager?.isPowerSaveMode == true
        nfcEnabled = runCatching { nfcAdapter?.isEnabled }.getOrNull()
        locationEnabled = isLocationEnabled()
        flashlightEnabled = false
        dataSaverEnabled = connectivityManager?.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
        autoRotateEnabled = Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1) == 1
        thermalWarningActive = null
        deviceWasLocked = keyguardManager?.isKeyguardLocked == true
        synchronized(activeTorchCameras) { activeTorchCameras.clear() }

        lastRingerMode = audioManager?.ringerMode
            ?: AudioManager.RINGER_MODE_NORMAL

        ContextCompat.registerReceiver(
            context,
            broadcastReceiver,
            buildIntentFilter(),
            ContextCompat.RECEIVER_EXPORTED,
        )

        audioManager?.registerAudioDeviceCallback(
            audioDeviceCallback,
            mainHandler,
        )

        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.LOCATION_MODE), false, locationObserver
        )
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), false, autoRotateObserver
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            powerManager?.addThermalStatusListener(thermalStatusListener)
        }
        cameraManager?.registerTorchCallback(flashlightCallback, mainHandler)

        connectivityManager?.registerNetworkCallback(
            wifiRequest(),
            wifiNetworkCallback,
        )
        connectivityManager?.registerDefaultNetworkCallback(defaultNetworkCallback)
        connectivityManager?.registerNetworkCallback(vpnRequest(), vpnNetworkCallback)
        updateAirplaneModeState()
        updateDndState()
        updateBatterySaverState()
        updateNfcState()
        updateLocationState()
        updateDataSaverState()
        updateAutoRotateState()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) updateThermalState(powerManager?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE)

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
        context.contentResolver.unregisterContentObserver(locationObserver)
        context.contentResolver.unregisterContentObserver(autoRotateObserver)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { powerManager?.removeThermalStatusListener(thermalStatusListener) }
        }
        runCatching { cameraManager?.unregisterTorchCallback(flashlightCallback) }

        connectivityManager?.unregisterNetworkCallback(wifiNetworkCallback)
        connectivityManager?.unregisterNetworkCallback(defaultNetworkCallback)
        connectivityManager?.unregisterNetworkCallback(vpnNetworkCallback)

        defaultNetworkIsVpn = false
        vpnNetworks.clear()
        lastPublishedVpnState = false
        wifiNetworks.clear()
        bluetoothDevices.clear()
        wasHotspotEnabled = false

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

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val percentage = if (level >= 0 && scale > 0) {
            ((level * 100f) / scale).roundToInt().coerceIn(0, 100)
        } else {
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                ?.takeIf { it in 0..100 }
        }
        if (percentage != null) {
            BatteryBus.update(BatteryState(percentage = percentage, isCharging = isCharging))
        }

        if (!isCharging) {
            wasBatteryFull = false
            ChargingBus.update(null)
            return
        }

        if (status == BatteryManager.BATTERY_STATUS_FULL && !wasBatteryFull) {
            wasBatteryFull = true
            emit(SystemEventType.CHARGING_COMPLETE)
        } else if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            wasBatteryFull = false
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

    private fun updateAirplaneModeState() {
        val enabled = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        val previous = airplaneMode
        airplaneMode = enabled
        if (previous != null && previous != enabled) emit(if (enabled) SystemEventType.AIRPLANE_MODE_ENABLED else SystemEventType.AIRPLANE_MODE_DISABLED)
    }

    private fun updateDndState() {
        val enabled = notificationManager?.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        val previous = dndEnabled
        dndEnabled = enabled
        if (previous != null && previous != enabled) emit(if (enabled) SystemEventType.DND_ENABLED else SystemEventType.DND_DISABLED)
    }

    private fun updateBatterySaverState() {
        val enabled = powerManager?.isPowerSaveMode == true
        val previous = batterySaverEnabled
        batterySaverEnabled = enabled
        if (previous != null && previous != enabled) emit(if (enabled) SystemEventType.BATTERY_SAVER_ENABLED else SystemEventType.BATTERY_SAVER_DISABLED)
    }

    private fun updateNfcState() {
        val enabled = runCatching { nfcAdapter?.isEnabled }.getOrNull() ?: return
        val previous = nfcEnabled
        nfcEnabled = enabled
        if (previous != null && previous != enabled) emit(if (enabled) SystemEventType.NFC_ENABLED else SystemEventType.NFC_DISABLED)
    }

    private fun isLocationEnabled(): Boolean = runCatching {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            context.getSystemService<LocationManager>()?.isLocationEnabled == true
        } else {
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF) != Settings.Secure.LOCATION_MODE_OFF
        }
    }.getOrDefault(false)

    private fun updateLocationState() {
        val enabled = isLocationEnabled()
        val previous = locationEnabled
        locationEnabled = enabled
        if (previous != null && previous != enabled) emit(if (enabled) SystemEventType.LOCATION_ENABLED else SystemEventType.LOCATION_DISABLED)
    }

    private fun updateDataSaverState() {
        val enabled = connectivityManager?.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
        val previous = dataSaverEnabled
        dataSaverEnabled = enabled
        if (previous != null && previous != enabled) emit(if (enabled) SystemEventType.DATA_SAVER_ENABLED else SystemEventType.DATA_SAVER_DISABLED)
    }

    private fun updateAutoRotateState() {
        val enabled = Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1) == 1
        val previous = autoRotateEnabled
        autoRotateEnabled = enabled
        if (previous != null && previous != enabled) emit(if (enabled) SystemEventType.AUTO_ROTATE_ENABLED else SystemEventType.AUTO_ROTATE_DISABLED)
    }

    private fun updateThermalState(status: Int) {
        val warning = status >= PowerManager.THERMAL_STATUS_MODERATE
        val previous = thermalWarningActive
        thermalWarningActive = warning
        if (previous != null && previous != warning) emit(if (warning) SystemEventType.THERMAL_WARNING else SystemEventType.THERMAL_NORMAL)
    }

    private fun updateFlashlightState(enabled: Boolean) {
        val previous = flashlightEnabled
        flashlightEnabled = enabled
        if (previous != null && previous != enabled) emit(if (enabled) SystemEventType.FLASHLIGHT_ENABLED else SystemEventType.FLASHLIGHT_DISABLED)
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

            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)

            addAction("android.net.wifi.WIFI_AP_STATE_CHANGED")
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WIFI_DETAILS_REFRESH_ACTION)

            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
            addAction(LocationManager.MODE_CHANGED_ACTION)
            addAction(ConnectivityManager.ACTION_RESTRICT_BACKGROUND_CHANGED)
        }

    private fun wifiRequest() =
        NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

    private fun vpnRequest() =
        NetworkRequest.Builder()
            .clearCapabilities()
            .addTransportType(TRANSPORT_VPN)
            .build()

    private val AudioDeviceInfo.isHeadphone: Boolean
        get() = type in HEADPHONE_TYPES

    private companion object {
        const val WIFI_DETAILS_REFRESH_ACTION =
            "com.vikram.expressiveisland.action.REFRESH_WIFI_DETAILS"

        val HEADPHONE_TYPES = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_USB_HEADSET,
        )
    }
}