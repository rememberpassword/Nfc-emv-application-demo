package com.example.nfcdemo

import android.annotation.SuppressLint
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        GlobalScope.launch {
            val nfcAdapter = NfcAdapter.getDefaultAdapter(this@MainActivity)

            val opts = Bundle()
            val detectTime = 5000
            opts.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, detectTime)

            var flags = NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
            flags = flags or NfcAdapter.FLAG_READER_NFC_A
            flags = flags or NfcAdapter.FLAG_READER_NFC_B
            flags = flags or NfcAdapter.FLAG_READER_NFC_F
            flags = flags or NfcAdapter.FLAG_READER_NFC_V
            Log.d("vinh","enableReaderMode= ${nfcAdapter.isEnabled}")
            nfcAdapter.enableReaderMode(this@MainActivity,object : NfcAdapter.ReaderCallback{
                @SuppressLint("NewApi")
                override fun onTagDiscovered(tag: Tag?) {

                    Log.d("vinh","tag = $tag")
                    GlobalScope.launch {
                        val isoDep = IsoDep.get(tag)
                        try {
                        isoDep.connect()
                        val response =
                            isoDep.transceive(BytesUtil.hexString2Bytes("00A404000e325041592e5359532e444446303100"))
                        Log.d("vinh", "reponse ${BytesUtil.bytes2HexString(response)}")


                        delay(2000)
                        val response1 =
                            isoDep.transceive(BytesUtil.hexString2Bytes("00A404000e325041592e5359532e444446303100"))
                        Log.d("vinh", "reponse ${BytesUtil.bytes2HexString(response1)}")
                    }catch (tagLost: TagLostException){
                            isoDep.close()
                    }
                    }

                }

            },flags,opts)
            Log.d("vinh","enableReaderMode= ${nfcAdapter.isEnabled}")
        }


    }
}