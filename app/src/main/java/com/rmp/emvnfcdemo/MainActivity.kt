package com.rmp.emvnfcdemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rmp.emvnfcdemo.transaction.EmvProcess
import com.rmp.emvnfcdemo.ui.UiControllerImpl
import com.rmp.secure.SecureEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var emvProcess: EmvProcess? = null
    private val defaultJob = SupervisorJob()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        CoroutineScope(Dispatchers.Default+defaultJob).launch {
            emvProcess = EmvProcess(this@MainActivity,UiControllerImpl(this@MainActivity,R.id.main_content), SecureEngine(this@MainActivity))
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
            emvProcess = EmvProcess(this@MainActivity,UiControllerImpl(this@MainActivity,R.id.main_content), SecureEngine(this@MainActivity))
            executeTransaction()
        }
    }


}