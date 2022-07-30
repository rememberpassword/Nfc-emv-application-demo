package com.rmp.emvengine.common

import com.rmp.emvengine.data.TransactionDecision
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.math.pow


fun String.hexToByteArray(): ByteArray {
    val filtered = filterHex()
    return ByteArray(filtered.length / 2) { index ->
        filtered.substring(2 * index, 2 * index + 2).toInt(16).toByte()
    }
}


fun ByteArray.toHexString(
    lowerCase: Boolean = false
): String {

    return foldIndexed(StringBuilder()) { index, sb, byte ->
        val byteValue = byte.toInt() and 0xff
        val converted = (0x100 + byteValue).toString(16).substring(1)
        val hex = if (lowerCase) converted else converted.uppercase()
        sb.append(hex)
    }.toString()
}

private val REGEX_HEX_ONLY = Regex("""(?:[\da-fA-F]{2})*""")

/**
 * Check if a string is a valid hexadecimal representation.
 *
 * The string must contain only hexadecimal digits and be of even length.
 */
val String.isHexOnly: Boolean
    get() = matches(REGEX_HEX_ONLY)

val Char.isHex: Boolean
    get() = this in '0'..'9' || this in 'A'..'F' || this in 'a'..'f'

/**
 * Remove all characters that are not hexadecimal digits.
 */
fun String.filterHex() = filter { it.isHex }

/**
 * Convert long to bcd byte array.
 * @param len the byte size expect.
 */
fun Long.toBcd(len: Int): ByteArray {
    var s = this.toString()
    if (s.length % 2 != 0) {
        s = "0$s"
    }


    if (len * 2 > s.length) {
        s = s.padStart(len * 2, '0')
    }
    return s.hexToByteArray()
}

fun ByteArray.isAPDUSuccess(): Boolean {
    if (this.size < 2) return false
    val sw = this.copyOfRange(this.size - 2, this.size)
    return sw[0] == 0x90.toByte() && sw[1] == 0x00.toByte()
}


//byte index will start form 1
//bit index range value from 1 -> 8
fun ByteArray.turnOn(byteIndex: Int, bit: Int): ByteArray {
    val result = mutableListOf<Byte>()
    val b = 2.toDouble().pow(bit - 1).toInt().toByte()
    this.forEachIndexed { index, byte ->
        if (byteIndex - 1 == index) {
            result.add(byte.or(b))
        } else {
            result.add(byte)
        }
    }
    return result.toByteArray()
}

//byte index will start form 1
//bit index range value from 1 -> 8
fun ByteArray.turnOff(byteIndex: Int, bit: Int): ByteArray {
    val result = mutableListOf<Byte>()
    val b = 2.toDouble().pow(bit - 1).toInt().toByte().inv()
    this.forEachIndexed { index, byte ->
        if (byteIndex - 1 == index) {
            result.add(byte.and(b))
        } else {
            result.add(byte)
        }
    }
    return result.toByteArray()
}

//byte index will start form 1
//bit index range value from 1 -> 8
fun ByteArray.checkBitOn(byteIndex: Int, bit: Int): Boolean {
    val b = 2.toDouble().pow(bit - 1).toInt().toByte()
    return this[byteIndex - 1].and(b) == b

}

fun Byte.toPosInit(): Int {
    val i = this.toInt()
    return if (i < 0) i + 256 else i
}

fun ByteArray?.toTransactionDecision(): TransactionDecision{
    return when(this?.get(0)){
        TransactionDecision.TC.value.toByte() -> TransactionDecision.TC
        TransactionDecision.AAC.value.toByte() -> TransactionDecision.AAC
        TransactionDecision.ARQC.value.toByte() -> TransactionDecision.ARQC
        else -> TransactionDecision.AAC
    }
}
