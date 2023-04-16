package com.example.data.model

import com.example.utils.getRandomString
import com.example.utils.sha256

data class Block(
    val index: Int,
    val hash: String,
    val prevHash: String,
    val data: String,
    val nonce: Int
) {
    companion object {
        fun createBlock(index: Int, prevHash: String): Block {
            val currentData = getRandomString()

            var currentHash = ""
            var currentNonce = 0
            var isCorrectHash = false
            while (!isCorrectHash) {
                currentHash = sha256("$index$prevHash$currentData$currentNonce")
                if (currentHash.endsWith("000000")) isCorrectHash = true else currentNonce++
            }

            return Block(
                index = index,
                hash = currentHash,
                prevHash = prevHash,
                data = currentData,
                nonce = currentNonce
            )
        }
    }
}