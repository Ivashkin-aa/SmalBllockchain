package com.example.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val type: MessageType,
    val senderAddress: String,
    val block: Block? = null
)

enum class MessageType {
    RESPONSE_BLOCK,
    REQUEST_BLOCK,
    UPD_BLOCK
}
