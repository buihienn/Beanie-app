package com.bh.beanie.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.model.User

class AdminManageCustomerAdapter(
    private var customers: MutableList<User>,
    private val onEditClick: (User) -> Unit
) : RecyclerView.Adapter<AdminManageCustomerAdapter.CustomerViewHolder>() {

    inner class CustomerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.textViewNameCustomer)
        private val emailTextView: TextView = itemView.findViewById(R.id.textEmail)
        private val birthdayTextView: TextView = itemView.findViewById(R.id.textViewBirthdayCustomer)
        private val genderTextView: TextView = itemView.findViewById(R.id.textViewGenderCustomer)
        private val editButton: Button = itemView.findViewById(R.id.btnEdit)

        fun bind(customer: User) {
            nameTextView.text = customer.username
            emailTextView.text = customer.email
            birthdayTextView.text = customer.dob
            genderTextView.text = customer.gender

            genderTextView.setTextColor(
                if (customer.gender.equals("Male", ignoreCase = true)) {
                    Color.parseColor("#2196F3")
                } else {
                    Color.parseColor("#E91E63")
                }
            )

            editButton.setOnClickListener {
                onEditClick(customer)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer_admin, parent, false)
        return CustomerViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        holder.bind(customers[position])
    }

    override fun getItemCount(): Int = customers.size

    fun updateCustomers(newCustomerList: List<User>) {
        customers = newCustomerList.toMutableList()
        notifyDataSetChanged()
    }
}
