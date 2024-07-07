package com.example.dreamcatch.ui.notifications

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.dreamcatch.R
import com.example.dreamcatch.databinding.FragmentNotificationsBinding
import com.example.dreamcatch.network.analytics
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.gson.Gson
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale


class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private lateinit var pieChart: PieChart
    private lateinit var pieChart2: PieChart
    private lateinit var pieChart3: PieChart
    private lateinit var pieChart4: PieChart
    private lateinit var donutChart: PieChart
    private lateinit var donutChart2: PieChart
    private lateinit var asleepfor: TextView
    private lateinit var sleptat: TextView
    private lateinit var wokeupat: TextView
    private lateinit var temperaturefeedback: TextView
    private lateinit var date: TextView
    private lateinit var temperature: TextView

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        pieChart = root.findViewById(R.id.pieChart)
        pieChart2 = root.findViewById(R.id.pieChart2)
        pieChart3 = root.findViewById(R.id.pieChart3)
        pieChart4 = root.findViewById(R.id.pieChart4)
        donutChart = root.findViewById(R.id.donutChart)
        donutChart2 = root.findViewById(R.id.donutChart2)
        asleepfor = root.findViewById(R.id.textView5)
        sleptat = root.findViewById(R.id.textView7)
        wokeupat = root.findViewById(R.id.textView9)
        temperaturefeedback = root.findViewById(R.id.textView15)
        temperature = root.findViewById(R.id.textView30)

        val analyticsData = getAnalytics()
        val currentDate = LocalDate.now()

        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)
        val formattedDate = currentDate.format(formatter)

        date = root.findViewById(R.id.textView20)
        date.text = formattedDate

        if (analyticsData != null) {
            Log.d("Analytics", "Sleep Score: ${analyticsData.sleepscore}")
            Log.d("Analytics", "Slept At: ${analyticsData.sleptat}")
            Log.d("Analytics", "Woke Up At: ${analyticsData.wokeupat}")
            Log.d("Analytics", "Asleep For: ${analyticsData.asleepfor}")
            Log.d("Analytics", "REM: ${analyticsData.rem}")
            Log.d("Analytics", "Temperature: ${analyticsData.temperature}")
            Log.d("Analytics", "Temperature Feedback: ${analyticsData.temperaturefeedback}")
            Log.d("Analytics", "Deep Sleep: ${analyticsData.deepsleep}")
            asleepfor.text = analyticsData.asleepfor
            sleptat.text = analyticsData.sleptat
            wokeupat.text = analyticsData.wokeupat
            temperaturefeedback.text = analyticsData.temperaturefeedback
            temperature.text = analyticsData.temperature

            val sleepscore = analyticsData.sleepscore
            val asleepfor = analyticsData.asleepfor
            val rem = analyticsData.rem
            val deepsleep = analyticsData.deepsleep
            val temperature = analyticsData.temperature

            createPieChart(pieChart, listOf(asleepfor.toFloat(), 10f-asleepfor.toFloat()))
            createPieChart(pieChart2, listOf(rem.toFloat(), asleepfor.toFloat()-rem.toFloat()))
            createPieChart(pieChart3, listOf(deepsleep.toFloat(), asleepfor.toFloat()-deepsleep.toFloat()))
            createPieChart(pieChart4, listOf(asleepfor.toFloat()-deepsleep.toFloat(), deepsleep.toFloat()))
            createDonutChart(donutChart, listOf(sleepscore.toFloat(), 100f-sleepscore.toFloat()))
            createDonutChart(donutChart2, listOf(temperature.toFloat(), 100f-sleepscore.toFloat()))

        }

        return root
    }

    private fun createPieChart(pieChart: PieChart, floatList: List<Float>) {
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(floatList[0], ""))
        entries.add(PieEntry(floatList[1], ""))
        pieChart.isDrawHoleEnabled = false


        val dataSet = PieDataSet(entries, "Deep Sleep")

        dataSet.colors = listOf(
            Color.parseColor("#FF9CACDC"),
            Color.parseColor("#205D74B3")
        )
        dataSet.setDrawValues(false)

        val data = PieData(dataSet)
        pieChart.description = null
        pieChart.legend.isEnabled = false
        pieChart.data = data

        pieChart.invalidate()
    }

    private fun createDonutChart(pieChart: PieChart, floatList: List<Float>) {
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(floatList[0], ""))
        entries.add(PieEntry(floatList[1], ""))
        pieChart.isDrawHoleEnabled = true

        pieChart.setHoleColor(Color.parseColor("#00FFFFFF"))
        pieChart.setHoleRadius(70f);

        val dataSet = PieDataSet(entries, "Deep Sleep")

        dataSet.colors = listOf(
            Color.rgb(156, 173, 220),
            Color.rgb(93, 116, 179),
        )
        dataSet.setDrawValues(false)

        val data = PieData(dataSet)
        pieChart.description = null
        pieChart.legend.isEnabled = false
        pieChart.data = data

        pieChart.invalidate()
    }

    private fun getAnalytics(): analytics? {
        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val analyticsJson = sharedPreferences.getString("analytics", null)
        return Gson().fromJson(analyticsJson, analytics::class.java)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}