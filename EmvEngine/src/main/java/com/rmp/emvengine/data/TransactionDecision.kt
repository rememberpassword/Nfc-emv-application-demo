package com.rmp.emvengine.data

enum class TransactionDecision(val value: Int) {
    AAC(0x00),
    TC(0x40),
    ARQC(0x80),
}