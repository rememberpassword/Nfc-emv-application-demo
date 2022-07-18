package com.rmp.emvnfcdemo.data

class Track2(
    val pan: String,
    val expDate: String,
    val serviceCode: String,
    val additionalData: String?
)

fun String.toTrack2(): Track2? {
    val track2Format = """(\d{1,19})(D|d)(\d{4})(\d{3})([^\?]*)"""
    val regex = Regex(track2Format)
    val matchValue = regex.matchEntire(this)?.groupValues

    return matchValue?.let {
        Track2(it[1], it[3], it[4], if (it.size == 4) it[5] else null)
    }

}