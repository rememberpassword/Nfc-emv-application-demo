package com.rmp.emvengine.data

import com.rmp.emvengine.common.hexToByteArray
import com.rmp.emvengine.common.toHexString

class TlvObject(val tag: Long, val value: ByteArray) {

    constructor(tag: String, value: ByteArray) : this(tag.toLong(16),value)
    constructor(tag: Long, value: String) : this(tag,value.hexToByteArray())
    constructor(tag: String, value: String) : this(tag.toLong(16),value.hexToByteArray())

    override fun toString(): String {
        return tag.toString(16)+value.size.toString(16)+value.toHexString()
    }
}