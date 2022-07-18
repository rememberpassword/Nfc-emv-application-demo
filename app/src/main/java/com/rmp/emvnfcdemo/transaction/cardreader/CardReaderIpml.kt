package com.rmp.emvnfcdemo.transaction.cardreader

import android.annotation.SuppressLint
import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import com.rmp.emvengine.CardReader
import com.rmp.emvengine.CardReaderError
import com.rmp.emvengine.TransmitResult
import com.rmp.emvengine.common.toHexString
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class CardReaderIpml(private val activity: Activity) : CardReader {

    private val defaultJob = SupervisorJob()
    private val ioScope = CoroutineScope(Dispatchers.IO + defaultJob)
    private val TAG = "CardReaderIpml"
    private var isoDep: IsoDep? = null
    private var _isCardIn = false
    var channel = Channel<Boolean>()
    override suspend fun detectClessCardAndActive(detectTime: Int): Boolean {
        channel = Channel()
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        val opts = Bundle()
        opts.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, detectTime)

        var flags = NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        flags = flags or NfcAdapter.FLAG_READER_NFC_A
        flags = flags or NfcAdapter.FLAG_READER_NFC_B
        flags = flags or NfcAdapter.FLAG_READER_NFC_F
        flags = flags or NfcAdapter.FLAG_READER_NFC_V

        nfcAdapter.enableReaderMode(activity, object : NfcAdapter.ReaderCallback {
            @SuppressLint("NewApi")
            override fun onTagDiscovered(tag: Tag?) {
                Log.d(TAG, "tag = $tag")
                if(!_isCardIn){
                    isoDep = IsoDep.get(tag)
                    isoDep?.connect()
                    _isCardIn = true
                    channel.trySend(true)
                }
            }
        }, flags, opts)
        ioScope.launch {
            delay(detectTime.toLong())
            channel.trySend(false)
        }
        return channel.receiveCatching().getOrNull() ?: false
    }

    override fun close(){
        channel.close()
        isoDep?.close()
        defaultJob.cancelChildren()
        _isCardIn = false
        isoDep = null
    }

    override fun transmitData(data: ByteArray): TransmitResult {
        Log.d(TAG, "#transmitData")
        if (isoDep == null || isoDep?.isConnected == false) {
            return TransmitResult(error = CardReaderError.CANT_ACTIVE, null)
        }
        try {
            Log.d(TAG, "transmit Data = ${data.toHexString()}")
            val response = isoDep?.transceive(data)
            Log.d(TAG, "response Data = ${response?.toHexString()}")
            return TransmitResult(error = null, response)

        } catch (tagLostException: TagLostException) {
            _isCardIn = false
            return TransmitResult(error = CardReaderError.CARD_REMOVED, null)
        }

    }

    override fun isCardRemoved(): Boolean {
        return !_isCardIn
    }
}