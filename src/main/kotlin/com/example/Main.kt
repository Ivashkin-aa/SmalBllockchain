package com.example

import io.ktor.network.sockets.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

suspend fun main(args: Array<String>) {
    val config = config()
    val node = Node(config.first.parseSocketAddress())
    val nodes = config.second.split(",").map { it.parseSocketAddress() }
    node.notifyAboutOtherNodes(nodes)

    runBlocking {
        launch { node.start(config.third) }
    }
}

private fun config(): Triple<String, String, Boolean> {
    val port = System.getenv("port")
    val other = System.getenv("other")
    val generated = System.getenv("generated").toBoolean()

    return Triple(port, other, generated)
}

private fun String.parseSocketAddress(): SocketAddress =
    InetSocketAddress("127.0.0.1", this.toInt())