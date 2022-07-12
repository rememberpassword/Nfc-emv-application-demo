package com.rmp.emvengine

interface CardReader {

    fun transmitData(data: ByteArray): TransmitResult

    fun isCardRemoved(): Boolean
}

data class TransmitResult (
    val error: CardReaderError?,
    val data: ByteArray?
)

enum class CardReaderError {
    CARD_REMOVED
}
