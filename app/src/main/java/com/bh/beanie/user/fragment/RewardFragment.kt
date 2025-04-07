package com.bh.beanie.user.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.R
import com.bh.beanie.adapter.MembershipBenefitsAdapter
import com.bh.beanie.model.MemberBenefit

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class RewardFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MembershipBenefitsAdapter
    private val listMemberBenefits = mutableListOf<MemberBenefit>()

    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reward, container, false)
        recyclerView = view.findViewById(R.id.listMemberBenefits)

        // Khởi tạo RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = MembershipBenefitsAdapter(listMemberBenefits)
        recyclerView.adapter = adapter

        // Load data mẫu
        loadFakeBenefits()

        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment OtherFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            RewardFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun loadFakeBenefits() {
        listMemberBenefits.clear()
        listMemberBenefits.addAll(
            listOf(
                MemberBenefit(
                    imgURL = "https://via.placeholder.com/150",
                    title = "Birthday Treat",
                    description = "Get 50% off up to 30k when you're promoted."
                ),
                MemberBenefit(
                    imgURL = "https://via.placeholder.com/150/FF5733/FFFFFF",
                    title = "Welcome Gift",
                    description = "Free drink for your first order."
                ),
                MemberBenefit(
                    imgURL = "https://via.placeholder.com/150/33FF57/000000",
                    title = "Loyalty Bonus",
                    description = "10k voucher after 5 purchases."
                )
            )
        )
        adapter.notifyDataSetChanged()
    }
}