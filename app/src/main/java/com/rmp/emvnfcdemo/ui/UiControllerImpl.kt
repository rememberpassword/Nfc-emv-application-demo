package com.rmp.emvnfcdemo.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.rmp.emvengine.data.TransactionDecision
import com.rmp.emvnfcdemo.data.Amount
import com.rmp.emvnfcdemo.data.Currency
import com.rmp.emvnfcdemo.data.TransactionType
import com.rmp.emvnfcdemo.ui.fragment.AmountEntryFragment
import com.rmp.emvnfcdemo.ui.fragment.DetectCardFragment
import com.rmp.emvnfcdemo.ui.fragment.TxnResultFragment
import com.rmp.emvnfcdemo.ui.fragment.LoadingFragment
import com.rmp.emvnfcdemo.ui.fragment.TxnInfoConfirmFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking

class UiControllerImpl(private val fragmentManager: FragmentManager, private val containerId: Int) :
    UiController {


    override suspend fun showAmountEntryScreen(
        transactionType: TransactionType,
        currencies: List<Currency>
    ): Amount? {
        val channel = Channel<Amount>()
        val fragment = AmountEntryFragment(
            transactionType = transactionType.value,
            currencies = currencies,
            cb = {
                channel.trySend(it)
            })
        show(fragment)
        return channel.receiveCatching().getOrNull()
    }

    override suspend fun showDetectCardScreen(
        amount: Amount
    ): UiAction? {
        val channel = Channel<UiAction>()
        val fragment = DetectCardFragment(amount = amount.toDisplayAmount(), cb = {
            channel.trySend(it)
        })
        show(fragment)
        return channel.receiveCatching().getOrNull()
    }

    override fun showProgressingScreen(title: String) {
        val fragment = LoadingFragment(title)
        show(fragment)
    }

    override suspend fun showConfirmInfoScreen(
        transactionType: TransactionType,
        amount: Amount,
        pan: String,
        expiredDate: String,
        txnDate: String,
        txnTime: String,
    ): UiAction? {
        val channel = Channel<UiAction>()
        val fragment = TxnInfoConfirmFragment(
            title = transactionType.value,
            amount = amount.toDisplayAmount(),
            pan = pan,
            expDate = expiredDate,
            txnDate = txnDate,
            txnTime = txnTime,
            cb = {
                channel.trySend(it)
            })
        show(fragment)
        return channel.receiveCatching().getOrNull()
    }

    override suspend fun showTransactionResult(
        transactionDecision: TransactionDecision,
        timeout: Long
    ): UiAction? {
        val channel = Channel<UiAction>()
        val fragment = TxnResultFragment(
            isApprovalTxn = transactionDecision == TransactionDecision.TC,
            timeout = timeout,
            cb = {
                channel.trySend(it)
            })
        show(fragment)
        return channel.receiveCatching().getOrNull()
    }

    private fun show(screen: Fragment) {
        runBlocking(Dispatchers.Main) {
            fragmentManager.beginTransaction().apply {
                replace(containerId, screen)
                commit()
            }
        }
    }
}