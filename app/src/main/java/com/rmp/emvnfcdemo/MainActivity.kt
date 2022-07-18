package com.rmp.emvnfcdemo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.rmp.emvengine.data.TransactionDecision
import com.rmp.emvnfcdemo.data.Amount
import com.rmp.emvnfcdemo.data.Currency
import com.rmp.emvnfcdemo.data.TransactionType
import com.rmp.emvnfcdemo.transaction.EmvProcess
import com.rmp.emvnfcdemo.ui.UiController
import com.rmp.emvnfcdemo.ui.UiControllerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var emvProcess: EmvProcess? = null
    private val defaultJob = SupervisorJob()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        CoroutineScope(Dispatchers.Default+defaultJob).launch {
            emvProcess = EmvProcess(this@MainActivity,UiControllerImpl(this@MainActivity,R.id.main_content))
            executeTransaction()
        }
    }

    private suspend fun executeTransaction() {
        emvProcess?.execute()
        executeTransaction()
    }

    override fun onStop() {
        super.onStop()
        emvProcess?.stopTransaction()
        defaultJob.cancelChildren()
    }

    override fun onStart() {
        super.onStart()
        CoroutineScope(Dispatchers.Default+defaultJob).launch {
            emvProcess = EmvProcess(this@MainActivity,UiControllerImpl(this@MainActivity,R.id.main_content))
            executeTransaction()
        }
    }


}