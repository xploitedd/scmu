package xyz.xploited.websocket

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.pingInterval
import io.ktor.server.websocket.timeout
import io.ktor.websocket.Frame
import io.ktor.websocket.FrameType
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.UUID

typealias ConnectionProducer<T> = (UUID, WebSocketSession) -> T
typealias ConnectionFrameHandler<T> = suspend (T) -> Unit
typealias ConnectionDisconnectHandler<T> = suspend (T) -> Unit

class ConnectionHandler<T : Connection>(
    private val connectionProducer: ConnectionProducer<T>,
    private val handleFrames: ConnectionFrameHandler<T>,
    private val onDisconnect: ConnectionDisconnectHandler<T> = {}
) {
    companion object {
        private val log = LoggerFactory.getLogger(ConnectionHandler::class.java)
        private val PING_INTERVAL = Duration.ofSeconds(10)
        private val TIMEOUT = Duration.ofSeconds(8)
    }

    private val mutex = Mutex()
    private val connections = hashMapOf<UUID, T>()

    suspend fun handleSession(session: DefaultWebSocketServerSession) {
        session.pingInterval = PING_INTERVAL
        session.timeout = TIMEOUT

        val connection = createConnection(session)
        log.info("New connection with id: {}", connection.id)

        try {
            handleFrames(connection)
        } catch (ex: Throwable) {
            log.error("Connection {} was disconnected because of an error", connection.id)
        } finally {
            log.info("Removing connection {}", connection.id)
            removeConnection(connection.id)
            onDisconnect(connection)
        }
    }

    private fun generateId(): UUID {
        while (true) {
            val uuid = UUID.randomUUID()
            if (!connections.containsKey(uuid))
                return uuid
        }
    }

    private suspend fun createConnection(session: WebSocketSession): T {
        return mutex.withLock {
            val id = generateId()
            val connection = connectionProducer(id, session)
            connections[id] = connection

            connection
        }
    }

    private suspend fun removeConnection(id: UUID) {
        mutex.withLock {
            connections.remove(id)
        }
    }

    suspend fun getConnections(): Iterable<T> {
        return mutex.withLock {
            connections.values
        }
    }
}