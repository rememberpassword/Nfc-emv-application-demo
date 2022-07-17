package com.rmp.emvnfcdemo.data

enum class Currency (
    val label: String,
    val symbol: String,
    val code: Int,
    val exponent: Int
){
    USD("USD","$",840,2),
    EURO("EURO","€",978,2),
}
