package xyz.xploited.scmumobile.screen.device

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import xyz.xploited.scmumobile.MobileApplication
import xyz.xploited.scmumobile.common.StateResult
import xyz.xploited.scmumobile.database.entities.Device

class DeviceViewModel(application: Application) : AndroidViewModel(application) {

    private val _application = getApplication<MobileApplication>()
    private val _repo = _application.deviceRepo

    @Volatile
    private var _currentWebsocket: DefaultClientWebSocketSession? = null

    val devicesList: LiveData<List<Device>> = _repo.getDevices().asLiveData(Dispatchers.IO)

    suspend fun getDeviceById(deviceId: Int): Device? {
        return _repo.getDeviceById(deviceId)
    }

    suspend fun deleteDevice(device: Device) {
        return _repo.deleteDevice(device)
    }

    fun getWebsocketData(device: Device): LiveData<StateResult<DeviceIncomingData>> {
        val liveData = MutableLiveData<StateResult<DeviceIncomingData>>()
        viewModelScope.launch {
            try {
                if (_currentWebsocket != null) {
                    if (_currentWebsocket!!.isActive) {
                        _currentWebsocket!!.close()
                        _currentWebsocket = null
                    }
                }

                _currentWebsocket = _repo.connectToWebsocket(device)
                _repo.getIncomingWebsocketData(_currentWebsocket!!).collect {
                    liveData.postValue(StateResult.success(it))
                }
            } catch (ex: Throwable) {
                liveData.postValue(StateResult.error(ex))
            }
        }

        return liveData
    }

    suspend fun updateThresholds(thresholds: DeviceThresholds) {
        if (_currentWebsocket != null) {
            _repo.sendDeviceThresholds(_currentWebsocket!!, thresholds)
        }
    }

    fun closeWebsocket() {
        viewModelScope.launch {
            if (_currentWebsocket != null && _currentWebsocket?.isActive == true)
                _currentWebsocket?.close()
        }
    }

}