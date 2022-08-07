package com.rmp.emvengine.data

internal enum class EmvCoreError(val code: String) {
    MISSING_TAG("01"),
    TXN_EXCEED_LIMIT("02"),
    NULL_PDOL("03"),
    NULL_PDOL_DATA("04"),
    APDU_RESPONSE_WRONG_FORMAT("05"),
    APDU_ERROR("06"),
    DUPLICATE_CARD_DATA("07"),
    TRY_OTHER_INTERFACE("08"),
    NOT_RECEIVE_APDU("09"),
    KERNEL_ABSENT("0A"),
    SELECT_NEXT("0B"),
    NO_APP("0C"),
}
internal fun String.toEmvCoreError(): EmvCoreError = when(this){
    "01" -> EmvCoreError.MISSING_TAG
    "02" -> EmvCoreError.TXN_EXCEED_LIMIT
    "03" -> EmvCoreError.NULL_PDOL
    "04" -> EmvCoreError.NULL_PDOL_DATA
    "05" -> EmvCoreError.APDU_RESPONSE_WRONG_FORMAT
    "06" -> EmvCoreError.APDU_ERROR
    "07" -> EmvCoreError.DUPLICATE_CARD_DATA
    "08" -> EmvCoreError.TRY_OTHER_INTERFACE
    "09" -> EmvCoreError.NOT_RECEIVE_APDU
    "0A" -> EmvCoreError.KERNEL_ABSENT
    "0B" -> EmvCoreError.SELECT_NEXT
    "0C" -> EmvCoreError.NO_APP
    else -> {
        EmvCoreError.MISSING_TAG
    }
}

internal enum class EmvErrorLevel(val code: String){
    APP_SELECTION("01"),
    FINAL_SELECT("02"),
    KERNEL_ACTIVATION("03"),
    PREPROCESSING("04"),
    INITIATE_TXN("05"),
    READ_RECORD("06"),
    ODA_PROCESS("07"),
    RESTRICTION_PROCESS("08"),
    CARDHOLDER_VERIFY("09"),
    TERMINAL_RISK("0A"),
    TERMINAL_ACTION_ANALYSIS("0B"),
    FIRST_GAC("0C"),
    ONLINE_PROCESS("0D"),
    ISSUER_SCRIPT("0E"),
    SECOND_GAC("0F")
}
internal fun String.toEmvErrorLevel() = when(this){
    "01" -> EmvErrorLevel.APP_SELECTION
    "02" -> EmvErrorLevel.FINAL_SELECT
    "03" -> EmvErrorLevel.KERNEL_ACTIVATION
    "04" -> EmvErrorLevel.PREPROCESSING
    "05" -> EmvErrorLevel.INITIATE_TXN
    "06" -> EmvErrorLevel.READ_RECORD
    "07" -> EmvErrorLevel.ODA_PROCESS
    "08" -> EmvErrorLevel.RESTRICTION_PROCESS
    "09" -> EmvErrorLevel.CARDHOLDER_VERIFY
    "0A" -> EmvErrorLevel.TERMINAL_RISK
    "0B" -> EmvErrorLevel.TERMINAL_ACTION_ANALYSIS
    "0C" -> EmvErrorLevel.FIRST_GAC
    "0D" -> EmvErrorLevel.ONLINE_PROCESS
    "0E" -> EmvErrorLevel.ISSUER_SCRIPT
    "0F" -> EmvErrorLevel.SECOND_GAC
    else -> {
        EmvErrorLevel.APP_SELECTION
    }
}