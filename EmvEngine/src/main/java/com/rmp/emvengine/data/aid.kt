package com.rmp.emvengine.data

data class Aid(
    val aid: String,
    val label: String?,
    val priority: Int?,
    val isPartial: Boolean?,
    val data: List<TlvObject>?
)

enum class KernelId(val value: Int){
    EMV(0),
    VISA(3),
    MASTER(2),
    AMEX(4),
    DISCOVER(6),
    POBC(7),
    JCB(5)
}