package com.rmp.emvengine.data

data class Capk (
    val index: Int,
    val rid: ByteArray,
    val modulus: ByteArray,
    val exponent: ByteArray,
    val sha: ByteArray?
)