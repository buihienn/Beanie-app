package com.bh.beanie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.model.Reward

class MembershipBenefitsAdapter(private val rewardsList: List<Reward>) :
    RecyclerView.Adapter<MembershipBenefitsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.textViewNameBen)
        val descriptionText: TextView = itemView.findViewById(R.id.textViewContentBen)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member_benefit, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reward = rewardsList[position]
        holder.titleText.text = reward.name
        holder.descriptionText.text = reward.content
    }

    override fun getItemCount(): Int = rewardsList.size
}