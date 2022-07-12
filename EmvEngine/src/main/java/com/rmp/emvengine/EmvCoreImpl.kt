package com.rmp.emvengine

import com.rmp.emvengine.common.toHexString
import com.rmp.emvengine.data.*
import com.rmp.emvengine.process.EntryPoint
import com.rmp.emvengine.process.EntryPointImpl

class EmvCoreImpl(private val cardReader: CardReader) : EmvCore {

    private var transactionData = TransactionData()
    private var _entryMode: EntryMode? = null
    private lateinit var entryPoint: EntryPoint

    override fun setEntryMode(entryMode: EntryMode) {
        if (_entryMode == null) {
            _entryMode = entryMode
            if (entryMode == EntryMode.CLESS) {
                entryPoint = EntryPointImpl(cardReader, transactionData)
            } else {
                TODO()
            }
        }
    }


    override fun startAppSelection(aidsSupported: List<Aid>): StartAppSelectionResult {
        if (cardReader.isCardRemoved()) {
            return StartAppSelectionResult(error = EmvError.COMMUNICATE_ERROR, null)
        }
        val candidateList = if (_entryMode == EntryMode.CLESS) {
            entryPoint.combinationSelection(aidsSupported)
        } else {
            TODO()
        }
        if (candidateList == null || entryPoint.getLastError() != 0) {
            return StartAppSelectionResult(error = EmvError.OTHER_ERROR, null)
        }
        if (candidateList.isEmpty()) {
            return StartAppSelectionResult(error = EmvError.NO_APPLICATION, null)
        }
        return StartAppSelectionResult(error = null, candidateList = candidateList)

    }

    override fun finalAppSelection(aid: Aid): FinalAppSelectionResult {
        if (cardReader.isCardRemoved()) {
            return FinalAppSelectionResult(error = EmvError.COMMUNICATE_ERROR, null, null)
        }
        if (_entryMode == EntryMode.CLESS) {
            entryPoint.finalCombinationSelection(aid)
        } else {
            TODO()
        }
        if (entryPoint.getLastError() != 0) {
            return FinalAppSelectionResult(error = EmvError.OTHER_ERROR, null, null)
        }
        if (_entryMode == EntryMode.CLESS) {
            entryPoint.kernelActivation()
        } else {
            TODO()
        }
        if (entryPoint.getLastError() != 0) {
            return FinalAppSelectionResult(error = EmvError.OTHER_ERROR, null, null)
        }

        val cardAid = transactionData.cardData[0x84]
        return FinalAppSelectionResult(
            error = null,
            kernelId = transactionData.kernelId,
            aidSelected = cardAid?.value?.toHexString()
        )
    }

    override fun startTransaction(data: List<TlvObject>): StartTransactionResult {
        TODO("Not yet implemented")
    }

    override fun processTransaction(data: List<TlvObject>): ProcessTransactionResult {
        TODO("Not yet implemented")
    }

    override fun cardholderVerification(
        cvmAccept: Boolean?,
        pinEntryStatus: PinEntryStatus?,
        offPinVerifyResult: OffPinVerifyResult?
    ): CvmResult {
        TODO("Not yet implemented")
    }

    override fun riskManagement(): TransactionDecision {
        TODO("Not yet implemented")
    }

    override fun completionTransaction(data: List<TlvObject>): CompletionTransactionResult {
        TODO("Not yet implemented")
    }

    override fun getData(tag: Long): ByteArray {
        TODO("Not yet implemented")
    }

    override fun getData(tag: String): ByteArray {
        TODO("Not yet implemented")
    }

    override fun terminate() {
        TODO("Not yet implemented")
    }
}