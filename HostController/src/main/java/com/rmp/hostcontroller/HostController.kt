package com.rmp.hostcontroller

interface HostController {

    fun performFinancialTransaction(transactionData: HostTransactionData): HostTransactionData

}