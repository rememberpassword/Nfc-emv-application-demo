package com.rmp.emvengine.data

enum class EmvTags(val value: Long) {
    //define tag for emv engine module
    EMV_CL_FLOOR_LIMIT(0xDF8101L), // cless floor limit
    EMV_CL_CVM_LIMIT(0xDF8102L), //cless cvm limit
    EMV_CL_TRANS_LIMIT(0xDF8103L),//cless transaction limit


}