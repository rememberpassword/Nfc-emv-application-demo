package com.rmp.emvengine.data

import com.rmp.emvengine.common.hexToByteArray
import com.rmp.emvengine.common.toHexString
import com.rmp.emvengine.common.toPosInit
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
        println("tag:${tag.toHexString()}")

        var len: Int = 0
        var lenLength = 1
        len = currentData[tag.size].toPosInit()

        if(len > 128){
            when(len){
                    0x81 ->{
                        //take 1 byte
                        len = 0
                        len = len shl 8
                        len = len or currentData[tag.size + 1].toPosInit()
                        lenLength = 2
                    }
                    0x82 ->{
                        //take 2 byte
                        len = len shl 8
                        len = len or currentData[tag.size + 1].toPosInit()
                        len = len shl 8
                        len = len or currentData[tag.size + 2].toPosInit()
                        lenLength = 3
                    }
            }
        }


        println("len: $len")

        val value = currentData.copyOfRange(tag.size + lenLength, tag.size + lenLength + len)
        println("value: ${value.toHexString()}")

        result.add(TlvObject(tag.toHexString(), value))
        val currentTagSize = tag.size + lenLength + value.size

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