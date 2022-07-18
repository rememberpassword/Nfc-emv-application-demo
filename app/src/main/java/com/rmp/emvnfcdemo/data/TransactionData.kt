package com.rmp.emvnfcdemo.data

import com.rmp.emvengine.data.CvmMethod
import com.rmp.emvengine.data.TransactionDecision

class TransactionData {

    var transactionType: TransactionType = TransactionType.SALE
    var amount: Amount? = null
    var cvmMethod: CvmMethod? = null
    var txnDecision: TransactionDecision? = null
    var txnDate: String? = null // format: yyMMdd
    var txnDateDisplay: String? = null  // format: dd/MM/yyyy
    var txnTime: String? = null  // format: HHmmss
    var txnTimeDisplay: String? = null  // format: HH:mm:ss

    //
    var pan: String? = null
    var expiredDate: String? = null
    var track2: Track2? = null
}