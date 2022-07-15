package com.rmp.emvengine.data

data class Aid(
    val aid: String,
    val label: String? = null,
    val priority: Int? = null,
    val isPartial: Boolean? = null,
    val data: List<TlvObject>? = null,
)

enum class KernelId(val value: Int) {
    EMV(0),
    VISA(3),
    MASTER(2),
    AMEX(4),
    DISCOVER(6),
    POBC(7),
    JCB(5)
}