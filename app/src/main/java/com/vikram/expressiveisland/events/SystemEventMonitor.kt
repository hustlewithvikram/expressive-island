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

    private val displayManager = context.getSystemService<DisplayManager>()

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
    private var audioOutputSnapshot: Set<String>? = null
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
                        emit(SystemEventType.HEADPHONES_CONNECTED)
                    }
                }
            updateAudioOutputDevices()
        }

        override fun onAudioDevicesRemoved(
            removedDevices: Array<out AudioDeviceInfo>,
        ) {
            removedDevices
                .filter { it.isHeadphone }
                .forEach { device -> headphoneDevices.remove(device.id) }
            if (headphoneDevices.isEmpty() && removedDevices.any { it.isHeadphone }) {
                HeadphonesBus.update(null)
                emit(SystemEventType.HEADPHONES_DISCONNECTED)
            }
            updateAudioOutputDevices()
        }
    }

    private val wifiNetworkCallback =
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

            val wifiInfo = capabilities.transportInfo as? WifiInfo ?: return
            val linkProperties = connectivity.getLinkProperties(network)
            val ipAddress = linkProperties
                ?.linkAddresses
                ?.firstOrNull { it.address.hostAddress?.contains(":") == false }
                ?.address
                ?.hostAddress

            val ssid = wifiInfo.ssid
                ?.removePrefix("\"")
                ?.removeSuffix("\"")
                ?.takeUnless { it.isBlank() || it == "<unknown ssid>" }

            WifiBus.update(
                WifiState(
                    ssid = ssid,
                    signalLevel = WifiManager.calculateSignalLevel(wifiInfo.rssi, 5),
                    rssi = wifiInfo.rssi.takeIf { it != -127 },
                    linkSpeedMbps = wifiInfo.linkSpeed.takeIf { it > 0 },
                    frequencyMhz = wifiInfo.frequency.takeIf { it > 0 },
                    ipAddress = ipAddress,
                    isMetered = !capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_NOT_METERED
                    ),
                )
            )
        } catch (e: SecurityException) {
            Log.w("SystemEventMonitor", "Unable to read Wi-Fi state: permission unavailable", e)
            WifiBus.update(null)
        } catch (e: Exception) {
            Log.w("SystemEventMonitor", "Unable to read Wi-Fi state", e)
            WifiBus.update(null)
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

        audioOutputSnapshot = currentAudioOutputSnapshot()
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
        displayManager?.unregisterDisplayListener(displayListener)
        audioOutputSnapshot = null
        lastRefreshRateHz = null
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
        headphoneDevices.clear()
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

    private fun currentAudioOutputSnapshot(): Set<String>? = runCatching {
        audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            ?.filter { it.isSink }
            ?.map { "${it.id}:${it.type}" }
            ?.toSet()
    }.getOrNull()

    private fun updateAudioOutputDevices() {
        val current = currentAudioOutputSnapshot() ?: return
        val previous = audioOutputSnapshot
        audioOutputSnapshot = current
        if (previous != null && previous != current) emit(SystemEventType.AUDIO_OUTPUT_CHANGED)
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

        val HEADPHONE_TYPES = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_USB_HEADSET,
        )
    }
}