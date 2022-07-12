package com.rmp.emvengine

import com.rmp.emvengine.data.Aid
import com.rmp.emvengine.data.TlvObject
import com.rmp.emvengine.process.ClessEmvProcess

internal class VisaClessProcess(private val cardReader: CardReader) : ClessEmvProcess {

    override fun preprocessing(data: List<TlvObject>): List<TlvObject>? {
        TODO("Not yet implemented")
    }

    override fun initiateTransaction(data: List<TlvObject>) {
        TODO("Not yet implemented")
    }

    override fun readRecord() {
        TODO("Not yet implemented")
    }

    override fun offlineDataAuthentication() {
        TODO("Not yet implemented")
    }

    override fun processingRestriction() {
        TODO("Not yet implemented")
    }

    override fun cardholderVerification() {
        TODO("Not yet implemented")
    }

    override fun terminalRiskManagement() {
        TODO("Not yet implemented")
    }

    override fun terminalActionAnalysis() {
        TODO("Not yet implemented")
    }

    override fun cardActionlActionAnalysis() {
        TODO("Not yet implemented")
    }

}