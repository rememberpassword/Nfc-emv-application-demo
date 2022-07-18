package com.rmp.emvengine.data

internal class TransactionData {
    val terminalData: MutableMap<Long,TlvObject> = mutableMapOf()
    val cardData: MutableMap<Long,TlvObject> = mutableMapOf()
    val cardAppData: MutableList<Aid> = mutableListOf()
    var transactionDecision: TransactionDecision = TransactionDecision.AAC
    var cvm : CvmMethod? = null
    var kernelId: KernelId? = null
    val recordOda: MutableMap<Long,TlvObject> = mutableMapOf()
    var isFddaSuccess = false
    var capk: Capk? = null



    fun getData(tag: Long): ByteArray?{
        return terminalData[tag]?.value ?: cardData[tag]?.value
    }
    fun getData(tag: String) = getData(tag.toLong(16))

}