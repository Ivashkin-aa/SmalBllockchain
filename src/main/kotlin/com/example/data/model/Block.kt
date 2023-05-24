package com.example.data.model

import com.example.utils.ChangeNonce
import com.example.utils.generateData
import com.example.utils.increment
import com.example.utils.sha256
import kotlinx.serialization.Serializable

@Serializable
data class Block internal constructor(
    val index: Int,
    val previousHash: String,
    val nonce: Int,
    val data: String,
    val hash: String,
) {
    companion object {
        fun new(previous: Block?, changeNonce: ChangeNonce = increment): Block {
            val index = previous?.let { it.index + 1 } ?: 0
            val previousHash = previous?.hash ?: ""
            val data = generateData()
            var nonce = 0
            var hash: String

            do {
                nonce = changeNonce(nonce)

                hash = sha256(
                    buildString {
                        append(index)
                        append(previousHash)
                        append(data)
                        append(nonce)
                    }
                )
            } while (!hash.endsWith("0000"))

            return Block(index, previousHash, nonce, data, hash)
        }
    }
}