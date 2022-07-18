package com.rmp.emvengine.process.kernel

import android.util.Log
import com.rmp.emvengine.CardReader
import com.rmp.emvengine.common.*
import com.rmp.emvengine.data.CvmMethod
import com.rmp.emvengine.data.EmvCoreError
import com.rmp.emvengine.data.EmvTags
import com.rmp.emvengine.data.TlvObject
import com.rmp.emvengine.data.TransactionData
import com.rmp.emvengine.data.TransactionDecision
import com.rmp.emvengine.data.toPDOL
import com.rmp.emvengine.data.toTlvObjects
import com.rmp.emvengine.process.ClessEmvProcess
import kotlin.random.Random

internal class VisaClessProcess(
    private val cardReader: CardReader,
    private val transactionData: TransactionData
) : ClessEmvProcess {
    private var lastError: EmvCoreError? = null

    override fun preprocessing(data: List<TlvObject>) {
        //check mandatory tag
        val isEnough = checkMandatoryTags(data)
        if (!isEnough) {
            lastError = EmvCoreError.MISSING_TAG
            return
        }
        data.forEach {
            transactionData.terminalData[it.tag] = it
        }
        //check txn limit
        val amount = transactionData.terminalData[0x9F02]?.value?.toHexString()?.toLong()!!
        val txnLimit =
            transactionData.terminalData[EmvTags.EMV_CL_TRANS_LIMIT.value]?.value?.toHexString()
                ?.toLong()!!
        if (amount > txnLimit) {
            lastError = EmvCoreError.TXN_EXCEED_LIMIT
            return
        }

        //modify B2 b7,8 of TTQ
        modifyTTQ()

        //generate unpredictable number
        val random = generateUnpredictableNumber()
        transactionData.terminalData[0x9F37] = TlvObject(0x9F37, random)
        //generate TVR
        transactionData.terminalData[0x95] = TlvObject(0x95, byteArrayOf(0, 0, 0, 0, 0))

    }

    private fun generateUnpredictableNumber(): ByteArray {
        return Random.nextBytes(4)
    }

    private fun modifyTTQ(): Boolean {
        var ttq = transactionData.terminalData[0x9F66]?.value ?: return false
        val amount = transactionData.terminalData[0x9F02]?.value?.toHexString()?.toLongOrNull()
            ?: return false
        val floorLimit =
            transactionData.terminalData[EmvTags.EMV_CL_FLOOR_LIMIT.value]?.value?.toHexString()
                ?.toLongOrNull() ?: return false
        val cvmLimit =
            transactionData.terminalData[EmvTags.EMV_CL_CVM_LIMIT.value]?.value?.toHexString()
                ?.toLongOrNull() ?: return false

        if (amount > cvmLimit) {
            //turn on B2b7
            ttq = ttq.turnOn(2, 7)

        } else {
            //turn off B2b7
            ttq = ttq.turnOff(2, 7)
        }

        if (amount > floorLimit) {
            //turn on B2b8
            ttq = ttq.turnOn(2, 8)
        } else {
            //turn off B2b8
            ttq = ttq.turnOff(2, 8)
        }

        transactionData.terminalData[0x9F66] = TlvObject(0x9F66, ttq)

        return true
    }

    override fun initiateTransaction() {
        //parse Pdol
        val tagPDOL = transactionData.cardData[0x9F38L]
        Log.d("EntryPointImpl", "tagPDOL: $tagPDOL")
        val pdol = tagPDOL?.value?.toPDOL()
        if (tagPDOL == null || pdol == null) {
            lastError = EmvCoreError.NULL_PDOL
            return
        }

        //buid GPO data
        var pdolData = byteArrayOf()
        pdol.forEach {

            val v = transactionData.terminalData[it.first]?.value?.also {
                pdolData = pdolData.plus(it)
            } ?: kotlin.run {
                lastError = EmvCoreError.NULL_PDOL_DATA
                return
            }
            Log.d("EntryPointImpl", "tag:${it.first.toString(16)}| ${v.toHexString()}")
        }

        val gpoCmd = CommandHelper.buildGPOCmd(pdolData)
        val gpoResponse = cardReader.transmitData(gpoCmd)
        if (gpoResponse.error != null || gpoResponse.data == null || gpoResponse.data.size < 2) {
            lastError = EmvCoreError.NOT_RECEIVE_APDU
            return
        }
        if (!gpoResponse.data.isAPDUSuccess()) {
            //apdu error
            lastError = EmvCoreError.APDU_ERROR
            return
        }
        val status =
            parseGPO(gpoResponse.data.copyOfRange(0,gpoResponse.data.size - 2))
        if (!status) {
            lastError = EmvCoreError.APDU_RESPONSE_WRONG_FORMAT
            return
        }
        transactionData.transactionDecision =
            transactionData.cardData[0x9F27]?.value.toTransactionDecision()

    }

    private fun parseGPO(data: ByteArray): Boolean {
        println(data.toHexString())
        data.toTlvObjects()?.firstOrNull {
            it.tag == 0x77L
        }?.also {
            it.value.toTlvObjects()?.forEach {
                if (transactionData.cardData[it.tag] == null) {
                    transactionData.cardData[it.tag] = it
                } else {
                    lastError = EmvCoreError.DUPLICATE_CARD_DATA
                    return false
                }

            }
        } ?: return false
        return true
    }

    override fun readRecord() {
        val afl = transactionData.cardData[0x94L]?.value
        if (afl == null) {
            //skip read record
            return
        } else {
            val cmdReadRecord = CommandHelper.buildReadRecordCmdByAFL(afl)
            cmdReadRecord.forEach { (cmd, isForOda) ->
                val readRecordResponse = cardReader.transmitData(cmd)
                if (readRecordResponse.data?.isAPDUSuccess() == true) {
                    readRecordResponse.data.toTlvObjects()?.firstOrNull {
                        it.tag == 0x70L
                    }?.value?.toTlvObjects()?.forEach {
                        if (transactionData.cardData[it.tag] == null) {
                            transactionData.cardData[it.tag] = it
                        } else {
                            lastError = EmvCoreError.DUPLICATE_CARD_DATA
                            return
                        }
                        if (isForOda) {
                            transactionData.recordOda[it.tag] = it
                        }
                    }

                }
            }
        }

    }

    override fun offlineDataAuthenticationAndProcessingRestriction() {
        processingRestriction()
        if (lastError != null) {
            return
        }
        offlineDataAuthentication()

    }

    private fun offlineDataAuthentication() {
        //run fDDA if need
        if (transactionData.cardData[0x9F27]?.value.toTransactionDecision() == TransactionDecision.TC) {
            //offline txn need perform ODA
            transactionData.isFddaSuccess = performFDDA()
        }
    }

    private fun performFDDA(): Boolean {
        val isCardSupportFDDA = transactionData.cardData[0x82]!!.value.checkBitOn(1, 6)
        if (!isCardSupportFDDA) {
            return false
        }
        //TODO("will impl late")

        return true
    }

    private fun processingRestriction() {
        //Application Expired Check
        if (transactionData.transactionDecision == TransactionDecision.TC) {
            val expireDate = transactionData.cardData[0x5F24]?.valueString
            val txnDate = transactionData.terminalData[0x9A]!!.valueString
            if (expireDate == null || expireDate < txnDate) {
                //card expired
                //process by CTQ
                if (transactionData.cardData[0x9F6C]?.value?.checkBitOn(1, 4) == true) {
                    transactionData.transactionDecision = TransactionDecision.ARQC
                } else {
                    transactionData.transactionDecision = TransactionDecision.AAC
                }
            }
        }
        //skip Terminal Exception File Check
        //Application Usage Control - Manual Cash Transactions check
        val auc = transactionData.cardData[0x9F07]?.value
        if (auc != null && transactionData.terminalData[0x9C]!!.valueString == "01") {
            val issuerCountryCode = transactionData.cardData[0x5F28]?.valueString
            val terminalCountryCode = transactionData.terminalData[0x9F1A]?.valueString
            if ((issuerCountryCode == terminalCountryCode && auc.checkBitOn(1, 8)) ||
                (issuerCountryCode != terminalCountryCode && auc.checkBitOn(1, 7))
            ) {
                //txn allow
            } else {
                if (transactionData.cardData[0x9F6C]?.value?.checkBitOn(1, 3) == true) {
                    Log.w("EMV", "try other interface")
                    lastError = EmvCoreError.TRY_OTHER_INTERFACE

                } else {
                    //decline
                    transactionData.transactionDecision  = TransactionDecision.AAC
                }
                return
            }

        }
        //Application Usage Control - Cashback Transactions
        if (auc != null && transactionData.terminalData[0x9C]!!.valueString == "20") {
            val issuerCountryCode = transactionData.cardData[0x5F28]?.valueString
            val terminalCountryCode = transactionData.terminalData[0x9F1A]?.valueString
            if ((issuerCountryCode == terminalCountryCode && auc.checkBitOn(2, 8)) ||
                (issuerCountryCode != terminalCountryCode && auc.checkBitOn(2, 7))
            ) {
                //txn allow
            } else {
                if (transactionData.cardData[0x9F6C]?.value?.checkBitOn(1, 2) == true) {
                    Log.w("EMV", "try other interface")
                    lastError = EmvCoreError.TRY_OTHER_INTERFACE
                } else {
                    //decline
                    transactionData.transactionDecision  = TransactionDecision.AAC
                }
                return
            }

        }
        //skip ATM Offline Check


    }

    override fun cardholderVerification() {

        val ctq = transactionData.cardData[0x9F6C]?.value
        val ttq = transactionData.terminalData[0x9F66]!!.value
        if (ctq == null) {
            //CTQ Not Returned by Card
            when {
                ttq.checkBitOn(1, 2) -> transactionData.cvm = CvmMethod.SIGNATURE
                ttq.checkBitOn(1, 3) -> transactionData.cvm = CvmMethod.ONLINE_PIN
                else -> {
                    transactionData.cvm = CvmMethod.NO_CVM
                    transactionData.transactionDecision = TransactionDecision.AAC
                }
            }
        } else {
            //check online pin
            if (ctq.checkBitOn(1, 8) && ttq.checkBitOn(1, 3)) {
                transactionData.cvm = CvmMethod.ONLINE_PIN
                transactionData.transactionDecision = TransactionDecision.ARQC
            } else if (ctq.checkBitOn(2, 8)) {
                //check cdcvm
                val cardAuth = transactionData.cardData[0x9F69]?.value
                if (cardAuth != null) {
                    //Card Authentication Related Data bytes 6-7 match CTQ bytes 1-2
                    if (cardAuth[5] == ctq[0] && cardAuth[6] == ctq[1]) {
                        // cdcvm success
                        transactionData.cvm = CvmMethod.NO_CVM
                    } else {
                        transactionData.cvm = CvmMethod.NO_CVM
                        transactionData.transactionDecision = TransactionDecision.AAC
                    }
                    return
                } else {
                    if (transactionData.transactionDecision == TransactionDecision.ARQC) {
                        // cdcvm success
                        transactionData.cvm = CvmMethod.NO_CVM
                    } else {
                        transactionData.cvm = CvmMethod.NO_CVM
                        transactionData.transactionDecision = TransactionDecision.AAC
                    }
                }
            } else if (ctq.checkBitOn(1, 7) && ttq.checkBitOn(1, 2)) {
                transactionData.cvm = CvmMethod.SIGNATURE
            } else {
                transactionData.cvm = CvmMethod.NO_CVM
            }
        }
    }

    override fun terminalRiskManagement() {
        //Skip for VISA kernel
    }

    override fun terminalActionAnalysis() {
        //Skip for VISA kernel
    }

    override fun cardActionlActionAnalysis() {
        //Skip for VISA kernel
    }

    override fun getLastError(): String? {
        return lastError?.code
    }

    private fun checkMandatoryTags(data: List<TlvObject>): Boolean {
        val requireTags = listOf<Long>(
            //base tag
            0x9F02L,//amount
            0x9F03L,//cashback amount
            0x5F2AL,//currency code
            0x5F36L,//currency exponent
            0x9CL,//txn type
            0x9f21L,//txn time
            0x9AL,//txn date
            0x9F41L,// txn sequen counter
            0x9F1AL,//terminal country code
            //visa tag
            0x9F66L,
            EmvTags.EMV_CL_FLOOR_LIMIT.value,
            EmvTags.EMV_CL_CVM_LIMIT.value,
            EmvTags.EMV_CL_TRANS_LIMIT.value,
        )
        requireTags.forEach { rq ->
            if (data.firstOrNull { rq == it.tag } == null) {
                return false
            }
        }
        return true
    }

}