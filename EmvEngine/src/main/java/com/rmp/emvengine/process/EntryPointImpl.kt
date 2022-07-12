package com.rmp.emvengine.process

import com.rmp.emvengine.CardReader
import com.rmp.emvengine.common.CommandHelper
import com.rmp.emvengine.VisaClessProcess
import com.rmp.emvengine.data.Aid
import com.rmp.emvengine.data.TlvObject
import com.rmp.emvengine.common.hexToByteArray
import com.rmp.emvengine.common.toHexString
import com.rmp.emvengine.data.KernelId
import com.rmp.emvengine.data.TransactionData

class EntryPointImpl(
    private val cardReader: CardReader,
    private val transactionData: TransactionData
) : EntryPoint {

    private var lastError: Int = 0
    private var kernelId: KernelId? = null

    private lateinit var clessEmvProcess: ClessEmvProcess

    override fun combinationSelection(list: List<Aid>): List<Aid>? {
        //build PPSE
        val data = "325041592E5359532E4444463031".hexToByteArray()
        val cmd = CommandHelper.buildSelectCmd(data = data)
        //send PPSE
        val response = cardReader.transmitData(cmd)

        if (response.error != null) {
            lastError = 1
            return null
        }
        //parse PPSE response


    }

    override fun finalCombinationSelection(aid: Aid) {

        //build select aid
        val data = aid.aid.hexToByteArray()
        val cmd = CommandHelper.buildSelectCmd(data = data)
        //send select aid
        val response = cardReader.transmitData(cmd)
        if (response.error != null) {
            lastError = 2
            return
        }
        //parse select aid response


    }

    override fun kernelActivation() {
        //decision kernel by entry point
        val appKernelId = transactionData.cardData[0x9F2A]?.value?.toHexString()?.toIntOrNull()
        if (appKernelId == null) {
            val aid = transactionData.cardData[0x4F]?.value?.toHexString()
            if (aid == null) {
                lastError = 2
                return
            }
            when {
                aid.startsWith("A000000003") -> kernelId = KernelId.VISA
                aid.startsWith("A000000004") -> kernelId = KernelId.MASTER
                aid.startsWith("A000000025") -> kernelId = KernelId.AMEX
                aid.startsWith("A000000152") -> kernelId = KernelId.DISCOVER
                aid.startsWith("A000000065") -> kernelId = KernelId.JCB
                aid.startsWith("A000000333") -> kernelId = KernelId.POBC
                else -> {
                    lastError = 3
                    return
                }
            }
        } else {
            when (appKernelId) {
                KernelId.VISA.value -> KernelId.VISA
                KernelId.MASTER.value -> KernelId.VISA
                KernelId.AMEX.value -> KernelId.VISA
                KernelId.JCB.value -> KernelId.VISA
                KernelId.DISCOVER.value -> KernelId.VISA
                KernelId.POBC.value -> KernelId.VISA
                else -> {
                    lastError = 3
                    return
                }
            }
        }

        clessEmvProcess = when (kernelId) {
            KernelId.VISA -> VisaClessProcess(cardReader)
            KernelId.MASTER -> TODO()
            else -> {
                lastError = 3
                return
            }
        }
    }


    override fun preprocessing(data: List<TlvObject>) {
        val result = clessEmvProcess.preprocessing(data)
        //update result to terminal tag
        result?.forEach {
            transactionData.terminalData[it.tag] = it
        }?: kotlin.run {
            lastError = 4
            return
        }
    }

    override fun initiateTransaction(data: List<TlvObject>) {
        clessEmvProcess.initiateTransaction(data)
    }

    override fun readRecord() {
        ///read record if have AFL
        val afl = transactionData.cardData[0x94]
        if( afl!= null && afl.value.size >=4){



        }
    }

    override fun offlineDataAuthentication() {
        TODO("Not yet implemented")
    }

    override fun processingRestriction() {
        TODO("Not yet implemented")
    }

    override fun cardholderVerification() {
        TODO("Not yet implemented")
    }

    override fun terminalRiskManagement() {
        TODO("Not yet implemented")
    }

    override fun terminalActionAnalysis() {
        TODO("Not yet implemented")
    }

    override fun cardActionlActionAnalysis() {
        TODO("Not yet implemented")
    }


    override fun getLastError(): Int {
        TODO("Not yet implemented")
    }
}