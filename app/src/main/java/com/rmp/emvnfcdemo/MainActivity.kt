package com.rmp.emvnfcdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private var emvProcess: EmvProcess? =null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView = findViewById<TextView>(R.id.tv_transaction)
        findViewById<Button>(R.id.btn_start_txn).setOnClickListener {
            CoroutineScope(Dispatchers.Default).launch {
                val emvProcess = EmvProcess(this@MainActivity) {
                    runBlocking(Dispatchers.Main) {
                        textView.append(it + "\n")
                    }
                }
                emvProcess.execute(2000)
            }


        }
        findViewById<Button>(R.id.btn_stop_txn).setOnClickListener {
            emvProcess?.stopTransaction()
        }
    }
}