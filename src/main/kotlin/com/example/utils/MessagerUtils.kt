package com.example.utils

import com.example.data.model.Message
import io.ktor.utils.io.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets

suspend fun ByteWriteChannel.sendMessage(message: Message) {
    val json = Json.encodeToString(message)
    val byteArray = json.toByteArray(StandardCharsets.UTF_8)
    writeInt(byteArray.size)
    writeFully(byteArray)
}

suspend fun ByteReadChannel.receiveMessage(): Message {
    val byteArray = ByteArray(readInt())
    readFully(byteArray)
    return Json.decodeFromString(String(byteArray, StandardCharsets.UTF_8))
}