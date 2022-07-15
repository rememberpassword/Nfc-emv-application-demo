package com.rmp.emvengine.data

enum class EmvError(val code: Int){
//    SUCCESS(0),
    OTHER_ERROR(1),
    COMMUNICATE_ERROR(2),
    NO_APPLICATION(3)

}
