package com.bh.beanie.adapter

import android.graphics.Color
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
    private var orderList: MutableList<Order>,
    private val onConfirmClick: (Order) -> Unit,
    private val onCancelClick: (Order) -> Unit,
    private val onCompleteClick: (Order) -> Unit,
    private val onItemClick: (Order) -> Unit
) : RecyclerView.Adapter<AdminOrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val listProducts: TextView = view.findViewById(R.id.listProducts)
        private val customerName: TextView = view.findViewById(R.id.contentOrder)
        private val orderTime: TextView = view.findViewById(R.id.textTime)
        private val orderStatus: TextView = view.findViewById(R.id.stateOrder)
        private val btnConfirm: Button = view.findViewById(R.id.btnConfirm)
        private val btnCancel: Button = view.findViewById(R.id.btnCancel)
        private val btnCompleted: Button = view.findViewById(R.id.btnComplted)

        fun bind(order: Order) {
            val format = SimpleDateFormat("dd/MM/yy - HH:mm:ss", Locale.getDefault())
            val formattedDate = format.format(order.orderTime.toDate())

            val productNames = order.items.joinToString(", ") { it.productName }
            listProducts.text = productNames

            customerName.text = order.customerName
            orderTime.text = formattedDate
            orderStatus.text = order.status

            orderStatus.setTextColor(
                when (order.status) {
                    "WAITING ACCEPT" -> android.graphics.Color.GRAY
                    "READY FOR PICKUP" -> android.graphics.Color.BLUE
                    "DELIVERING" -> android.graphics.Color.BLUE
                    "CONFIRMED" -> Color.parseColor("#4CAF50")
                    "PENDING" -> Color.parseColor("#FBC02D")
                    "CANCELED" -> android.graphics.Color.RED
                    else -> android.graphics.Color.GRAY
                }
            )

            btnConfirm.visibility = if (order.status == "WAITING ACCEPT") View.VISIBLE else View.GONE
            btnCancel.visibility = if (order.status == "WAITING ACCEPT") View.VISIBLE else View.GONE
            btnCompleted.visibility = if (order.status == "PENDING") View.VISIBLE else View.GONE


            btnConfirm.setOnClickListener { onConfirmClick(order) }
            btnCancel.setOnClickListener { onCancelClick(order) }
            btnCompleted.setOnClickListener { onCompleteClick(order) }

            itemView.setOnClickListener { onItemClick(order) }
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

    fun updateOrders(newOrderList: List<Order>) {
        orderList = newOrderList.toMutableList()
        notifyDataSetChanged()
    }

    fun appendOrders(newOrders: List<Order>) {
        val startPosition = orderList.size
        orderList.addAll(newOrders)
        notifyItemRangeInserted(startPosition, newOrders.size)
    }

    fun clearOrders() {
        orderList.clear()
        notifyDataSetChanged()
    }
}
