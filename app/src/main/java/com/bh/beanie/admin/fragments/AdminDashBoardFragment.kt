package com.bh.beanie.admin.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bh.beanie.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.util.Calendar
import java.util.Locale

class AdminDashBoardFragment : Fragment() {
    private lateinit var barChart: BarChart
    private lateinit var textDay: TextView
    private lateinit var textMonth: TextView
    private lateinit var textYear: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_admin_dash_board, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        barChart = view.findViewById(R.id.barChart)
        textDay = view.findViewById(R.id.textDay)
        textMonth = view.findViewById(R.id.textMonth)
        textYear = view.findViewById(R.id.textYear)

        setupChart()
        updateDate()
    }

    private fun setupChart() {
        barChart.axisRight.setDrawLabels(false)

        val entries = arrayListOf(
            BarEntry(0f, 45f),
            BarEntry(1f, 30f),
            BarEntry(2f, 60f),
            BarEntry(3f, 10f),
            BarEntry(4f, 100f),
            BarEntry(5f, 90f),
            BarEntry(6f, 800f),
        )

        val xValues = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        val yAxis: YAxis = barChart.axisLeft
        yAxis.axisMinimum = 0f
        yAxis.axisMaximum = entries.maxOf { it.y } + 50f
        yAxis.axisLineColor = Color.BLACK
        yAxis.setLabelCount(10, true)

        barChart.axisRight.isEnabled = false

        val dataSet = BarDataSet(entries, "Sales")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        val barData = BarData(dataSet)
        barChart.data = barData

        barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(xValues)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            isGranularityEnabled = true
        }

        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.xAxis.setDrawGridLines(false)
        barChart.axisLeft.setDrawGridLines(false)
        barChart.axisRight.setDrawGridLines(false)
        barChart.invalidate()
    }

    private fun updateDate() {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        val year = calendar.get(Calendar.YEAR)

        textDay.text = day.toString()
        textMonth.text = month
        textYear.text = year.toString()
    }
}
