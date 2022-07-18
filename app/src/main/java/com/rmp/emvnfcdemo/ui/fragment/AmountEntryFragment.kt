package com.rmp.emvnfcdemo.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.rmp.emvnfcdemo.R
import com.rmp.emvnfcdemo.data.Amount
import com.rmp.emvnfcdemo.data.Currency
import android.widget.AdapterView

import android.widget.ArrayAdapter
import android.widget.Spinner


class AmountEntryFragment(
    private val cb: (Amount) -> Unit,
    private val transactionType: String,
    private val currencies: List<Currency>
) : Fragment() {

    private var amount = ""
    private var currencySelected: Currency? = null

    private var tvAmount : TextView? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_amount_entry,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvAmount = view.findViewById(R.id.tv_amount)
        view.findViewById<TextView>(R.id.tv_transaction_type).text = transactionType

        val adapter: ArrayAdapter<Currency> = ArrayAdapter<Currency>(
            this.requireContext(),
            android.R.layout.simple_spinner_item,
            currencies
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        view.findViewById<Spinner>(R.id.sp_currency).apply {
            setAdapter(adapter)
            setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    currencySelected = currencies[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            })
        }



        view.findViewById<TextView>(R.id.btn_confirm).setOnClickListener {
            currencySelected?.let {
                cb.invoke(Amount(amount.toLongOrNull() ?: 0,it))
            }
        }
        view.findViewById<TextView>(R.id.btn_clear).setOnClickListener {
            amount = ""
            updateUI()
        }
        val numberListener = View.OnClickListener {
            when(it.id){
                R.id.btn0 -> if(amount.isNotEmpty()) amount += "0"
                R.id.btn1 -> amount += "1"
                R.id.btn2 -> amount += "2"
                R.id.btn3 -> amount += "3"
                R.id.btn4 -> amount += "4"
                R.id.btn5 -> amount += "5"
                R.id.btn6 -> amount += "6"
                R.id.btn7 -> amount += "7"
                R.id.btn8 -> amount += "8"
                R.id.btn9 -> amount += "9"
            }
            updateUI()
        }
        view.findViewById<TextView>(R.id.btn0).setOnClickListener(numberListener)
        view.findViewById<TextView>(R.id.btn1).setOnClickListener(numberListener)
        view.findViewById<TextView>(R.id.btn2).setOnClickListener(numberListener)
        view.findViewById<TextView>(R.id.btn3).setOnClickListener(numberListener)
        view.findViewById<TextView>(R.id.btn4).setOnClickListener(numberListener)
        view.findViewById<TextView>(R.id.btn5).setOnClickListener(numberListener)
        view.findViewById<TextView>(R.id.btn6).setOnClickListener(numberListener)
        view.findViewById<TextView>(R.id.btn7).setOnClickListener(numberListener)
        view.findViewById<TextView>(R.id.btn8).setOnClickListener(numberListener)
        view.findViewById<TextView>(R.id.btn9).setOnClickListener(numberListener)
    }

    private fun updateUI() {
        tvAmount?.text = amount.toDisplayAmount(currencySelected)
    }
}

private fun String.toDisplayAmount(currencySelected: Currency?): String {
    if (currencySelected == null) return this
    //0 -> 0.00
    //12 -> 0.12
    var result = this
    if (this.length <= 3) {
        result = result.padStart(3, '0')
    }
    // add '.'
    return result.substring(0, result.length - 2) + "." + result.substring(result.length - 2)
}
