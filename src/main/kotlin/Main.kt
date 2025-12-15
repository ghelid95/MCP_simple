package dev.ru.ghelid

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.awaitCancellation

fun main(): Unit = runBlocking {
    val server = WeatherMcpServer()

    Runtime.getRuntime().addShutdownHook(Thread {
        server.close()
    })

    server.start()

    // Keep the server running until cancelled
    awaitCancellation()
}