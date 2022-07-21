package com.rmp.secure

import android.content.Context
import com.rmp.secure.pin.PinEntry

class SecureEngine(private val context: Context) {

    fun getPinEntry(): PinEntry{
        return PinEntry(context)
    }
}