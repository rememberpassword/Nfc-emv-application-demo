package com.rmp.emvengine.process.kernel

import android.util.Log
import com.rmp.emvengine.CardReader
import com.rmp.emvengine.common.*
import com.rmp.emvengine.data.CvmMethod
import com.rmp.emvengine.data.EmvCoreError
import com.rmp.emvengine.data.EmvTags
import com.rmp.emvengine.data.OdaType
import com.rmp.emvengine.data.TlvObject
import com.rmp.emvengine.data.TransactionData
import com.rmp.emvengine.data.TransactionDecision
import com.rmp.emvengine.data.toPDOL
import com.rmp.emvengine.data.toTlvObjects
import com.rmp.emvengine.process.ClessEmvProcess
import com.rmp.emvengine.process.oda.OdaProcessHelper
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

        if (gpoResponse.data.isAPDUConditionsOfUseNotSatisfied()) {
            //Conditions of use not satisfied.
            //select next
            lastError = EmvCoreError.SELECT_NEXT
            return
        }
        if (!gpoResponse.data.isAPDUSuccess()) {
            //apdu error
            lastError = EmvCoreError.APDU_ERROR
            return
        }
        val status =
            parseGPO(gpoResponse.data.copyOfRange(0, gpoResponse.data.size - 2))
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
                    readRecordResponse.data.copyOfRange(0, readRecordResponse.data.size - 2)
                        .toTlvObjects()?.firstOrNull {
                            it.tag == 0x70L
                        }?.value?.apply {
                            toTlvObjects()?.forEach {
                                if (transactionData.cardData[it.tag] == null) {
                                    transactionData.cardData[it.tag] = it
                                } else {
                                    lastError = EmvCoreError.DUPLICATE_CARD_DATA
                                    return
                                }
                            }
                            if (isForOda) {
                                transactionData.recordOda = transactionData.recordOda + this
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

        if (transactionData.transactionDecision == TransactionDecision.TC &&
            (transactionData.terminalData[0x9F66]?.value?.checkBitOn(1, 1) == true &&
                    transactionData.cardData[0x94L] != null &&
                    transactionData.transactionDecision == TransactionDecision.ARQC)
        ) {
            //offline txn need perform ODA
            transactionData.isOdaSuccess = performODA()
            if (!transactionData.isOdaSuccess) {
                if (transactionData.transactionDecision == TransactionDecision.TC) {
                    val ctq = transactionData.getData(0x9F6C)
                    if (ctq?.checkBitOn(1, 6) == true) {
                        //‘Go online if ODA fails’
                        LogUtils.d("Go online when ODA fails")
                        transactionData.transactionDecision = TransactionDecision.ARQC
                    } else if (ctq?.checkBitOn(1, 5) == true) {
                        //‘Switch interface if ODA fails
                        LogUtils.e("Switch interface when ODA fails")
                        lastError = EmvCoreError.TRY_OTHER_INTERFACE
                        return
                    } else {
                        transactionData.transactionDecision = TransactionDecision.AAC
                    }
                } else {
                    transactionData.transactionDecision = TransactionDecision.AAC
                }

            }
        }
    }

    private fun performODA(): Boolean {
        transactionData.odaType = if (transactionData.cardData[0x9F4B] != null) {
            OdaType.FDDA
        } else if (transactionData.cardData[0x93] != null && transactionData.transactionDecision == TransactionDecision.ARQC) {
            OdaType.SDA
        } else {
            null
        }

        if (transactionData.odaType == null) {
            LogUtils.e("Transaction not support ODA")
            return false
        }

        val isCardSupportFDDA = transactionData.cardData[0x82]!!.value.checkBitOn(1, 6)
        if (!isCardSupportFDDA && transactionData.odaType == OdaType.FDDA) {
            LogUtils.e("Card not support fDDA")
            return false
        }
        val isCardSupportSDA = transactionData.cardData[0x82]!!.value.checkBitOn(1, 7)
        if (!isCardSupportSDA && transactionData.odaType == OdaType.SDA) {
            LogUtils.e("Card not support SDA")
            return false
        }

        if (!checkCapk()) {
            LogUtils.e("Capk not correct")
            return false
        }
        //run fdda


        LogUtils.d("Start retrieval Issuer Public Key")
        val issuerPKCert = transactionData.getData(0x90) ?: kotlin.run {
            LogUtils.e(" Missing Issuer Public Key Certificate")
            return false
        }
        val issuerExp = transactionData.getData(0x9F32) ?: kotlin.run {
            LogUtils.e(" Missing Issuer Public Key Exponent")
            return false
        }
        val issuerPKRemainder = transactionData.getData(0x92)
        val issuerPK = OdaProcessHelper.decipherIssuerPublicKey(
            capk = transactionData.capk!!,
            issuerCertificate = issuerPKCert,
            issuerExponent = issuerExp,
            issuerPublicKeyRemainder = issuerPKRemainder
        )
        if (issuerPK == null) {
            LogUtils.e("Retrieval Issuer Public Key FAIL")
            return false
        }
        LogUtils.d("Retrieval Issuer Public Key SUCCESS")
        LogUtils.d("Start retrieval ICC Public Key")

        val iccPKCert = transactionData.getData(0x9F46) ?: kotlin.run {
            LogUtils.e(" Missing ICC Public Key Certificate")
            return false
        }
        val iccExp = transactionData.getData(0x9F47) ?: kotlin.run {
            LogUtils.e(" Missing ICC Public Key Exponent")
            return false
        }
        val iccRemainder = transactionData.getData(0x9F48)

        val offlineAuthenticationRecords = transactionData.recordOda

        var sdaTagListData: ByteArray? = byteArrayOf()
        if (transactionData.getData(0x9F4A) == null) {
            sdaTagListData = null
        } else if (transactionData.getData(0x9F4A)?.size == 1 && transactionData.getData(0x9F4A)
                ?.first() == 0x82.toByte()
        ) {
            sdaTagListData = transactionData.getData(0x82)!!
        } else {
            LogUtils.e(" SDA Tag list not ONLY contain AIP.")
            return false
        }

        val iccPk = OdaProcessHelper.decipherICCPublicKey(
            issuerPublicKey = issuerPK,
            iccPKCertificate = iccPKCert,
            iccExponent = iccExp,
            iccPublicKeyRemainder = iccRemainder,
            offlineAuthenticationRecords = offlineAuthenticationRecords,
            aip = sdaTagListData
        )
        if (iccPk == null) {
            LogUtils.e("Retrieval ICC Public Key FAIL")
            return false
        }
        LogUtils.d("Retrieval ICC Public Key SUCCESS")

        LogUtils.d("Start verify FDDA data")
        if (transactionData.odaType == OdaType.FDDA) {
            val sdad = transactionData.getData(0x9F4B) ?: kotlin.run {
                LogUtils.e(" Missing Signed Dynamic Application Data")
                return false
            }

            //9F37,9F02,5F2A,9F69
            val tag9F37 = transactionData.getData(0x9F37)!!
            val tag9F02 = transactionData.getData(0x9F02)!!
            val tag5F2A = transactionData.getData(0x5F2A)!!
            val tag9F69 = transactionData.getData(0x9F69) ?: kotlin.run {
                LogUtils.e(" Missing Card Authentication Related Data")
                return false
            }
            val terminalDynamicData = tag9F37 + tag9F02 + tag5F2A + tag9F69
            LogUtils.d("terminalDynamicData: ${terminalDynamicData.toHexString()}")//C901E2E0000000001800097801253996170000
            val result = OdaProcessHelper.verifyFDDA(
                iccPublicKey = iccPk,
                sdad = sdad,
                terminalDynamicData = terminalDynamicData,
                isOfflineTxn = transactionData.transactionDecision == TransactionDecision.TC
            )

            if (result) {
                LogUtils.d("Perform fDDA success.")
            } else {
                LogUtils.e("Perform fDDA fail.")
            }

            return result
        }
        if (transactionData.odaType == OdaType.SDA) {
            val sdad = transactionData.getData(0x93) ?: kotlin.run {
                LogUtils.e(" Missing Signed Static Application Data")
                return false
            }
            val signedDataFormatExpect =
                if (transactionData.transactionDecision == TransactionDecision.TC) 0x03.toByte() else 0x93.toByte()

            val result = OdaProcessHelper.verifySDA(
                issuerPublicKey = issuerPK,
                sdad = sdad,
                offlineAuthenticationRecords = transactionData.recordOda,
                signedDataFormatExpect = signedDataFormatExpect
            )

            if (result) {
                LogUtils.d("Perform SDA success.")
            } else {
                LogUtils.e("Perform SDA fail.")
            }

            return result
        }
        return false
    }

    private fun checkCapk(): Boolean {

        val rid = transactionData.getData(0x4F)?.copyOfRange(0, 5)
        val capkIndex = transactionData.getData(0x8F)
        val capk = transactionData.capk
        if (rid == null || capk == null || capkIndex == null) {
            LogUtils.e("Missing capk: $capk, capkIndex:$capkIndex, rid: $rid")
            return false
        } else if (capk.rid.contentEquals(rid) && capkIndex.toHexString().toInt(16) == capk.index) {
            if (capk.sha == null) {
                return true
            } else if (capk.sha.contentEquals(RSAHelper.calculateSHA1(capk.modulus))) {
                return true
            } else {
                LogUtils.d("CAPK verify hash fail.")
                return false
            }

        } else {
            LogUtils.e("Capk not match")
            return false
        }
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
                    transactionData.transactionDecision = TransactionDecision.AAC
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
                    transactionData.transactionDecision = TransactionDecision.AAC
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