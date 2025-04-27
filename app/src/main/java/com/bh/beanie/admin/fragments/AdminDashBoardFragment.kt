package com.bh.beanie.admin.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bh.beanie.R
import com.bh.beanie.model.Order
import com.bh.beanie.repository.FirebaseRepository
import com.bh.beanie.utils.NavigationUtils
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.*

class AdminDashBoardFragment : Fragment() {
    private lateinit var barChart: BarChart
    private lateinit var textDay: TextView
    private lateinit var textMonth: TextView
    private lateinit var textYear: TextView
    private lateinit var spinnerMonth: Spinner
    private lateinit var spinnerYear: Spinner
    private val firebaseRepository = FirebaseRepository(FirebaseFirestore.getInstance())
    private var selectedMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var selectedYear = Calendar.getInstance().get(Calendar.YEAR)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_admin_dash_board, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textUserName = view.findViewById<TextView>(R.id.textUserName)
        val btnLogout = view.findViewById<ImageButton>(R.id.imageBtnLogOut)
        val numberOfOrderToday = view.findViewById<TextView>(R.id.textNumberOfOrder)
        val nameAdmin: String = arguments?.getString("nameAdmin") ?: "Admin"
        val branchId = arguments?.getString("branchId") ?: "branches_q5"

        barChart = view.findViewById(R.id.barChart)
        textDay = view.findViewById(R.id.textDay)
        textMonth = view.findViewById(R.id.textMonth)
        textYear = view.findViewById(R.id.textYear)
        spinnerMonth = view.findViewById(R.id.spinnerMonth)
        spinnerYear = view.findViewById(R.id.spinnerYear)


        textUserName.text = "Hi $nameAdmin!"

        // Set up spinner for months
        val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonth.adapter = monthAdapter
        spinnerMonth.setSelection(selectedMonth - 1) // Set the default month to current month

        // Set up spinner for years
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear - 5..currentYear).toList() // Show the last 10 years
        val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerYear.adapter = yearAdapter
        spinnerYear.setSelection(years.indexOf(selectedYear)) // Set the default year to current year

        // Handle month and year selection
        spinnerMonth.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedMonth = position + 1
                lifecycleScope.launch {
                    val counts = countOrdersByWeekdayInMonthFromFirestore(selectedMonth, selectedYear, branchId)
                    updateChart(counts) // Pass the counts to updateChart
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {}
        })

        spinnerYear.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedYear = years[position]
                lifecycleScope.launch {
                    val counts = countOrdersByWeekdayInMonthFromFirestore(selectedMonth, selectedYear, branchId)
                    updateChart(counts) // Pass the counts to updateChart
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {}
        })

        val today = Date() // Ngày hôm nay
        lifecycleScope.launch {
            val countTodayOrders = firebaseRepository.countOrdersInDay(today, branchId)
            numberOfOrderToday.text = countTodayOrders.toString()

            // Lấy số lượng đơn hàng theo ngày trong tuần từ Firestore
            val counts = countOrdersByWeekdayInMonthFromFirestore(selectedMonth, selectedYear, branchId)
            updateChart(counts)  // Cập nhật biểu đồ với dữ liệu đếm
        }

        btnLogout.setOnClickListener {
            NavigationUtils.logout(requireActivity())
        }

        setupChart()
        updateDate()
    }

    private fun setupChart() {
        barChart.setBackgroundColor(Color.WHITE)
        barChart.axisRight.setDrawLabels(false)

        // Tạo dữ liệu mẫu cho biểu đồ
        val entries = arrayListOf(
            BarEntry(0f, 45f),
            BarEntry(1f, 30f),
            BarEntry(2f, 60f),
            BarEntry(3f, 10f),
            BarEntry(4f, 100f),
            BarEntry(5f, 90f),
            BarEntry(6f, 800f),
        )

        val xValues = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        val yAxis: YAxis = barChart.axisLeft
        yAxis.axisMinimum = 0f
        yAxis.axisMaximum = entries.maxOf { it.y } + 50f
        yAxis.axisLineColor = Color.GRAY
        yAxis.setLabelCount(10, true)

        barChart.axisRight.isEnabled = false

        val dataSet = BarDataSet(entries, "Sales")
        dataSet.color = Color.parseColor("#90CAF9")
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

    private fun updateChart(counts: Map<String, Int>) {
        val entries = arrayListOf<BarEntry>()

        // Duyệt qua các ngày trong tuần để tạo BarEntry cho biểu đồ
        val weekdays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        weekdays.forEachIndexed { index, day ->
            val count = counts[day] ?: 0  // Lấy số lượng đơn hàng cho ngày đó
            entries.add(BarEntry(index.toFloat(), count.toFloat()))
        }

        // Tạo dữ liệu cho BarChart
        val dataSet = BarDataSet(entries, "Orders by Weekday")
        dataSet.color = Color.parseColor("#90CAF9")

        val yAxis: YAxis = barChart.axisLeft
        yAxis.axisMaximum = entries.maxOf { it.y } + 2f

        val barData = BarData(dataSet)
        barChart.data = barData

        // Cập nhật biểu đồ
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

    // Hàm đếm số lượng đơn hàng theo ngày trong tuần cho tháng, năm đã cho
    fun countOrdersByWeekdayInMonth(orders: List<Order>, month: Int, year: Int): Map<String, Int> {
        val counts = mutableMapOf<String, Int>().apply {
            this["Monday"] = 0
            this["Tuesday"] = 0
            this["Wednesday"] = 0
            this["Thursday"] = 0
            this["Friday"] = 0
            this["Saturday"] = 0
            this["Sunday"] = 0
        }

        // Tính ngày đầu tháng và cuối tháng
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1)
        val startDate = calendar.time

        calendar.set(year, month - 1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))  // Cuối tháng
        val endDate = calendar.time

        // Duyệt qua tất cả các đơn hàng
        for (order in orders) {
            val orderDate = order.getOrderDate()

            // Nếu đơn hàng nằm trong tháng cần đếm
            if (orderDate.after(startDate) && orderDate.before(endDate)) {
                val dayOfWeek = getDayOfWeek(orderDate)  // Lấy ngày trong tuần của đơn hàng

                // Cập nhật số lượng đơn hàng cho ngày đó
                counts[dayOfWeek] = counts[dayOfWeek]!! + 1
            }
        }

        return counts
    }

    // Hàm lấy tên ngày trong tuần
    fun getDayOfWeek(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> "Unknown"
        }
    }

    suspend fun countOrdersByWeekdayInMonthFromFirestore(month: Int, year: Int, branchId: String): Map<String, Int> {
        val orders = firebaseRepository.getOrdersForMonth(month, year, branchId)
        return countOrdersByWeekdayInMonth(orders, month, year)
    }
}
