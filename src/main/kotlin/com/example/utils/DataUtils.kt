package com.example.utils

fun getRandomString() : String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..256)
        .map { allowedChars.random() }
        .joinToString("")
}