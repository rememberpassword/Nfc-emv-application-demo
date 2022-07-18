package com.rmp.emvnfcdemo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.rmp.emvnfcdemo.R
import com.rmp.emvnfcdemo.ui.UiAction
import java.util.*

class WarningFragment(private val title: String, private val timeout: Long,    private val cb: (UiAction) -> Unit) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_warning, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.tv_warning).text = title

        val timer = Timer()
        val timerTask = object : TimerTask() {
            override fun run() {
                cb.invoke(UiAction.TIMEOUT)
            }
        }
        timer.schedule(timerTask, timeout)
    }
}