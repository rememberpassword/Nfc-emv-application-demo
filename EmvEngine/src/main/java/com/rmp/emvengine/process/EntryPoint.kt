package com.rmp.emvengine.process

import com.rmp.emvengine.data.Aid
import com.rmp.emvengine.data.TlvObject

internal interface EntryPoint {

    fun preprocessing(data: List<TlvObject>)

    fun combinationSelection(list: List<Aid>): List<Aid>?

    fun finalCombinationSelection(aid: Aid):  List<Aid>?

    fun kernelActivation()

    fun initiateTransaction()

    fun readRecord()

    fun offlineDataAuthenticationAndProcessingRestriction()

    fun cardholderVerification(data: List<TlvObject>)

    fun terminalRiskManagement()

    fun terminalActionAnalysis()

    fun cardActionlActionAnalysis()

    fun getLastError(): String?

}