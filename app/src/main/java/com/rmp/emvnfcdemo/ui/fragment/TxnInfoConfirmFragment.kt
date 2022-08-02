package com.rmp.emvnfcdemo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.rmp.emvnfcdemo.R
import com.rmp.emvnfcdemo.ui.UiAction

class TxnInfoConfirmFragment(
    private val title: String,
    private val amount: String,
    private val pan: String,
    private val expDate: String,
    private val txnDate: String,
    private val txnTime: String,
    private val aid: String,
    private val appName: String,
    private val cb: (UiAction) -> Unit
) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transaction_info_confirm, container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.tv_title).text = title
        view.findViewById<TextView>(R.id.tv_amount).text = amount
        view.findViewById<TextView>(R.id.tv_pan).text = pan
        view.findViewById<TextView>(R.id.tv_exp_date).text = expDate
        view.findViewById<TextView>(R.id.tv_txn_date).text = txnDate
        view.findViewById<TextView>(R.id.tv_txn_time).text = txnTime
        view.findViewById<TextView>(R.id.tv_aid).text = aid
        view.findViewById<TextView>(R.id.tv_app_name).text = appName

        view.findViewById<Button>(R.id.btn_confirm).setOnClickListener {
            cb.invoke(UiAction.CONFIRM)
        }
        view.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            cb.invoke(UiAction.CANCEL)
        }
    }

}