package com.example.dreamcatch.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.dreamcatch.DonutSelectorView
import com.example.dreamcatch.Login
import com.example.dreamcatch.MainActivity
import com.example.dreamcatch.OnHourChangeListener
import com.example.dreamcatch.R
import com.example.dreamcatch.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val startHourTextView = binding.textView5
        val endHourTextView = binding.textView6

        val donutSelectorView = binding.donutSelectorView
        donutSelectorView.setOnHourChangeListener(object : OnHourChangeListener {
            override fun onHourChanged(startHour: Int, endHour: Int) {
                // Handle hour changes here
                Log.d("HourChange", "Start Hour: $startHour, End Hour: $endHour")
                startHourTextView.text = "Start Hour: $startHour"
                endHourTextView.text = "End Hour: $endHour"
            }

        })
        val startHour = donutSelectorView.getStartHour()
        val endHour = donutSelectorView.getEndHour()


        val button = binding.button
        button.setOnClickListener {
            navigateToLoginActivity()
        }
        return root
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(requireContext(), Login::class.java)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}