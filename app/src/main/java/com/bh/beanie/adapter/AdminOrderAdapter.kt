package com.bh.beanie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.model.AdminOrder

class AdminOrderAdapter(
    private val orderList: List<AdminOrder>,
    private val onConfirmClick: (AdminOrder) -> Unit,
    private val onCancelClick: (AdminOrder) -> Unit
) : RecyclerView.Adapter<AdminOrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val orderId: TextView = view.findViewById(R.id.idOrder)
        val customerName: TextView = view.findViewById(R.id.contentOrder)
        val orderTime: TextView = view.findViewById(R.id.textTime)
        val orderStatus: TextView = view.findViewById(R.id.stateOrder)
        val btnConfirm: Button = view.findViewById(R.id.btnConfirm)
        val btnCancel: Button = view.findViewById(R.id.btnCancel)
        val imgOrder : ImageView = view.findViewById(R.id.imgOrder)

        fun bind(order: AdminOrder) {
            orderId.text = "#${order.id}"
            customerName.text = order.customerName
            orderTime.text = order.orderTime
            orderStatus.text = order.status

            orderStatus.setTextColor(
                when (order.status) {
                    "Done" -> android.graphics.Color.GREEN
                    "Pending" -> android.graphics.Color.YELLOW
                    "Cancelled" -> android.graphics.Color.RED
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
}