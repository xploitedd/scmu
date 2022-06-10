@file:OptIn(ExperimentalStdlibApi::class)

package xyz.xploited.websocket

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.withLock

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

fun Application.configureWebsocket() {
    routing {
        val ubiquitousHandler = ConnectionHandler(
            connectionProducer = { id, session -> UbiquitousConnection(id, session) },
            handleFrames = ::handleUbiquitousFrame,
            onDisconnect = {
                // TODO: send notification to all
            }
        )

        webSocket("/ubiquitous") {
            ubiquitousHandler.handleSession(this)
        }

        val mobileHandler = ConnectionHandler(
            connectionProducer = { id, session -> MobileConnection(id, session) },
            handleFrames = { handleMobileFrame(it, ubiquitousHandler) },
            onDisconnect = { it.config?.ubiquitousConnection?.config?.mobileConnections?.remove(it) }
        )

        webSocket("/mobile") {
            mobileHandler.handleSession(this)
        }
    }
}

private suspend fun handleUbiquitousFrame(conn: UbiquitousConnection) {
    for (frame in conn.session.incoming) {
        frame as? Frame.Text ?: continue
        val text = frame.readBytes()
            .decodeToString()

        val connConfig = conn.config
        if (connConfig == null) {
            val config = moshi.adapter<UbiquitousConfiguration>()
                .failOnUnknown()
                .fromJson(text)!!

            conn.config = UbiquitousConnectionConfiguration(config.publicKey)
        } else {
            val data = moshi.adapter<InfoPacket>()
                .fromJson(text)

            if (data != null) {
                for (mobile in connConfig.mobileConnections) {
                    if (mobile.session.isActive)
                        mobile.session.send(text)
                }
            }
        }
    }
}

private suspend fun handleMobileFrame(conn: MobileConnection, ubiquitousHandler: ConnectionHandler<UbiquitousConnection>) {
    for (frame in conn.session.incoming) {
        frame as? Frame.Text ?: continue
        val text = frame.readBytes()
            .decodeToString()

        val connConfig = conn.config
        if (connConfig == null) {
            val config = moshi.adapter<MobileConfiguration>()
                .failOnUnknown()
                .fromJson(text)!!

            for (ubConn in ubiquitousHandler.getConnections()) {
                val ubConfig = ubConn.config ?: continue

                if (ubConfig.publicKey == config.publicKey) {
                    conn.config = MobileConnectionConfiguration(config.publicKey, ubConn)
                    ubConfig.mobileConnections.add(conn)
                    break
                }
            }
        } else {
            val data = moshi.adapter<ThresholdPacket>()
                .fromJson(text)

            if (data != null) {
                connConfig.ubiquitousConnection
                    .session
                    .send(text)
            }
        }
    }
}
