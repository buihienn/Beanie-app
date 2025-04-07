package com.bh.beanie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.model.AdminVoucher

class AdminVoucherAdapter(private val voucherList: List<AdminVoucher>) :
    RecyclerView.Adapter<AdminVoucherAdapter.VoucherViewHolder>() {

    class VoucherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView4)
        val nameVoucher: TextView = itemView.findViewById(R.id.nameVoucher)
        val contentVoucher: TextView = itemView.findViewById(R.id.contentVoucher)
        val expiryDateText: TextView = itemView.findViewById(R.id.textTime)
        val stateVoucher: TextView = itemView.findViewById(R.id.stateVoucher)

        fun bind(voucher: AdminVoucher) {
            nameVoucher.text = voucher.name
            contentVoucher.text = voucher.content
            expiryDateText.text = "Han: ${voucher.expiryDate}"
            stateVoucher.text = voucher.state
            // Tải hình ảnh cho ImageView nếu cần, ví dụ sử dụng Glide hoặc Picasso
            // Glide.with(itemView.context).load(voucher.imageUrl).into(imageView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoucherViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_voucher_admin, parent, false)
        return VoucherViewHolder(view)
    }

    override fun onBindViewHolder(holder: VoucherViewHolder, position: Int) {
        val voucher = voucherList[position]
        holder.bind(voucher)
    }

    override fun getItemCount(): Int {
        return voucherList.size
    }
}