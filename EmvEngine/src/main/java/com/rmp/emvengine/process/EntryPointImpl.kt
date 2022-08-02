package com.rmp.emvengine.process

import com.rmp.emvengine.CardReader
import com.rmp.emvengine.common.CommandHelper
import com.rmp.emvengine.process.kernel.VisaClessProcess
import com.rmp.emvengine.common.hexToByteArray
import com.rmp.emvengine.common.isAPDUSuccess
import com.rmp.emvengine.common.toHexString
import com.rmp.emvengine.data.*

internal class EntryPointImpl(
    private val cardReader: CardReader,
    private val transactionData: TransactionData
) : EntryPoint {

    private val TAG = "EntryPointImpl"
    private var lastError: String? = null

    private lateinit var clessEmvProcess: ClessEmvProcess

    override fun combinationSelection(aidsSupported: List<Aid>): List<Aid>? {
        //build PPSE
        //Data:  2PAY.SYS.DDF01
        val data = "325041592E5359532E4444463031".hexToByteArray()
        val cmd = CommandHelper.buildSelectCmd(data = data)
        //send PPSE
        val response = cardReader.transmitData(cmd)

        if (response.error != null || response.data == null || response.data.size < 2) {
            lastError =
                EmvErrorLevel.APP_SELECTION.code + EmvCoreError.NOT_RECEIVE_APDU.code
            return null
        }
        if (!response.data.isAPDUSuccess()) {
            //apdu error
            lastError =
                EmvErrorLevel.APP_SELECTION.code + EmvCoreError.APDU_ERROR.code
            return null
        }

        val status = parsePPSE(response.data.copyOf(response.data.size - 2))
        if (status) {
            return buildCandidateList(aidsSupported, transactionData.cardAppData)
        } else {
            return null
        }

    }

    private fun buildCandidateList(terminalAids: List<Aid>, cardAids: MutableList<Aid>): List<Aid> {
        //only base on matching aid (always support all kernel)
        val candidateList = mutableListOf<Aid>()
        cardAids.forEach { cardAid ->

            val match = terminalAids.firstOrNull { terminalAid ->
                terminalAid.aid == cardAid.aid ||
                        (cardAid.aid.startsWith(terminalAid.aid) && terminalAid.isPartial == true)
            }
            if (match != null) {
                candidateList.add(cardAid)
            }
        }
        return candidateList
    }

    private fun parsePPSE(data: ByteArray): Boolean {
        //lv1
        val tag6F = data.toTlvObjects()?.firstOrNull {
            it.tag == 0x6F.toLong()
        } ?: return false
        //lv2
        tag6F.value.toTlvObjects()?.firstOrNull {
            it.tag == 0x84.toLong()
        } ?: return false


        tag6F.value.toTlvObjects()?.also { tag6FValue ->
            //check A5 template
            val tagA5 = tag6FValue.firstOrNull {
                it.tag == 0xA5.toLong()
            } ?: return false
            tagA5.value.toTlvObjects()?.also {

                val appInfo = mutableListOf<TlvObject>()
                var aid = ""
                var priority = 0
                var label = ""
                val tagBF0C = it.firstOrNull {
                    it.tag == 0xBF0C.toLong()
                }
                tagBF0C?.value?.toTlvObjects()?.forEach { tag61 ->
                    val appData = tag61.value.toTlvObjects()
                    appData?.forEach {
                        if (it.tag == 0x4F.toLong()) {
                            aid = it.valueString
                        }
                        if (it.tag == 0x87.toLong()) {
                            priority = it.valueString.toInt(16)
                        }
                        if (it.tag == 0x50.toLong()) {
                            label = it.valueString
                        }
                        appInfo.add(it)
                    }

                }
                transactionData.cardAppData.add(
                    Aid(
                        aid = aid,
                        label = label,
                        priority = priority,
                        isPartial = null,
                        data = appInfo
                    )
                )
            }


        }
        return true
    }

    override fun finalCombinationSelection(aid: Aid):  List<Aid>? {

        transactionData.terminalData[0x4F] = TlvObject(0x4F, aid.aid)

        //build select aid
        val data = aid.aid.hexToByteArray()
        val cmd = CommandHelper.buildSelectCmd(data = data)
        //send select aid
        val response = cardReader.transmitData(cmd)
        if (response.error != null || response.data == null) {
            lastError = EmvErrorLevel.FINAL_SELECT.code + EmvCoreError.NOT_RECEIVE_APDU.code
            return buildRemainsCandidateList()
        }
        if (!response.data.isAPDUSuccess()) {
            //apdu error
            lastError = EmvErrorLevel.FINAL_SELECT.code + EmvCoreError.APDU_ERROR.code
            return buildRemainsCandidateList()
        }
        //parse select aid response
        val status = parsePPSEFinalSelect(response.data)
        if (!status) {
            lastError =
                EmvErrorLevel.FINAL_SELECT.code + EmvCoreError.APDU_RESPONSE_WRONG_FORMAT.code
            return buildRemainsCandidateList()
        }
        return null

    }

    private fun buildRemainsCandidateList(): List<Aid>? {

        if (transactionData.cardData.size > 1) {
            transactionData.cardAppData.firstOrNull {
                it.aid == transactionData.terminalData[0x4F]?.valueString || it.aid.startsWith(
                    transactionData.terminalData[0x4F]?.valueString!!
                )
            }?.let {
                transactionData.cardAppData.remove(it)
            }
            return transactionData.cardAppData
        } else {
            return null
        }
    }

    private fun parsePPSEFinalSelect(data: ByteArray): Boolean {
        //lv1
        val tag6F = data.toTlvObjects()?.firstOrNull {
            it.tag == 0x6F.toLong()
        } ?: return false
        //lv2
        tag6F.value.toTlvObjects()?.firstOrNull {
            it.tag == 0x84.toLong()
        } ?: return false


        tag6F.value.toTlvObjects()?.also { tag6FValue ->
            //check A5 template
            val tagA5 = tag6FValue.firstOrNull {
                it.tag == 0xA5.toLong()
            } ?: return false
            tagA5.value.toTlvObjects()?.forEach {
                if (it.tag == 0xBF0C.toLong()) {
                    it.value.toTlvObjects()?.forEach {
                        transactionData.cardData[it.tag] = it
                    }
                } else {
                    transactionData.cardData[it.tag] = it
                }
            }
        }
        return true
    }


    override fun kernelActivation() {
        //decision kernel by entry point
        val appKernelId = transactionData.cardData[0x9F2A]?.value?.toHexString()?.toIntOrNull(16)
        if (appKernelId == null) {
            val aid = transactionData.terminalData[0x4F]?.value?.toHexString()
            if (aid == null) {
                lastError = EmvErrorLevel.KERNEL_ACTIVATION.code + EmvCoreError.MISSING_TAG.code
                return
            }
            when {
                aid.startsWith("A000000003") -> transactionData.kernelId = KernelId.VISA
                aid.startsWith("A000000004") -> transactionData.kernelId = KernelId.MASTER
                aid.startsWith("A000000025") -> transactionData.kernelId = KernelId.AMEX
                aid.startsWith("A000000152") -> transactionData.kernelId = KernelId.DISCOVER
                aid.startsWith("A000000065") -> transactionData.kernelId = KernelId.JCB
                aid.startsWith("A000000333") -> transactionData.kernelId = KernelId.POBC
                else -> {
                    lastError =
                        EmvErrorLevel.KERNEL_ACTIVATION.code + EmvCoreError.KERNEL_ABSENT.code
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
                    lastError =
                        EmvErrorLevel.KERNEL_ACTIVATION.code + EmvCoreError.KERNEL_ABSENT.code
                    return
                }
            }
        }

        clessEmvProcess = when (transactionData.kernelId) {
            KernelId.VISA -> VisaClessProcess(cardReader, transactionData)
            KernelId.MASTER -> TODO()
            else -> {
                lastError = EmvErrorLevel.KERNEL_ACTIVATION.code + EmvCoreError.KERNEL_ABSENT.code
                return
            }
        }
    }


    override fun preprocessing(data: List<TlvObject>) {
        clessEmvProcess.preprocessing(data)
        if (clessEmvProcess.getLastError() != null) {
            lastError = EmvErrorLevel.PREPROCESSING.code + clessEmvProcess.getLastError()
        }
    }

    override fun initiateTransaction() {
        //send GPO cmd
        clessEmvProcess.initiateTransaction()
        if (clessEmvProcess.getLastError() != null) {
            lastError = EmvErrorLevel.INITIATE_TXN.code + clessEmvProcess.getLastError()
        }
    }

    override fun readRecord() {
        ///read record if have AFL
        clessEmvProcess.readRecord()
        if (clessEmvProcess.getLastError() != null) {
            lastError = EmvErrorLevel.READ_RECORD.code + clessEmvProcess.getLastError()
        }
    }

    override fun offlineDataAuthenticationAndProcessingRestriction() {
        clessEmvProcess.offlineDataAuthenticationAndProcessingRestriction()
        if (clessEmvProcess.getLastError() != null) {
            lastError = EmvErrorLevel.ODA_PROCESS.code + clessEmvProcess.getLastError()
        }

    }

    override fun cardholderVerification(data: List<TlvObject>) {
        clessEmvProcess.cardholderVerification()
        if (clessEmvProcess.getLastError() != null) {
            lastError = EmvErrorLevel.CARDHOLDER_VERIFY.code + clessEmvProcess.getLastError()
        }
    }

    override fun terminalRiskManagement() {
        clessEmvProcess.terminalRiskManagement()
        if (clessEmvProcess.getLastError() != null) {
            lastError = EmvErrorLevel.TERMINAL_RISK.code + clessEmvProcess.getLastError()
        }
    }

    override fun terminalActionAnalysis() {
        clessEmvProcess.terminalActionAnalysis()
        if (clessEmvProcess.getLastError() != null) {
            lastError = EmvErrorLevel.TERMINAL_ACTION_ANALYSIS.code + clessEmvProcess.getLastError()
        }
    }

    override fun cardActionlActionAnalysis() {
        clessEmvProcess.cardActionlActionAnalysis()
        if (clessEmvProcess.getLastError() != null) {
            lastError = EmvErrorLevel.FIRST_GAC.code + clessEmvProcess.getLastError()
        }
    }


    override fun getLastError(): String? {
        return lastError
    }

}