package com.example.data.model

import com.example.utils.getRandomString
import com.example.utils.sha256
import kotlinx.serialization.Serializable

@Serializable
data class Block(
    val index: Int,
    val hash: String,
    val prevHash: String,
    val data: String,
    val nonce: Int
) {
    companion object {
        fun createBlock(index: Int? = null, prevHash: String? = null): Block {
            val currentData = getRandomString()

            var currentHash = ""
            var currentNonce = 0
            var isCorrectHash = false
            val newIndex = index?.plus(1) ?: 0
            while (!isCorrectHash) {
                currentHash = sha256("${newIndex}$prevHash$currentData$currentNonce")
                if (currentHash.endsWith("000000")) isCorrectHash = true else currentNonce++
            }

            return Block(
                index = newIndex,
                hash = currentHash,
                prevHash = prevHash ?: "",
                data = currentData,
                nonce = currentNonce
            )
        }
    }
}