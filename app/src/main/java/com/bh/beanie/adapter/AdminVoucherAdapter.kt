package com.bh.beanie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.model.Voucher
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class AdminVoucherAdapter(private val voucherList: MutableList<Voucher>) :
    RecyclerView.Adapter<AdminVoucherAdapter.VoucherViewHolder>() {

    class VoucherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView4)
        val nameVoucher: TextView = itemView.findViewById(R.id.nameVoucher)
        val contentVoucher: TextView = itemView.findViewById(R.id.contentVoucher)
        val expiryDateText: TextView = itemView.findViewById(R.id.textTime)
        val stateVoucher: TextView = itemView.findViewById(R.id.stateVoucher)

        fun bind(voucher: Voucher) {
            nameVoucher.text = voucher.name
            contentVoucher.text = voucher.content
            val expiryDate = voucher.expiryDate.toDate()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(expiryDate)
            expiryDateText.text = "Hạn: $formattedDate"

            stateVoucher.text = voucher.state

            if (voucher.state == "ACTIVE") {
                stateVoucher.setTextColor(itemView.context.getColor(R.color.green))
            } else {
                stateVoucher.setTextColor(itemView.context.getColor(R.color.button_red))
            }

            Glide.with(itemView.context).load(voucher.imageUrl).into(imageView)
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

//    fun updateVoucherList(newVoucherList: List<Voucher>) {
//        voucherList.clear()
//        voucherList.addAll(newVoucherList)
//        notifyDataSetChanged()
//    }
}
