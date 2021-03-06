package xyz.xploited.websocket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.ktor.websocket.WebSocketSession
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

abstract class Connection(
    open val id: UUID,
    open val session: WebSocketSession
)

data class UbiquitousConnection(
    override val id: UUID,
    override val session: WebSocketSession,
    @Volatile
    var config: UbiquitousConnectionConfiguration? = null
) : Connection(id, session)

data class UbiquitousConnectionConfiguration(
    val publicKey: String,
    val mobileConnections: ConcurrentLinkedQueue<MobileConnection> = ConcurrentLinkedQueue()
)

@JsonClass(generateAdapter = false)
data class UbiquitousConfiguration(
    @Json(name = "public_key")
    val publicKey: String
)

data class MobileConnection(
    override val id: UUID,
    override val session: WebSocketSession,
    @Volatile
    var config: MobileConnectionConfiguration? = null
) : Connection(id, session)

data class MobileConnectionConfiguration(
    val publicKey: String,
    val ubiquitousConnection: UbiquitousConnection
)

@JsonClass(generateAdapter = false)
data class MobileConfiguration(
    @Json(name = "public_key")
    val publicKey: String
)

@JsonClass(generateAdapter = false)
data class WifiPacket(
    val ssid: String,
    val strength: Int
)

@JsonClass(generateAdapter = false)
data class ThresholdPacket(
    @Json(name = "rain_threshold")
    val rainThreshold: Int,
    @Json(name = "pm_25_threshold")
    val pm25Threshold: Int,
    @Json(name = "pm_10_threshold")
    val pm10Threshold: Int,
    val signature: String? = null
)

@JsonClass(generateAdapter = false)
data class InfoPacket(
    val thresholds: ThresholdPacket,
    val wifi: WifiPacket,
    @Json(name = "is_closed")
    val isClosed: Boolean,
    @Json(name = "is_raining")
    val isRaining: Boolean,
    @Json(name = "pm_25_level")
    val pm25Level: Int,
    @Json(name = "pm_10_level")
    val pm10Level: Int
)