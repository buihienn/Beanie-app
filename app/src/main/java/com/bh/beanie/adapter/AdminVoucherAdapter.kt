package com.bh.beanie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.model.Voucher
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class AdminVoucherAdapter(
    private val voucherList: MutableList<Voucher>,
    private val onEditClick: (Voucher) -> Unit,
) : RecyclerView.Adapter<AdminVoucherAdapter.VoucherViewHolder>() {

    inner class VoucherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView4)
        private val nameVoucher: TextView = itemView.findViewById(R.id.nameVoucher)
        private val contentVoucher: TextView = itemView.findViewById(R.id.contentVoucher)
        private val expiryDateText: TextView = itemView.findViewById(R.id.textTime)
        private val stateVoucher: TextView = itemView.findViewById(R.id.stateVoucher)
        private val btnEditVoucher: Button = itemView.findViewById(R.id.btnEditVoucher)

        fun bind(voucher: Voucher, onEditClick: (Voucher) -> Unit) {
            nameVoucher.text = voucher.name
            contentVoucher.text = voucher.content

            val expiryDate = voucher.expiryDate?.toDate()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            expiryDateText.text = if (expiryDate != null) {
                "Hạn: ${dateFormat.format(expiryDate)}"
            } else {
                "Hạn: N/A"
            }

            stateVoucher.text = voucher.state
            val colorRes = if (voucher.state == "ACTIVE") R.color.green else R.color.button_red
            stateVoucher.setTextColor(itemView.context.getColor(colorRes))

            Glide.with(itemView.context)
                .load(voucher.imageUrl)
                .into(imageView)

            btnEditVoucher.setOnClickListener {
                onEditClick(voucher)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoucherViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_voucher_admin, parent, false)
        return VoucherViewHolder(view)
    }

    override fun onBindViewHolder(holder: VoucherViewHolder, position: Int) {
        holder.bind(voucherList[position], onEditClick)
    }

    override fun getItemCount(): Int = voucherList.size

    fun updateData(newList: List<Voucher>) {
        voucherList.clear()
        voucherList.addAll(newList)
        notifyDataSetChanged()
    }
}
