package com.rmp.emvengine.process

import com.rmp.emvengine.data.Aid
import com.rmp.emvengine.data.TlvObject

interface EntryPoint {

    fun preprocessing(data: List<TlvObject>)

    fun combinationSelection(list: List<Aid>): List<Aid>?

    fun finalCombinationSelection(aid: Aid)

    fun kernelActivation()

    fun initiateTransaction(data: List<TlvObject>)

    fun readRecord()

    fun offlineDataAuthentication()

    fun processingRestriction()

    fun cardholderVerification()

    fun terminalRiskManagement()

    fun terminalActionAnalysis()

    fun cardActionlActionAnalysis()

    fun getLastError(): Int

}