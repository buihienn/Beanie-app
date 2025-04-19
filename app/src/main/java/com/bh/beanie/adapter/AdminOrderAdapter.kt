package com.bh.beanie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.model.Order
import java.text.SimpleDateFormat
import java.util.Locale


class AdminOrderAdapter(
    private var orderList: List<Order>, // Thay đổi sang var để có thể cập nhật
    private val onConfirmClick: (Order) -> Unit,
    private val onCancelClick: (Order) -> Unit,
    private val onItemClick: (Order) -> Unit
) : RecyclerView.Adapter<AdminOrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val listProducts: TextView = view.findViewById(R.id.listProducts)
        val customerName: TextView = view.findViewById(R.id.contentOrder)
        val orderTime: TextView = view.findViewById(R.id.textTime)
        val orderStatus: TextView = view.findViewById(R.id.stateOrder)
        val btnConfirm: Button = view.findViewById(R.id.btnConfirm)
        val btnCancel: Button = view.findViewById(R.id.btnCancel)

        fun bind(order: Order) {
            // Concatenate product names
            val format = SimpleDateFormat("dd/MM/yy - HH:mm:ss", Locale.getDefault())
            val formattedDate = format.format(order.orderTime.toDate())

            val productNames = order.items.joinToString(", ") { it.productName }
            listProducts.text = productNames

            customerName.text = order.customerName
            orderTime.text = order.orderTime.toDate().toString()
            orderStatus.text = order.status

            orderTime.text = formattedDate

            orderStatus.setTextColor(
                when (order.status) {
                    "CONFIRMED" -> android.graphics.Color.GREEN
                    "PENDING" -> android.graphics.Color.YELLOW
                    "CANCELLED" -> android.graphics.Color.RED
                    else -> android.graphics.Color.GRAY
                }
            )

            btnConfirm.setOnClickListener { onConfirmClick(order) }
            btnCancel.setOnClickListener { onCancelClick(order) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_admin, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orderList[position])
    }

    override fun getItemCount(): Int = orderList.size

    // Thêm phương thức để cập nhật danh sách đơn hàng
    fun updateOrders(newOrderList: List<Order>) {
        orderList = newOrderList
        notifyDataSetChanged()
    }
}
