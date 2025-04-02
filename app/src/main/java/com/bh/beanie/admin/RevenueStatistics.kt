package com.bh.beanie.admin

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.widget.TextView
import com.bh.beanie.R

class RevenueStatisticsActivity : AppCompatActivity() {

    private lateinit var tvOrdersToday: TextView
    private lateinit var tvCurrentDate: TextView
    private lateinit var barChartWeekly: BarChart
    private lateinit var lineChartMonthly: LineChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_revenue_statistics)

        tvOrdersToday = findViewById(R.id.tvOrdersToday)
        tvCurrentDate = findViewById(R.id.tvCurrentDate)
        barChartWeekly = findViewById(R.id.barChartWeekly)
        lineChartMonthly = findViewById(R.id.lineChartMonthly)

        // Set data (placeholder - bạn cần thay bằng dữ liệu thật)
        tvOrdersToday.text = "15" // Ví dụ
        tvCurrentDate.text = getCurrentDate()

        setupWeeklyBarChart()
        setupMonthlyLineChart()
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun setupWeeklyBarChart() {
        val entries = ArrayList<BarEntry>()
        // Dữ liệu mẫu (7 ngày trong tuần) - Thay bằng dữ liệu thật
        entries.add(BarEntry(0f, 10f)) // Thứ 2
        entries.add(BarEntry(1f, 14f)) // Thứ 3
        entries.add(BarEntry(2f, 8f))  // Thứ 4
        entries.add(BarEntry(3f, 20f)) // Thứ 5
        entries.add(BarEntry(4f, 16f)) // Thứ 6
        entries.add(BarEntry(5f, 5f))  // Thứ 7
        entries.add(BarEntry(6f, 12f)) // Chủ nhật

        val dataSet = BarDataSet(entries, "Orders")
        dataSet.color = Color.BLUE // Màu cột

        val barData = BarData(dataSet)
        barChartWeekly.data = barData

        // Cấu hình trục X
        val xAxis = barChartWeekly.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"))
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false) // Ẩn đường lưới dọc


        // Cấu hình khác
        barChartWeekly.description.isEnabled = false // Ẩn description
        barChartWeekly.legend.isEnabled = false       // Ẩn chú thích
        barChartWeekly.setDrawGridBackground(false)  // Ẩn background grid
        barChartWeekly.invalidate() // Cập nhật biểu đồ
    }

    private fun setupMonthlyLineChart() {
        val entries = ArrayList<Entry>()
        // Dữ liệu mẫu (4 tuần) - Thay bằng dữ liệu thật
        entries.add(Entry(0f, 50f)) // Tuần 1
        entries.add(Entry(1f, 70f)) // Tuần 2
        entries.add(Entry(2f, 60f)) // Tuần 3
        entries.add(Entry(3f, 80f)) // Tuần 4

        val dataSet = LineDataSet(entries, "Revenue")
        dataSet.color = Color.RED
        dataSet.valueTextColor = Color.BLACK
        dataSet.lineWidth = 2f
        //dataSet.setDrawFilled(true) // Fill color below the line
        //dataSet.fillColor = Color.RED

        val lineData = LineData(dataSet)
        lineChartMonthly.data = lineData

        // Cấu hình trục X
        val xAxis = lineChartMonthly.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(arrayOf("Week 1", "Week 2", "Week 3", "Week 4"))
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false) //Ẩn đường lưới dọc

        // Cấu hình khác
        lineChartMonthly.description.isEnabled = false
        lineChartMonthly.legend.isEnabled = false
        lineChartMonthly.setDrawGridBackground(false) // Ẩn background grid
        lineChartMonthly.invalidate()
    }
}