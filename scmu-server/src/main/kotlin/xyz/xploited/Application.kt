package xyz.xploited

import io.ktor.server.engine.*
import io.ktor.server.cio.*
import xyz.xploited.plugins.*

fun main() {
    embeddedServer(CIO, port = 8080, host = "0.0.0.0") {
        configureRouting()
    }.start(wait = true)
}
