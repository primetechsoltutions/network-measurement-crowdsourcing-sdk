package com.ptsl.networksdk_event.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.ptsl.networksdk_event.R

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        view.findViewById<Button>(R.id.start_assessment_btn).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, NetworkAssessmentFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }
}
