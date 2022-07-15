package com.rmp.emvengine

import android.util.Log
import com.rmp.emvengine.common.hexToByteArray
import com.rmp.emvengine.common.toBcd
import com.rmp.emvengine.common.toHexString
import com.rmp.emvengine.data.Aid
import com.rmp.emvengine.data.TlvObject
import com.rmp.emvengine.data.detectTag
import com.rmp.emvengine.data.toTlvObjects
import org.junit.Test

import org.junit.Assert.*
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.experimental.xor
import kotlin.math.pow
import kotlin.random.Random

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {

            val x = "774C8202200057134665840080965277D23122019999985599999F5F2002202F5F3401009F100706010A03A000009F26083D419953633D97F69F2701809F3602125B9F6C0278009F6E0420700000".hexToByteArray()
        val y = parseGPO(x)
        print( y)

    }

    val cardData = mutableMapOf<Long,TlvObject>()
    private fun parseGPO(data: ByteArray): Boolean {
        data.toTlvObjects()?.firstOrNull {
            it.tag == 0x77L
        }?.also {
            it.value.toTlvObjects()?.forEach {
//                cardData[it.tag] = it
                if (cardData[it.tag] == null) {
                    cardData[it.tag] = it
                } else {
//                    lastError = "4"
                    println(it.tag.toString(16))
                    return false
                }

            }
        } ?: return false
        return true
    }

    fun ByteArray.toPDOL(): List<Pair<Long,Int>>? {
        println("data:"+this.toHexString())
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





    @Test
    fun ttt(){
        val s= "BF0C1B61194F08A000000333010101500A55494353204445424954870101".hexToByteArray()
        val r = s.toTlvObjects()

        println(r)
        println(r?.last()?.valueString)
    }

    val cardAppData = mutableListOf<Aid>()



}
