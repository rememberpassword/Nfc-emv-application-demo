package com.rmp.emvnfcdemo.ui

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import com.rmp.emvengine.data.TransactionDecision
import com.rmp.emvnfcdemo.data.Amount
import com.rmp.emvnfcdemo.data.Currency
import com.rmp.emvnfcdemo.data.TransactionType
import com.rmp.emvnfcdemo.ui.fragment.AmountEntryFragment
import com.rmp.emvnfcdemo.ui.fragment.DetectCardFragment
import com.rmp.emvnfcdemo.ui.fragment.ErrorFragment
import com.rmp.emvnfcdemo.ui.fragment.LoadingFragment
import com.rmp.emvnfcdemo.ui.fragment.PinEntryFragment
import com.rmp.emvnfcdemo.ui.fragment.TxnInfoConfirmFragment
import com.rmp.emvnfcdemo.ui.fragment.TxnResultFragment
import com.rmp.emvnfcdemo.ui.fragment.WarningFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking

class UiControllerImpl(private val activity: FragmentActivity, private val containerId: Int) :
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
        val fragment = DetectCardFragment(amount = amount.toAmountWithCurrency(), cb = {
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
        aid: String,
        appName: String,
    ): UiAction? {
        val channel = Channel<UiAction>()
        val fragment = TxnInfoConfirmFragment(
            title = transactionType.value,
            amount = amount.toAmountWithCurrency(),
            pan = pan,
            expDate = expiredDate,
            txnDate = txnDate,
            txnTime = txnTime,
            aid = aid,
            appName = appName,
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

    override suspend fun showWarningScreen(title: String, timeout: Long): UiAction? {
        val channel = Channel<UiAction>()
        val fragment = WarningFragment(
            title = title,
            timeout = timeout,
            cb = {
                channel.trySend(it)
            })
        show(fragment)
        return channel.receiveCatching().getOrNull()
    }

    override suspend fun showErrorScreen(title: String, timeout: Long): UiAction? {
        val channel = Channel<UiAction>()
        val fragment = ErrorFragment(
            title = title,
            timeout = timeout,
            cb = {
                channel.trySend(it)
            })
        show(fragment)
        return channel.receiveCatching().getOrNull()
    }

    override fun showPinEntryScreen() {
        val fragment = PinEntryFragment()
        show(fragment)
    }

    private fun show(screen: Fragment) {
        runBlocking(Dispatchers.Main) {
            //check activity visible or not
            if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                activity.supportFragmentManager.beginTransaction().apply {
                    replace(containerId, screen)
                    commit()
                }
            }
        }
    }
}