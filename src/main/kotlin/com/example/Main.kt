package com.example

import io.ktor.network.sockets.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
   val config = args.config()
    val node = Node(config.first.parseSocketAddress())
    val nodes = config.second.split(",").map { it.parseSocketAddress() }
    node.notifyAboutOtherNodes(nodes)

    runBlocking {
        node.start(config.third)
    }
}

private fun Array<String>.config() : Triple<String, String, Boolean> {
    val parser = ArgParser("Small Blockchain")
    val address by parser.option(
        ArgType.String,
        fullName = "address",
        shortName = "a",
        description = "node address"
    ).required()

    val otherNodes by parser.option(
        ArgType.String,
        fullName = "other",
        shortName = "o",
        description = "other nodes addresses"
    ).required()

    val first by parser.option(
        ArgType.Boolean,
        fullName = "Generated",
        shortName = "g",
        description = "generated block"
    ).default(false)

    parser.parse(this)

    return Triple(address, otherNodes, first)
}

private fun String.parseSocketAddress(): SocketAddress =
    split(':')
        .let { InetSocketAddress(it[0], it[1].toInt()) }