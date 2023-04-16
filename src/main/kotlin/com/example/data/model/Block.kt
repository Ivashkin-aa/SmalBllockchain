package com.example.data.model

data class Block(
    val index: Int,
    val hash: String,
    val prevHash: String,
    val data: String,
    val nonce: Int
)
