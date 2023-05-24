package com.example.utils

import java.math.BigInteger
import java.security.MessageDigest

fun sha250(input: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val messageDigest = md.digest(input.toByteArray())
    val no = BigInteger(1, messageDigest)
    var hashtext = no.toString(16)
    while (hashtext.length < 32) {
        hashtext = "0$hashtext"
    }
    return hashtext
}