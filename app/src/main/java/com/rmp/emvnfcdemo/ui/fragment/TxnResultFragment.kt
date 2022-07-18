package com.rmp.emvnfcdemo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.rmp.emvnfcdemo.R
import com.rmp.emvnfcdemo.ui.UiAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class TxnResultFragment(
    private val isApprovalTxn: Boolean,
    private val timeout: Long,
    private val cb: (UiAction) -> Unit
) :
    Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_txn_result, container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.tv_txn_result).text = if (isApprovalTxn) {
            "Transaction Approval"
        } else {
            "Transaction Decline"
        }
        val img = if (isApprovalTxn) {
            R.drawable.txn_approve
        } else {
            R.drawable.txn_decline
        }
        view.findViewById<ImageView>(R.id.img_txn_result).setImageResource(img)
        val timer = Timer()
        val timerTask = object : TimerTask() {
            override fun run() {
                cb.invoke(UiAction.TIMEOUT)
            }
        }
        timer.schedule(timerTask, timeout)
    }

    override fun onResume() {
        super.onResume()
    }
}