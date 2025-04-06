package com.bh.beanie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.model.MemberBenefit

class MembershipBenefitsAdapter (private val memBenList: List<MemberBenefit>) :
    RecyclerView.Adapter<MembershipBenefitsAdapter.ViewHolder>(){
    class ViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView){
        val imgView: ImageView = itemView.findViewById(R.id.imgViewMemBen)
        val titleText: TextView = itemView.findViewById(R.id.textViewNameBen)
        val descriptionText: TextView = itemView.findViewById(R.id.textView6)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member_benefit, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = memBenList[position]
//        holder.imgView.setImageResource(item.imgURL) img
        holder.titleText.text = item.title
        holder.descriptionText.text = item.description
    }

    override fun getItemCount(): Int = memBenList.size
}