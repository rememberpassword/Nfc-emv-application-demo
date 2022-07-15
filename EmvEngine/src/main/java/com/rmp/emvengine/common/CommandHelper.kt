package com.rmp.emvengine.common

import com.rmp.emvengine.data.TlvObject
import kotlin.experimental.or

object CommandHelper {

    fun buildSelectCmd(
        isSelectByName: Boolean = true,
        firstOnlyOccurrence: Boolean = false,
        data: ByteArray
    ): ByteArray {

        val clazz = 0x00.toByte()
        val ins = 0xA4.toByte()
        val lc = data.size.toByte()
        val p1 = if (isSelectByName) 0x04 else 0x00
        val p2 = if (firstOnlyOccurrence) 0x02 else 0x00
        val le = 0x00.toByte()

        return byteArrayOf(clazz, ins, p1.toByte(), p2.toByte(), lc).plus(data).plus(le)
    }

    //return list read record cmd with use for ODA or not.
    fun buildReadRecordCmdByAFL(afl: ByteArray): List<Pair<ByteArray, Boolean>> {
        val result = mutableListOf<Pair<ByteArray, Boolean>>()
        val aflItem = mutableListOf<Byte>()
        afl.forEach {
            aflItem.add(it)
            if (afl.size >= 4) {
                //gen cmd read record
                val sfl = afl[0].or(0x04)
                val srec = afl[1]
                val erec = afl[2]
                val odaNumberRec = afl[3]
                var count = 0
                for (recordIndex in srec..erec) {
                    count += 1

                    val sflCmd = buildReadRecordCmdBySFL(sfl, recordIndex.toByte())
                    if (count <= odaNumberRec) {
                        result.add(Pair(sflCmd, true))
                    } else {
                        result.add(Pair(sflCmd, false))
                    }

                }
                //clear aflItem
                aflItem.clear()
            }
        }

        return result
    }

    private fun buildReadRecordCmdBySFL(p2: Byte, p1: Byte): ByteArray {
        val clazz = 0x00.toByte()
        val ins = 0xB2.toByte()
        val le = 0x00.toByte()
        return byteArrayOf(clazz, ins, p1, p2, le)

    }

    fun buildGPOCmd(data: ByteArray): ByteArray {
        val clazz = 0x80.toByte()
        val ins = 0xA8.toByte()
        val tag83 = TlvObject(0x83L,data).toString().hexToByteArray()
        val lc = tag83.size.toByte()
        val le = 0x00.toByte()

        return byteArrayOf(clazz, ins, 0, 0, lc).plus(tag83).plus(le)
    }

    fun buildGenerateAC(terminalDecision: Byte, isCdaRequest: Boolean, data: ByteArray): ByteArray {
        val clazz = 0x80.toByte()
        val ins = 0xAE.toByte()
        val lc = data.size.toByte()
        val le = 0x00.toByte()
        val p1 = terminalDecision.or(if (isCdaRequest) 0x10 else 0)

        return byteArrayOf(clazz, ins, p1, 0, lc).plus(data).plus(le)
    }

}