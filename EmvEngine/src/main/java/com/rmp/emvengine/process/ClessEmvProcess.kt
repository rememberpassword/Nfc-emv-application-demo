package com.rmp.emvengine.process

import com.rmp.emvengine.data.TlvObject

interface ClessEmvProcess {

    fun preprocessing(data: List<TlvObject>)

    fun initiateTransaction()

    fun readRecord()

    fun offlineDataAuthenticationAndProcessingRestriction()

    fun cardholderVerification()

    fun terminalRiskManagement()

    fun terminalActionAnalysis()

    fun cardActionlActionAnalysis()

    fun getLastError(): String?

}