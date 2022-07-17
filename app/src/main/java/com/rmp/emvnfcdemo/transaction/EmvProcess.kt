package com.rmp.emvnfcdemo.transaction

import android.app.Activity
import android.util.Log
import com.rmp.emvengine.EmvCore
import com.rmp.emvengine.EmvCoreImpl
import com.rmp.emvengine.EntryMode
import com.rmp.emvengine.common.toBcd
import com.rmp.emvengine.data.Aid
import com.rmp.emvengine.data.CvmMethod
import com.rmp.emvengine.data.EmvTags
import com.rmp.emvengine.data.PinEntryStatus
import com.rmp.emvengine.data.TlvObject
import com.rmp.emvengine.data.TransactionDecision
import com.rmp.emvnfcdemo.transaction.cardreader.CardReaderIpml

class EmvProcess(private val activity: Activity, private val updateUi: (String) -> Unit) {
    private val TAG = "EmvProcess"

    private var cardReader: CardReaderIpml? = null
    private lateinit var emvCore: EmvCore

    suspend fun execute(amount: Long) {

        cardReader = CardReaderIpml(activity)
        updateUi("-->Start search card")
        val result = cardReader?.detectClessCardAndActive()
        updateUi("Search card status: $result")
        if (result == true) {
            emvCore = EmvCoreImpl(cardReader!!)
            emvCore.setEntryMode(EntryMode.CLESS)
            updateUi("-->Start application selection")
            val startAppSelectionResult = emvCore.startAppSelection(buildTerminalAidsSupported())
            //show candidate list
            Log.d(TAG, "$startAppSelectionResult")
            if (startAppSelectionResult.error != null || startAppSelectionResult.candidateList == null) {
                updateUi("->Error: ${startAppSelectionResult.error}")
                return
            }
            updateUi("->List Aid:")
            if (startAppSelectionResult.candidateList?.isEmpty() == true) {
                updateUi("- Error no app available")
                return
            }
            startAppSelectionResult.candidateList?.forEach {
                updateUi("-Aid:${it.aid}")
            }
            val aidSelected =
                startAppSelectionResult.candidateList!!.maxByOrNull { it.priority ?: 0 }
                    ?: startAppSelectionResult.candidateList!!.first()
            updateUi("-->Final select aid: ${aidSelected.aid}")
            val finalAppSelectionResult = emvCore.finalAppSelection(aidSelected)
            if (finalAppSelectionResult.error != null) {
                updateUi("->Error: ${finalAppSelectionResult.error}")
                return
            }
            updateUi("->Selected success aid: ${finalAppSelectionResult.aidSelected}")
            updateUi("->Kernel Id: ${finalAppSelectionResult.kernelId}")

            updateUi("-->Start transaction")
            val startTransactionData = buildStartTransactionData(amount)
            val startTransactionResult = emvCore.startTransaction(startTransactionData)
            if (startTransactionResult.error != null) {
                updateUi("->Error: ${startTransactionResult.error}")
                return
            }
            updateUi("->Start transaction success")
            updateUi("-->Process transaction")
            val dataUpdate = listOf<TlvObject>()
            val processTransactionResult = emvCore.processTransaction(dataUpdate)
            if (processTransactionResult.error != null) {
                updateUi("->Error: ${processTransactionResult.error}")
                return
            }
            updateUi("->First cvm: ${processTransactionResult.cvm}")
            processCvm(processTransactionResult.cvm)
            updateUi("-->Risk management")
            val transactionDecision = emvCore.riskManagement()
            updateUi("-->Transaction Decision: $transactionDecision")
            if (transactionDecision == TransactionDecision.ARQC) {
                processGoOnline()
            }
        }

    }

    private fun processGoOnline() {
        updateUi("-->Send transaction to host")
        updateUi("-->Host response")
        val hostAuthCode = "00"
        updateUi("->Host authorize code(8A):00")
        updateUi("-->Completion Transaction")
        val completionResult = emvCore.completionTransaction(listOf(TlvObject(0x8A, hostAuthCode)))
        updateUi("-->Final transaction decision: ${completionResult.transactionDecision}")

    }

    private fun processCvm(cvm: CvmMethod?) {
        if (cvm == CvmMethod.ONLINE_PIN) {
            val status = showPinEntry()
            updateUi("-> Pin entry status: $status")
            emvCore.cardholderVerification(cvmAccept = true, pinEntryStatus = status, null)
        } else {
            //TODO
        }
    }

    private fun showPinEntry(): PinEntryStatus {
        //TODO("Not yet implemented")
        return PinEntryStatus.EXC_SUCCESS

    }

    private fun buildStartTransactionData(amount: Long): List<TlvObject> {

        return listOf<TlvObject>(
            TlvObject(0x9F02, amount.toBcd(6)),//amount
            TlvObject(0x9F03, 0L.toBcd(6)),//cashback amount
            TlvObject(0x5F2AL, "0840"),//currency code
            TlvObject(0x5F36L, 2L.toBcd(1)),//currency exponent
            TlvObject(0x9CL, "00"),//txn type
            TlvObject(0x9F21L, "120000"),//txn time HHMMSS
            TlvObject(0x9AL, "220715"),//txn date YYMMDD
            TlvObject(0x9F41L, "00000001"),// txn sequen counter
            TlvObject(0x9F1AL, "0840"),// terminal country code
            TlvObject(0x9F66L, "A6004000"),// ttq
            TlvObject(EmvTags.EMV_CL_FLOOR_LIMIT.value, 1000L.toBcd(6)),// EMV_CL_FLOOR_LIMIT.value,
            TlvObject(EmvTags.EMV_CL_CVM_LIMIT.value, 1500L.toBcd(6)),// EMV_CL_CVM_LIMIT.value,
            TlvObject(EmvTags.EMV_CL_TRANS_LIMIT.value, 5000L.toBcd(6)),// EMV_CL_TRANS_LIMIT.value,


        )
    }

    private fun buildTerminalAidsSupported(): List<Aid> {
        return listOf<Aid>(
            Aid(aid = "A000000003", isPartial = true)
        )
    }

    fun stopTransaction() {
        cardReader?.close()
    }
}