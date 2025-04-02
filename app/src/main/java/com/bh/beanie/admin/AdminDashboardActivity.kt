package com.bh.beanie.admin

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

class AdminDashboardActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val barChart = findViewById<BarChart>(R.id.barChart)

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

        updateChart(barChart, entries)
        updateDate(this)

    }

    private fun updateChart(barChart: BarChart, entries: List<BarEntry>) {
        val xValues = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        val yAxis: YAxis = barChart.axisLeft
        yAxis.axisMinimum = 0f
        yAxis.axisMaximum = entries.maxOf { it.y } + 50f
        yAxis.axisLineColor = Color.BLACK
        yAxis.setLabelCount(10, true)

        barChart.axisRight.isEnabled = false

        val dataSet = BarDataSet(entries, "Subjects")
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

    fun updateDate(context: AppCompatActivity) {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        val year = calendar.get(Calendar.YEAR)

        context.findViewById<TextView>(R.id.textDay).text = day.toString()
        context.findViewById<TextView>(R.id.textMonth).text = month
        context.findViewById<TextView>(R.id.textYear).text = year.toString()
    }
}