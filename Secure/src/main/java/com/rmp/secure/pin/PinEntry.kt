package com.rmp.secure.pin

import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.rmp.secure.R
import com.rmp.secure.common.hexToByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.Exception

class PinEntry(private val context: Context) {

    private var pinEntryCloser: (() -> Unit)? = null

    suspend fun showPinEntry(
        pan: String,
        bypassAllows: Boolean = true,
        random: Boolean = true
    ): PinEntryResult {

        val channel = Channel<PinEntryResult>()
        val windowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
        val view = View.inflate(context, R.layout.pin_entry_layout, null)

        val listPad = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9").apply {
            if (random) shuffled()
        }

        var pinCode = ""
        val pinLabel = view.findViewById<TextView>(R.id.pin_label)

        val pinEntryListener = View.OnClickListener {
            when (it.id) {
                R.id.btn_cancel -> {
                    if (pinCode.isNotEmpty()) {
                        pinCode = ""
                    } else {
                        pinEntryCloser?.invoke()
                    }
                }
                R.id.btn_clear -> {
                    if (pinCode.isNotEmpty()) {
                        pinCode = pinCode.substring(0, pinCode.length - 1)
                    }
                }
                R.id.btn_confirm -> {
                    if (pinCode.isEmpty() && bypassAllows) {
                        channel.trySend(PinEntryResult(pinEntryStatus = PinEntryStatus.BYPASS))
                    } else if (pinCode.length >= 4) {
                        val pinBlock = pinCode.padStart(16, 'F')
                        //TODO("impl encrypt pin code late")
                        channel.trySend(
                            PinEntryResult(
                                pinEntryStatus = PinEntryStatus.PIN_ENTERED,
                                pinBlock = pinBlock.hexToByteArray()
                            )
                        )
                    }

                }
                else -> {
                    pinCode = "${(it as? TextView)?.text}$pinCode"
                }
            }

            pinLabel.text = "".padStart(pinCode.length, '*')
        }
        view.findViewById<TextView>(R.id.btn_0).apply {
            setOnClickListener(pinEntryListener)
            text = listPad[0]
        }
        view.findViewById<TextView>(R.id.btn_1).apply {
            setOnClickListener(pinEntryListener)
            text = listPad[1]
        }
        view.findViewById<TextView>(R.id.btn_2).apply {
            setOnClickListener(pinEntryListener)
            text = listPad[2]
        }
        view.findViewById<TextView>(R.id.btn_3).apply {
            setOnClickListener(pinEntryListener)
            text = listPad[3]
        }
        view.findViewById<TextView>(R.id.btn_4).apply {
            setOnClickListener(pinEntryListener)
            text = listPad[4]
        }
        view.findViewById<TextView>(R.id.btn_5).apply {
            setOnClickListener(pinEntryListener)
            text = listPad[5]
        }
        view.findViewById<TextView>(R.id.btn_6).apply {
            setOnClickListener(pinEntryListener)
            text = listPad[6]
        }
        view.findViewById<TextView>(R.id.btn_7).apply {
            setOnClickListener(pinEntryListener)
            text = listPad[7]
        }
        view.findViewById<TextView>(R.id.btn_8).apply {
            setOnClickListener(pinEntryListener)
            text = listPad[8]
        }
        view.findViewById<TextView>(R.id.btn_9).apply {
            setOnClickListener(pinEntryListener)
            text = listPad[9]
        }
        view.findViewById<TextView>(R.id.btn_cancel)
            .apply { setOnClickListener(pinEntryListener) }

        view.findViewById<TextView>(R.id.btn_clear).apply { setOnClickListener(pinEntryListener) }
        view.findViewById<TextView>(R.id.btn_confirm)
            .apply { setOnClickListener(pinEntryListener) }


        ///////

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        runBlocking(Dispatchers.Main) {
            windowManager.addView(view, params)
        }

        pinEntryCloser = {
            try {
                CoroutineScope(Dispatchers.Main).launch {
                    windowManager.removeView(view)
                }
            } catch (e: Exception) {
                Log.e("SecureEngine", e.toString())
            }
            channel.trySend(PinEntryResult(pinEntryStatus = PinEntryStatus.CANCEL))
        }

        return channel.receiveCatching().getOrNull().also {
            try {
                runBlocking(Dispatchers.Main) {
                    windowManager.removeView(view)
                }
            } catch (e: Exception) {
                Log.e("SecureEngine", e.toString())
            }
        } ?: PinEntryResult(pinEntryStatus = PinEntryStatus.ERROR)

    }

    fun close() {
        Log.d("SecureEngine","#close")
        pinEntryCloser?.invoke()
    }

}

data class PinEntryResult(
    val pinEntryStatus: PinEntryStatus,
    val pinBlock: ByteArray? = null
)

enum class PinEntryStatus {
    ERROR,
    PIN_ENTERED,
    CANCEL,
    BYPASS
}
