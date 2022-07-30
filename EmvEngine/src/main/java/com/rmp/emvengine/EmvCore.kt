package com.rmp.emvengine

import com.rmp.emvengine.data.Aid
import com.rmp.emvengine.data.Capk
import com.rmp.emvengine.data.CvmMethod
import com.rmp.emvengine.data.EmvError
import com.rmp.emvengine.data.KernelId
import com.rmp.emvengine.data.PinEntryStatus
import com.rmp.emvengine.data.TlvObject
import com.rmp.emvengine.data.TransactionDecision

interface EmvCore {

    fun setEntryMode(entryMode: EntryMode)

    fun startAppSelection(aidsSupported: List<Aid>): StartAppSelectionResult

    fun finalAppSelection(aid: Aid): FinalAppSelectionResult

    fun startTransaction(data: List<TlvObject>): StartTransactionResult

    fun processTransaction(data: List<TlvObject>, capk: Capk?): ProcessTransactionResult

    fun cardholderVerification(
        cvmAccept: Boolean?,
        pinEntryStatus: PinEntryStatus?,
        offPinVerifyResult: OffPinVerifyResult?
    ): CvmResult

    fun riskManagement(): TransactionDecision

    fun completionTransaction(data: List<TlvObject>): CompletionTransactionResult

    fun getData(tag: Long): ByteArray?

    fun getData(tag: String): ByteArray?

    fun terminate()
}

enum class EntryMode {
    CONTACT,
    CLESS
}

data class CompletionTransactionResult(
    val error: EmvError? = null,
    val transactionDecision: TransactionDecision? = null,
)

data class CvmResult(
    val error: EmvError? = null,
    val nextCvm: CvmMethod?= null,
    val isVerifyOfflinePin: Boolean?= null,
    val capk: Capk?= null,
    val random: ByteArray?= null,
    val isNeedEncrypt: Boolean?= null,

)

data class ProcessTransactionResult(
    val error: EmvError?,
    val cvm: CvmMethod?
)

data class StartTransactionResult(
    val error: EmvError?,
    val capkIndex: Int? = null
)

data class FinalAppSelectionResult(
    val error: EmvError?,
    val aidSelected: String?,
    val kernelId: KernelId?
)

data class StartAppSelectionResult(
    val error: EmvError?,
    val candidateList: List<Aid>?
)

data class OffPinVerifyResult(
    val isVerifySuccess: Boolean,
    val sw: String
)