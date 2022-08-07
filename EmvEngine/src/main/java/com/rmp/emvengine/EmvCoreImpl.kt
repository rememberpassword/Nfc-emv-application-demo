package com.rmp.emvengine

import android.util.Log
import com.rmp.emvengine.common.toHexString
import com.rmp.emvengine.data.*
import com.rmp.emvengine.process.EntryPoint
import com.rmp.emvengine.process.EntryPointImpl

class EmvCoreImpl(private val cardReader: CardReader) : EmvCore {
    private val TAG = "EmvCoreImpl"
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
        if (candidateList == null || entryPoint.getLastError() != null) {
            return StartAppSelectionResult(error = entryPoint.getLastError()?.toEmvError(), null)
        }
        if (candidateList.isEmpty()) {
            return StartAppSelectionResult(error = EmvError.NO_APPLICATION, null)
        }
        return StartAppSelectionResult(error = null, candidateList = candidateList)

    }

    override fun finalAppSelection(aid: Aid): FinalAppSelectionResult {
        if (cardReader.isCardRemoved()) {
            return FinalAppSelectionResult(error = EmvError.COMMUNICATE_ERROR, null, null, null)
        }
        if (_entryMode == EntryMode.CLESS) {
            val remainsCandidateList = entryPoint.finalCombinationSelection(aid)
            if (remainsCandidateList != null) {
                return FinalAppSelectionResult(
                    error = null,
                    kernelId = null,
                    aidSelected = null,
                    candidateList = remainsCandidateList
                )
            }
        } else {
            TODO()
        }
        if (entryPoint.getLastError() != null) {
            return FinalAppSelectionResult(
                error = entryPoint.getLastError()?.toEmvError(),
                null,
                null,
                null
            )
        }
        if (_entryMode == EntryMode.CLESS) {
            entryPoint.kernelActivation()
        } else {
            TODO()
        }
        if (entryPoint.getLastError() != null) {
            return FinalAppSelectionResult(
                error = entryPoint.getLastError()?.toEmvError(),
                null,
                null,
                null
            )
        }

        val cardAid = transactionData.cardData[0x84]
        return FinalAppSelectionResult(
            error = null,
            kernelId = transactionData.kernelId,
            aidSelected = cardAid?.value?.toHexString(),
            candidateList = null
        )
    }

    override fun startTransaction(data: List<TlvObject>): StartTransactionResult {
        if (cardReader.isCardRemoved()) {
            return StartTransactionResult(error = EmvError.COMMUNICATE_ERROR)
        }
        if (_entryMode == EntryMode.CLESS) {
            entryPoint.preprocessing(data)
        } else {
            TODO()
        }
        if (entryPoint.getLastError() != null) {
            Log.d(TAG, "last error:" + entryPoint.getLastError())
            return StartTransactionResult(error = entryPoint.getLastError()?.toEmvError())
        }
        if (_entryMode == EntryMode.CLESS) {
            entryPoint.initiateTransaction()
        } else {
            TODO()
        }
        if (entryPoint.getLastError() != null) {
            Log.d(TAG, "last error:" + entryPoint.getLastError())
            if (entryPoint.getLastError() == EmvErrorLevel.INITIATE_TXN.code + EmvCoreError.SELECT_NEXT) {
                return StartTransactionResult(
                    error = entryPoint.getLastError()?.toEmvError(),
                    candidateList = transactionData.cardAppData
                )

            } else {
                return StartTransactionResult(error = entryPoint.getLastError()?.toEmvError())
            }

        }
        if (_entryMode == EntryMode.CLESS) {
            entryPoint.readRecord()
        } else {
            TODO()
        }
        if (entryPoint.getLastError() != null) {
            Log.d(TAG, "last error:" + entryPoint.getLastError())
            return StartTransactionResult(error = entryPoint.getLastError()?.toEmvError())
        }
        return StartTransactionResult(
            error = null,
            capkIndex = transactionData.getData(0x8F)?.toHexString()?.toInt(16)
        )
    }

    override fun processTransaction(data: List<TlvObject>, capk: Capk?): ProcessTransactionResult {


        if (cardReader.isCardRemoved()) {
            return ProcessTransactionResult(error = EmvError.COMMUNICATE_ERROR, null)
        }
        transactionData.capk = capk
        if (_entryMode == EntryMode.CLESS) {
            entryPoint.offlineDataAuthenticationAndProcessingRestriction()
        } else {
            TODO()
        }
        if (entryPoint.getLastError() != null) {
            Log.d(TAG, "last error:" + entryPoint.getLastError())
            return ProcessTransactionResult(error = entryPoint.getLastError()?.toEmvError(), null)
        }
        if (_entryMode == EntryMode.CLESS) {

            entryPoint.cardholderVerification(listOf())
            if (entryPoint.getLastError() != null) {
                Log.d(TAG, "last error:" + entryPoint.getLastError())
                return ProcessTransactionResult(
                    error = entryPoint.getLastError()?.toEmvError(),
                    null
                )
            }

            entryPoint.terminalRiskManagement()
            if (entryPoint.getLastError() != null) {
                Log.d(TAG, "last error:" + entryPoint.getLastError())
                return ProcessTransactionResult(
                    error = entryPoint.getLastError()?.toEmvError(),
                    null
                )
            }

            entryPoint.cardActionlActionAnalysis()
            if (entryPoint.getLastError() != null) {
                Log.d(TAG, "last error:" + entryPoint.getLastError())
                return ProcessTransactionResult(
                    error = entryPoint.getLastError()?.toEmvError(),
                    null
                )
            }
        }
        return ProcessTransactionResult(error = null, cvm = transactionData.cvm)

    }

    override fun cardholderVerification(
        cvmAccept: Boolean?,
        pinEntryStatus: PinEntryStatus?,
        offPinVerifyResult: OffPinVerifyResult?
    ): CvmResult {
        if (_entryMode == EntryMode.CLESS) {
            if (transactionData.cvm == CvmMethod.ONLINE_PIN) {
                if (cvmAccept != true || pinEntryStatus != PinEntryStatus.EXC_SUCCESS) {
                    //decline txn when cvm fail
                    transactionData.transactionDecision = TransactionDecision.AAC
                }

            } else if (transactionData.cvm == CvmMethod.CDCVM) {
                //TODO( impl late)
            }
        } else {
            TODO()
        }
        if (entryPoint.getLastError() != null) {
            Log.d(TAG, "last error:" + entryPoint.getLastError())
            return CvmResult(error = entryPoint.getLastError()?.toEmvError())
        }
        return CvmResult()
    }

    override fun riskManagement(): TransactionDecision {
        if (_entryMode == EntryMode.CONTACT) {
            TODO()
        }
        if (entryPoint.getLastError() != null) {
            Log.d(TAG, "last error:" + entryPoint.getLastError())
            return TransactionDecision.AAC
        }
        return transactionData.transactionDecision
    }

    override fun completionTransaction(data: List<TlvObject>): CompletionTransactionResult {
        if (_entryMode == EntryMode.CLESS) {
            val hostAuthCode = data.firstOrNull {
                it.tag == 0x8AL
            }?.valueString
            if (hostAuthCode in HostAuthResponseCode.LIST_APPROVE_RESPONSE) {
                transactionData.transactionDecision = TransactionDecision.TC
            }
        } else {
            TODO()
        }
        if (entryPoint.getLastError() != null) {
            Log.d(TAG, "last error:" + entryPoint.getLastError())
            return CompletionTransactionResult(error = entryPoint.getLastError()?.toEmvError())
        }
        return CompletionTransactionResult(transactionDecision = transactionData.transactionDecision)
    }

    override fun getData(tag: Long): ByteArray? {
        return transactionData.getData(tag)
    }

    override fun getData(tag: String): ByteArray? {
        return getData(tag.toLong(16))
    }

    override fun terminate() {
        //TODO("Not yet implemented")
    }
}