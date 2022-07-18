package com.rmp.emvengine.data

import android.util.Log

enum class EmvError(val code: Int){
//    SUCCESS(0),
    OTHER_ERROR(1),
    COMMUNICATE_ERROR(2),
    NO_APPLICATION(3),
    TRY_OTHER_INTERFACE(4),
    TXN_EXCEED_LIMIT(5),
    MISSING_TAG(5),
    PRESENT_CARD_AGAIN(6),
    KERNEL_ABSENT(7)

}

 fun String.toEmvError(): EmvError{
//    Log.d("EmvError","error code:$this")
    val level = this.substring(0,2).toEmvErrorLevel()
    val error = this.substring(2).toEmvCoreError()
    return when(error){
        EmvCoreError.MISSING_TAG,
        EmvCoreError.NULL_PDOL_DATA -> EmvError.MISSING_TAG
        EmvCoreError.TXN_EXCEED_LIMIT -> EmvError.TXN_EXCEED_LIMIT
        EmvCoreError.NULL_PDOL,
        EmvCoreError.APDU_RESPONSE_WRONG_FORMAT,
        EmvCoreError.DUPLICATE_CARD_DATA,
        EmvCoreError.TRY_OTHER_INTERFACE -> EmvError.TRY_OTHER_INTERFACE
        EmvCoreError.APDU_ERROR,
        EmvCoreError.NOT_RECEIVE_APDU-> EmvError.PRESENT_CARD_AGAIN
        EmvCoreError.KERNEL_ABSENT -> EmvError.KERNEL_ABSENT
    }
}
