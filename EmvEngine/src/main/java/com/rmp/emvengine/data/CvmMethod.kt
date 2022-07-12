package com.rmp.emvengine.data

enum class CvmMethod {
    NO_CVM,
    ONLINE_PIN,
    OFFLINE_PL_PIN,
    OFFLINE_PL_PIN_SIGN,
    OFFLINE_EN_PIN,
    OFFLINE_EN_PIN_SIGN,
    CDCVM,
    SIGNATURE
}
enum class PinEntryStatus{
    EXC_SUCCESS,
    ERROR,
    CANCEL,
    BYPASS
}