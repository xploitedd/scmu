package xyz.xploited.scmumobile.screen.bluetooth

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.xploited.scmumobile.MobileApplication

class BluetoothScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val application = getApplication<MobileApplication>()

    private var _bluetoothDevicesMap = mutableMapOf<String, BluetoothDevice>()

    private val _searching = MutableLiveData(false)
    val searching: LiveData<Boolean> = _searching

    private val _bluetoothDevices = MutableLiveData<Set<BluetoothDevice>>()
    val bluetoothDevices: LiveData<Set<BluetoothDevice>> = _bluetoothDevices

    fun startSearch() {
        _searching.value = true
    }

    fun stopSearch() {
        _searching.value = false
    }

    fun addBluetoothDevice(device: BluetoothDevice) {
        _bluetoothDevicesMap[device.address] = device
        _bluetoothDevices.value = _bluetoothDevicesMap.values.toSet()
    }

    fun getBluetoothDeviceByAddress(address: String): BluetoothDevice? {
        return _bluetoothDevicesMap[address]
    }

    fun resetBluetoothSearch() {
        if (_searching.value == true) {
            stopSearch()

            viewModelScope.launch {
                delay(5000)
                startSearch()
            }
        }
    }

}