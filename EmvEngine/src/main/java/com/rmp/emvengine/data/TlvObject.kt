package com.rmp.emvengine.data

import com.rmp.emvengine.common.hexToByteArray
import com.rmp.emvengine.common.toHexString
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.sign

class TlvObject(val tag: Long, val value: ByteArray) {

    constructor(tag: String, value: ByteArray) : this(tag.toLong(16), value)
    constructor(tag: Long, value: String) : this(tag, value.hexToByteArray())
    constructor(tag: String, value: String) : this(tag.toLong(16), value.hexToByteArray())

    val valueString : String
        get() {
            return value.toHexString()
        }


    override fun toString(): String {
        var len = value.size.toString(16)
        if (len.length == 1) {
            len = "0$len"
        }
        return tag.toString(16) + len + value.toHexString()
    }
}

fun ByteArray.toTlvObjects(): List<TlvObject>? {
    println("data:"+this.toHexString())
    val result = mutableListOf<TlvObject>()
    //detect tag
    var currentData = this

    while (currentData.size > 1) {

        val tag = currentData.detectTag() ?: return null

        var len: Int = 0
        len = currentData[tag.size].toInt()
        if (len < 0) len += 256

        val value = currentData.copyOfRange(tag.size + 1, tag.size + 1 + len)

        result.add(TlvObject(tag.toHexString(), value))
        val currentTagSize = tag.size + 1 + value.size

        if (currentTagSize < currentData.size) {
            currentData = currentData.copyOfRange(currentTagSize, currentData.size)
        } else {
            currentData = byteArrayOf()
        }
    }


    if (result.isEmpty())
        return null
    else
        return result
}

//detect tag, return tag as byte array
fun ByteArray.detectTag(): ByteArray? {
    if (this.isEmpty()) return null
    var tag: ByteArray = byteArrayOf()
    var first = true
    for (item in this) {
        if (first) {
            first = false
            tag = tag.plus(item)
            if (item == 0.toByte() || item.and(0x1F.toByte()) != 0x1F.toByte()) {
                break
            }
        } else {
            tag = tag.plus(item)
            if (!(!first && item != 0.toByte() && item.and(0x80.toByte()) == 0x80.toByte())) {
                break
            }
        }
    }
    return tag
}

fun ByteArray.toPDOL(): List<Pair<Long,Int>>? {

    val result = mutableListOf<Pair<Long,Int>>()
    //detect tag
    var currentData = this

    while (currentData.size > 1) {

        val tag = currentData.detectTag() ?: return null

        var len = currentData[tag.size].toInt()
        if (len < 0) len += 256

        result.add(Pair(tag.toHexString().toLong(16), len))
        val currentTagSize = tag.size + 1

        if (currentTagSize < currentData.size) {
            currentData = currentData.copyOfRange(currentTagSize, currentData.size)
        } else {
            currentData = byteArrayOf()
        }
    }


    if (result.isEmpty())
        return null
    else
        return result
}