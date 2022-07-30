package com.rmp.emvnfcdemo.transaction

import android.app.Activity
import android.util.Log
import com.rmp.emvengine.EmvCore
import com.rmp.emvengine.EmvCoreImpl
import com.rmp.emvengine.EntryMode
import com.rmp.emvengine.common.hexToByteArray
import com.rmp.emvengine.common.toBcd
import com.rmp.emvengine.common.toHexString
import com.rmp.emvengine.data.Aid
import com.rmp.emvengine.data.Capk
import com.rmp.emvengine.data.CvmMethod
import com.rmp.emvengine.data.EmvError
import com.rmp.emvengine.data.EmvTags
import com.rmp.emvengine.data.PinEntryStatus
import com.rmp.emvengine.data.TlvObject
import com.rmp.emvengine.data.TransactionDecision
import com.rmp.emvnfcdemo.data.Amount
import com.rmp.emvnfcdemo.data.Currency
import com.rmp.emvnfcdemo.data.TransactionData
import com.rmp.emvnfcdemo.data.TransactionType
import com.rmp.emvnfcdemo.data.toTrack2
import com.rmp.emvnfcdemo.transaction.cardreader.CardReaderIpml
import com.rmp.emvnfcdemo.ui.UiAction
import com.rmp.emvnfcdemo.ui.UiController
import com.rmp.secure.SecureEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EmvProcess(private val activity: Activity, private val uiController: UiController, private val secureEngine: SecureEngine) {
    private val TAG = "EmvProcess"

    private var cardReader: CardReaderIpml? = null
    private lateinit var emvCore: EmvCore

    private lateinit var transactionData: TransactionData

    suspend fun execute() {
        transactionData = TransactionData()

        transactionData.amount = captureAmount()

        cardReader = CardReaderIpml(activity)
        val captureSuccess = captureCard()
        if (!captureSuccess) return stopTransaction()

        val cvmResult = processCvm(transactionData.cvmMethod)
        if (!cvmResult) return stopTransaction()

        val transactionDecision = emvCore.riskManagement()
        transactionData.txnDecision = transactionDecision

        if (transactionDecision == TransactionDecision.ARQC) {
            processGoOnlineTransaction()
        } else {
            completionTransaction(transactionData.txnDecision!!)
        }
    }

    private suspend fun captureCard(): Boolean {
        val detectResult = detectCard(transactionData.amount!!)
        //detect fail, return
        if (!detectResult) return false
        emvCore = EmvCoreImpl(cardReader!!)
        emvCore.setEntryMode(EntryMode.CLESS)

        val selectError = selectApplication()
        if (selectError != null) {
            when (selectError) {
                EmvError.COMMUNICATE_ERROR,
                EmvError.PRESENT_CARD_AGAIN -> {
                    stopTransaction()
                    uiController.showWarningScreen("Please remove and tap card again.")
                    return captureCard()
                }
                EmvError.KERNEL_ABSENT -> {
                    stopTransaction()
                    uiController.showErrorScreen("Card not support.")
                    return false
                }
                EmvError.NO_APPLICATION -> {
                    stopTransaction()
                    uiController.showErrorScreen("No application available.")
                    return false
                }
                else -> {
                    stopTransaction()
                    uiController.showErrorScreen("EMV ERROR.")
                    return false
                }
            }
        }

        val startError = startTransaction(transactionData.amount!!)
        if (startError != null) {
            //cancel by user
            if(startError.first) return false
            //got error from emv
            when (startError.second) {
                EmvError.COMMUNICATE_ERROR,
                EmvError.PRESENT_CARD_AGAIN -> {
                    stopTransaction()
                    uiController.showWarningScreen("Please remove and tap card again.")
                    return captureCard()
                }
                EmvError.TXN_EXCEED_LIMIT -> {
                    stopTransaction()
                    uiController.showErrorScreen("Exceed transaction limit.")
                    return false
                }
                else -> {
                    stopTransaction()
                    uiController.showErrorScreen("EMV ERROR.")
                    return false
                }
            }
        }
        return true
    }

    private suspend fun completionTransaction(transactionDecision: TransactionDecision) {
        uiController.showTransactionResult(transactionDecision)
        stopTransaction()
    }

    private suspend fun processGoOnlineTransaction() {
        uiController.showProgressingScreen("Host processing ...")
        Log.d(TAG, "-->Send transaction to host")
        delay(2000L)
        Log.d(TAG, "-->Host response")
        val hostAuthCode = "00"
        Log.d(TAG, "->Host authorize code(8A):$hostAuthCode")
        Log.d(TAG, "-->Completion Transaction")
        val completionResult = emvCore.completionTransaction(listOf(TlvObject(0x8A, hostAuthCode)))
        Log.d(TAG, "-->Final transaction decision: ${completionResult.transactionDecision}")
        transactionData.txnDecision = completionResult.transactionDecision
        completionTransaction(transactionData.txnDecision!!)
    }

    private suspend fun startTransaction(amount: Amount): Pair<Boolean, EmvError?>? {
        Log.d(TAG, "-->Start transaction")
        val startTransactionData = buildStartTransactionData(amount)
        val startTransactionResult = emvCore.startTransaction(startTransactionData)
        if (startTransactionResult.error != null) {
            Log.d(TAG, "->Error: ${startTransactionResult.error}")
            return Pair(false, startTransactionResult.error)
        }
        Log.d(TAG, "->Start transaction success")
        Log.d(TAG,"->CAPK index:${startTransactionResult.capkIndex?.toString(16)}")
        //
        Log.d(TAG, "-->Process transaction")
        val dataUpdate = listOf<TlvObject>()
        val visaCapk92 = Capk(
            index = 0x92,
            rid = "A000000003".hexToByteArray(),
            modulus = "996AF56F569187D09293C14810450ED8EE3357397B18A2458EFAA92DA3B6DF6514EC060195318FD43BE9B8F0CC669E3F844057CBDDF8BDA191BB64473BC8DC9A730DB8F6B4EDE3924186FFD9B8C7735789C23A36BA0B8AF65372EB57EA5D89E7D14E9C7B6B557460F10885DA16AC923F15AF3758F0F03EBD3C5C2C949CBA306DB44E6A2C076C5F67E281D7EF56785DC4D75945E491F01918800A9E2DC66F60080566CE0DAF8D17EAD46AD8E30A247C9F".hexToByteArray(),
            exponent = "03".hexToByteArray(),
            sha = null
        )
        val processTransactionResult = emvCore.processTransaction(dataUpdate, visaCapk92)
        if (processTransactionResult.error != null) {
            Log.d(TAG, "->Error: ${processTransactionResult.error}")
            return Pair(false, processTransactionResult.error)
        }
        collectEmvData()
        //show removed card screen
        uiController.showWarningScreen("Please, remove card!")
        //
        val uiAction = uiController.showConfirmInfoScreen(
            transactionType = transactionData.transactionType,
            amount = transactionData.amount!!,
            pan = transactionData.pan!!.maskToDisplay(),
            expiredDate = transactionData.expiredDate ?: "",
            txnDate = transactionData.txnDateDisplay ?: "",
            txnTime = transactionData.txnTimeDisplay ?: ""
        )
        if (uiAction != UiAction.CONFIRM) {
            return Pair(true, null)
        }

        transactionData.cvmMethod = processTransactionResult.cvm
        return null
    }

    private fun String.maskToDisplay():String{
        //visible first and last 4 digit
        val firstDigit = this.substring(0,4)
        val lastDigit = this.substring(this.length-4,this.length)
        return firstDigit+ lastDigit.padStart(this.length-4,'*')
    }

    private fun collectEmvData() {
        transactionData.pan =
            emvCore.getData(0x5A)?.toHexString()

        transactionData.track2 = emvCore.getData(0x57)?.toHexString()?.toTrack2()
            ?.also {
                transactionData.pan = it.pan
                transactionData.expiredDate = it.expDate
            }
        emvCore.getData(0x5F24)?.toHexString()?.let {
            transactionData.expiredDate = it
        }
    }

    private fun selectApplication(): EmvError? {
        uiController.showProgressingScreen("Capture card")

        Log.d(TAG, "-->Start application selection")
        val startAppSelectionResult = emvCore.startAppSelection(buildTerminalAidsSupported())
        //show candidate list
        Log.d(TAG, "$startAppSelectionResult")
        if (startAppSelectionResult.error != null || startAppSelectionResult.candidateList == null) {
            Log.d(TAG, "->Error: ${startAppSelectionResult.error}")
            return startAppSelectionResult.error
        }
        Log.d(TAG, "->List Aid:")
        if (startAppSelectionResult.candidateList?.isEmpty() == true) {
            Log.d(TAG, "- Error no app available")
            return EmvError.NO_APPLICATION
        }
        startAppSelectionResult.candidateList?.forEach {
            Log.d(TAG, "-Aid:${it.aid}")
        }
        val aidSelected =
            startAppSelectionResult.candidateList!!.maxByOrNull { it.priority ?: 0 }
                ?: startAppSelectionResult.candidateList!!.first()
        Log.d(TAG, "-->Final select aid: ${aidSelected.aid}")
        val finalAppSelectionResult = emvCore.finalAppSelection(aidSelected)
        if (finalAppSelectionResult.error != null) {
            Log.d(TAG, "->Error: ${finalAppSelectionResult.error}")
            return finalAppSelectionResult.error
        }
        Log.d(TAG, "->Selected success aid: ${finalAppSelectionResult.aidSelected}")
        Log.d(TAG, "->Kernel Id: ${finalAppSelectionResult.kernelId}")
        return null
    }

    private suspend fun detectCard(amount: Amount): Boolean {

        val channel = Channel<Boolean?>()
        val job = SupervisorJob()
        CoroutineScope(Dispatchers.IO + job).launch {
            val uiAction = uiController.showDetectCardScreen(amount)
            uiAction?.let { channel.trySend(null) }
        }
        CoroutineScope(Dispatchers.Default + job).launch {
            val result = cardReader?.detectClessCardAndActive(10000)
            channel.trySend(result)
        }
        val detectResult = channel.receive()
        job.cancel()
        when (detectResult) {
            true -> {
                //detect success
                return true
            }
            false -> {
                //timeout
                uiController.showErrorScreen("Timeout")
                return false
            }
            null -> {
                //user cancel
                return false
            }

        }


    }

    private suspend fun captureAmount(): Amount {
        val currencySupported = listOf(Currency.USD, Currency.EURO)

        return uiController.showAmountEntryScreen(
            transactionType = TransactionType.SALE,
            currencies = currencySupported
        ) ?: Amount(0, Currency.USD)
    }

    private suspend fun processCvm(cvm: CvmMethod?): Boolean {
        if (cvm == CvmMethod.ONLINE_PIN) {
            val status = showPinEntry()
            Log.d(TAG, "-> Pin entry status: $status")
            emvCore.cardholderVerification(cvmAccept = true, pinEntryStatus = status, null)
        } else {
            //TODO
        }
        return true
    }

    private suspend fun showPinEntry(): PinEntryStatus {
        uiController.showPinEntryScreen()
        val pinResult = secureEngine.getPinEntry().showPinEntry(transactionData.pan!!)
        val pinEntryStatus = when(pinResult.pinEntryStatus){
            com.rmp.secure.pin.PinEntryStatus.ERROR -> PinEntryStatus.ERROR
            com.rmp.secure.pin.PinEntryStatus.PIN_ENTERED -> PinEntryStatus.EXC_SUCCESS
            com.rmp.secure.pin.PinEntryStatus.CANCEL -> PinEntryStatus.CANCEL
            com.rmp.secure.pin.PinEntryStatus.BYPASS -> PinEntryStatus.BYPASS
        }
        return pinEntryStatus
    }

    private fun buildStartTransactionData(amount: Amount): List<TlvObject> {

        val emvDateFormat = "yyMMdd"
        val displayDateFormat = "dd/MM/yyyy"
        val emvTimeFormat = "HHmmss"
        val displayTimeFormat = "HH:mm:ss"
        val date = Date()

        transactionData.txnDate = SimpleDateFormat(emvDateFormat, Locale.getDefault()).format(date)
        transactionData.txnDateDisplay =
            SimpleDateFormat(displayDateFormat, Locale.getDefault()).format(date)
        transactionData.txnTime = SimpleDateFormat(emvTimeFormat, Locale.getDefault()).format(date)
        transactionData.txnTimeDisplay =
            SimpleDateFormat(displayTimeFormat, Locale.getDefault()).format(date)

        return listOf<TlvObject>(
            TlvObject(0x9F02, amount.value.toBcd(6)),//amount
            TlvObject(0x9F03, 0L.toBcd(6)),//cashback amount
            TlvObject(0x5F2AL, amount.currency.code.toLong().toBcd(2)),//currency code
            TlvObject(0x5F36L, amount.currency.exponent.toLong().toBcd(1)),//currency exponent
            TlvObject(0x9CL, "00"),//txn type
            TlvObject(0x9F21L, transactionData.txnTime!!),//txn time HHMMSS
            TlvObject(0x9AL, transactionData.txnDate!!),//txn date YYMMDD
            TlvObject(0x9F41L, 1L.toBcd(4)),// txn sequen counter
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