package com.odom.barcodeqr.ui

import android.R.attr.action
import android.R.attr.type
import android.os.Bundle
import android.text.TextUtils.replace
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.navigation.fragment.findNavController
import com.odom.barcodeqr.R

class RadioGroupFragment : Fragment() {

    private var listener: OnRadioSelectedListener? = null

    interface OnRadioSelectedListener {
        fun onRadioSelected(type: String)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_radio_group, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroup)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedType = when (checkedId) {
                R.id.radioText -> "text"
                R.id.radioUrl -> "url"
                R.id.radioEmail -> "email"
                R.id.radioContact -> "contact"
                else -> "text"
            }

            val generateFragment = GenerateFragment.newInstance(selectedType)
            //userInputFragment.arguments = bundle

            val action = RadioGroupFragmentDirections.actionRadioGroupFragmentToGenerateFragment(selectedType)
            findNavController().navigate(action)


        }
    }


}
