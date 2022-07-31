package com.rmp.emvengine.data

internal class TransactionData {

    val terminalData: MutableMap<Long,TlvObject> = mutableMapOf()
    val cardData: MutableMap<Long,TlvObject> = mutableMapOf()
    val cardAppData: MutableList<Aid> = mutableListOf()
    var transactionDecision: TransactionDecision = TransactionDecision.AAC
    var cvm : CvmMethod? = null
    var kernelId: KernelId? = null
    var recordOda: ByteArray = byteArrayOf()
    var isOdaSuccess = false
    var capk: Capk? = null
    var odaType: OdaType? = null



    fun getData(tag: Long): ByteArray?{
        return terminalData[tag]?.value ?: cardData[tag]?.value
    }
    fun getData(tag: String) = getData(tag.toLong(16))

}