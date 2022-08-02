package com.rmp.emvnfcdemo.ui

import com.rmp.emvengine.data.TransactionDecision
import com.rmp.emvnfcdemo.data.Amount
import com.rmp.emvnfcdemo.data.Currency
import com.rmp.emvnfcdemo.data.TransactionType

interface UiController {

    suspend fun showAmountEntryScreen(
        transactionType: TransactionType,
        currencies: List<Currency>
    ): Amount?

    suspend fun showDetectCardScreen(amount: Amount): UiAction?

    fun showProgressingScreen(
        title: String
    )

    suspend fun showConfirmInfoScreen(
        transactionType: TransactionType,
        amount: Amount,
        pan: String,
        expiredDate: String,
        txnDate: String,
        txnTime: String,
        aid: String,
        appName: String,
    ): UiAction?

    suspend fun showTransactionResult(
        transactionDecision: TransactionDecision,
        timeout: Long = 3000L
    ): UiAction?

    suspend fun showWarningScreen(title: String, timeout: Long = 2000L): UiAction?

    suspend fun showErrorScreen(title: String, timeout: Long = 2000L): UiAction?

    fun showPinEntryScreen()

}

enum class UiAction {
    CONFIRM,
    TIMEOUT,
    CANCEL
}
