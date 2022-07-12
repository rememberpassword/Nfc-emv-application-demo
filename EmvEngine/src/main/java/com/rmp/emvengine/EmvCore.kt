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

    fun processTransaction(data: List<TlvObject>): ProcessTransactionResult

    fun cardholderVerification(
        cvmAccept: Boolean?,
        pinEntryStatus: PinEntryStatus?,
        offPinVerifyResult: OffPinVerifyResult?
    ): CvmResult

    fun riskManagement(): TransactionDecision

    fun completionTransaction(data: List<TlvObject>): CompletionTransactionResult

    fun getData(tag: Long): ByteArray

    fun getData(tag: String): ByteArray

    fun terminate()
}

enum class EntryMode {
    CONTACT,
    CLESS
}

data class CompletionTransactionResult(
    val error: EmvError?,
    val transactionDecision: TransactionDecision
)

data class CvmResult(
    val error: EmvError?,
    val nextCvm: CvmMethod?,
    val isVerifyOfflinePin: Boolean?,
    val capk: Capk?,
    val random: ByteArray?,
    val isNeedEncrypt: Boolean?

)

data class ProcessTransactionResult(
    val error: EmvError?,
    val cvm: CvmMethod?
)

data class StartTransactionResult(
    val error: EmvError?
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