package xyz.xploited.scmumobile.screen.bluetooth

import android.app.Application
import android.bluetooth.le.ScanSettings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ensarsarajcic.kotlinx.serialization.msgpack.MsgPack
import com.juul.kable.Advertisement
import com.juul.kable.ObsoleteKableApi
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.State
import com.juul.kable.characteristicOf
import com.juul.kable.descriptorOf
import com.juul.kable.peripheral
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import xyz.xploited.scmumobile.MobileApplication

@kotlinx.serialization.Serializable
data class WifiNetwork(
    val ssid: String,
    val security: Int,
    val strength: Int
)

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val application = getApplication<MobileApplication>()

    private val _bluetoothDevicesMap = mutableMapOf<String, Advertisement>()
    private var _scannerJob: Job? = null
    private var _peripheral: Peripheral? = null

    private val _bluetoothDevices = MutableLiveData<List<Advertisement>>()

    @OptIn(ObsoleteKableApi::class)
    fun startSearch(): LiveData<List<Advertisement>> {
        val scanner = Scanner {
            scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

//            filters = listOf(
//                Filter.Service(uuidFrom("ddbc279f-61eb-484a-bbc2-f65f2d4325be"))
//            )
        }

        if (_scannerJob != null)
            _scannerJob!!.cancel()

        _scannerJob = viewModelScope.launch {
            scanner.advertisements
                .cancellable()
                .collect {
                    _bluetoothDevicesMap[it.address] = it
                    _bluetoothDevices.postValue(_bluetoothDevicesMap.values.toList())
                }
        }

        return _bluetoothDevices
    }

    fun getBluetoothAdvertisement(macAddress: String): Advertisement? {
        return _bluetoothDevicesMap[macAddress]
    }

    fun connect(adv: Advertisement) {
        viewModelScope.launch {
            if (_peripheral != null)
                _peripheral!!.disconnect()

            try {
                _peripheral = peripheral(adv)
                _peripheral!!.connect()
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun getWifiNetworks(): LiveData<List<WifiNetwork>> {
        val liveData = MutableLiveData<List<WifiNetwork>>()
        viewModelScope.launch {
            _peripheral?.let {
                it.state.first { state -> state == State.Connected }
                Log.e("hello", "connected")

                it.observe(characteristicOf(
                    service = "ddbc279f-61eb-484a-bbc2-f65f2d4325be",
                    characteristic = "a6bb77a3-e0d5-4841-b424-55a7ddc9f1cb"
                )).map { bytes ->
                    Log.e("hello", "received data")
                    MsgPack.decodeFromByteArray<List<WifiNetwork>>(bytes)
                }.collect { networks ->
                    liveData.postValue(networks)
                }
            }
        }

        return liveData
    }

    fun disconnect() {
        viewModelScope.launch {
            _peripheral?.disconnect()
        }
    }

    fun stopSearch() {
        _scannerJob?.cancel()
    }

    override fun onCleared() {
        stopSearch()
    }

}