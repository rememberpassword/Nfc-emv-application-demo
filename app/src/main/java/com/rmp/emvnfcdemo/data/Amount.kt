package com.rmp.emvnfcdemo.data

data class Amount (
    val value: Long,
    val currency: Currency
) {


    fun toAmountWithCurrency(): String {
        var result = value.toString()
        if (result.length <= 3) {
            result = result.padStart(3, '0')
        }
        // add '.'
        return result.substring(0, result.length - 2) + "." + result.substring(result.length - 2) + currency.symbol
    }
}