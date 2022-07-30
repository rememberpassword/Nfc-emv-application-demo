package com.rmp.emvengine.data

data class RSAPublicKey (
    val modulus: ByteArray,
    val exponent: ByteArray
    )