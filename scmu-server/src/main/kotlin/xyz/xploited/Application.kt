package xyz.xploited

import io.ktor.server.application.install
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.websocket.WebSockets
import xyz.xploited.websocket.*

fun main() {
    embeddedServer(CIO, port = 8080) {
        install(WebSockets)

        configureWebsocket()
    }.start(wait = true)
}
