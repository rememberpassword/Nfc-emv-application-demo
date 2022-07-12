package com.rmp.emvengine.process

import com.rmp.emvengine.data.TlvObject

interface ClessEmvProcess {

    fun preprocessing(data: List<TlvObject>): List<TlvObject>?

    fun initiateTransaction(data: List<TlvObject>)

    fun readRecord()

    fun offlineDataAuthentication()

    fun processingRestriction()

    fun cardholderVerification()

    fun terminalRiskManagement()

    fun terminalActionAnalysis()

    fun cardActionlActionAnalysis()

}