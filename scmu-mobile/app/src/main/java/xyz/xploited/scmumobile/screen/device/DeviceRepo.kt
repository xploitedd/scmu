package xyz.xploited.scmumobile.screen.device

import android.os.Parcelable
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import xyz.xploited.scmumobile.database.MobileDatabase
import xyz.xploited.scmumobile.database.entities.Device

private const val WEBSOCKET_URI = "ws://192.168.0.232:8080/mobile"

@kotlinx.serialization.Serializable
@Parcelize
data class DeviceWifiData(
    val ssid: String,
    val strength: Int
) : Parcelable

@kotlinx.serialization.Serializable
@Parcelize
data class DeviceThresholds(
    @SerialName("rain_threshold")
    val rainThreshold: Int,
    @SerialName("pm_25_threshold")
    val pm25Threshold: Int,
    @SerialName("pm_10_threshold")
    val pm10Threshold: Int,
    val signature: String? = null
) : Parcelable

fun DeviceThresholds.changed(
    newRain: Int? = null,
    newPM25: Int? = null,
    newPM10: Int? = null,
    newSignature: String? = null
) = DeviceThresholds(
    newRain ?: rainThreshold,
    newPM25 ?: pm25Threshold,
    newPM10 ?: pm10Threshold,
    newSignature ?: signature
)

@kotlinx.serialization.Serializable
@Parcelize
data class DeviceIncomingData(
    val thresholds: DeviceThresholds,
    val wifi: DeviceWifiData,
    @SerialName("is_closed")
    val isClosed: Boolean,
    @SerialName("is_raining")
    val isRaining: Boolean,
    @SerialName("pm_25_level")
    val pm25Level: Int,
    @SerialName("pm_10_level")
    val pm10Level: Int
) : Parcelable

@kotlinx.serialization.Serializable
@Parcelize
data class DeviceConfigurationData(
    @SerialName("public_key")
    val publicKey: String
) : Parcelable

class DeviceRepo(
    private val db: MobileDatabase,
    private val httpClient: HttpClient
) {

    fun getDevices(): Flow<List<Device>> {
        return db.deviceDao()
            .getAllDevices()
    }

    suspend fun getDeviceById(deviceId: Int): Device? {
        return db.deviceDao()
            .getDeviceById(deviceId)
    }

    suspend fun addDevice(device: Device): Long {
        return db.deviceDao()
            .insertDevice(device)
    }

    suspend fun deleteDevice(device: Device) {
        return db.deviceDao()
            .deleteDevice(device)
    }

    suspend fun connectToWebsocket(device: Device): DefaultClientWebSocketSession {
        val ws = httpClient.webSocketSession(WEBSOCKET_URI) {
            // WebSocket client closes connection due to request timeout
            // https://youtrack.jetbrains.com/issue/KTOR-4419
            timeout {
                requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
            }
        }

        ws.sendSerialized(DeviceConfigurationData(device.publicKey))
        return ws
    }

    fun getIncomingWebsocketData(ws: DefaultClientWebSocketSession): Flow<DeviceIncomingData> {
        return flow {
            while (true) {
                val incomingData = ws.receiveDeserialized<DeviceIncomingData>()
                emit(incomingData)
            }
        }
    }

    suspend fun sendDeviceThresholds(ws: DefaultClientWebSocketSession, thresholds: DeviceThresholds) {
        ws.sendSerialized(thresholds)
    }

}