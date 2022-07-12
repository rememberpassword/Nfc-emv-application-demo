package com.rmp.emvengine.common


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

