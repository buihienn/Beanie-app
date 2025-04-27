package com.bh.beanie.user.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.BeanieApplication
import com.bh.beanie.R
import com.bh.beanie.adapter.MembershipBenefitsAdapter
import com.bh.beanie.model.Reward
import com.bh.beanie.repository.MembershipRepository
import com.bh.beanie.repository.UserRepository
import kotlinx.coroutines.launch

class RewardFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MembershipBenefitsAdapter
    private val rewardsList = mutableListOf<Reward>()

    private lateinit var btnNew: Button
    private lateinit var btnLoyal: Button
    private lateinit var btnVip: Button

    private var selectedButton: Button? = null

    private val membershipRepository = MembershipRepository()
    private val userRepository = UserRepository()

    companion object {
        fun newInstance(): RewardFragment {
            return RewardFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reward, container, false)

        val textLevel = view.findViewById<TextView>(R.id.textLevel)
        val tvBeaniesCount = view.findViewById<TextView>(R.id.tvBeaniesCount)
        // val fortuneWheelInfoButton = view.findViewById<Button>(R.id.fortuneWheelInfoButton) // doi thuong
        recyclerView = view.findViewById(R.id.listMemberBenefits)
        btnNew = view.findViewById(R.id.btnNew)
        btnLoyal = view.findViewById(R.id.btnLoyal)
        btnVip = view.findViewById(R.id.btnVip)

        // Initialize RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = MembershipBenefitsAdapter(rewardsList)
        recyclerView.adapter = adapter

        // Set button click listeners
        btnNew.setOnClickListener { onButtonClicked(btnNew, "New") }
        btnLoyal.setOnClickListener { onButtonClicked(btnLoyal, "Loyal") }
        btnVip.setOnClickListener { onButtonClicked(btnVip, "VIP") }

        val userId = BeanieApplication.instance.getUserId()
        checkAndUpdateMembershipLevel(userId)

        lifecycleScope.launch {
            try {
                val userInfo = userRepository.getUserMembershipInfo(userId)
                userInfo?.let {
                    val membershipLevel = it["membershipLevel"]?.toString()?.uppercase() ?: "UNKNOWN"
                    val presentPoints = it["presentPoints"]?.toString() ?: "0"

                    textLevel.text = membershipLevel
                    tvBeaniesCount.text = presentPoints

                    when (membershipLevel) {
                        "NEW" -> onButtonClicked(btnNew, "New")
                        "LOYAL" -> onButtonClicked(btnLoyal, "Loyal")
                        "VIP" -> onButtonClicked(btnVip, "VIP")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val redeemButton = view.findViewById<LinearLayout>(R.id.redeem)
        redeemButton.setOnClickListener {
            val redeemFragment = RedeemFragment.newInstance()
            redeemFragment.show(parentFragmentManager, RedeemFragment.TAG)
        }

        return view
    }

    private fun fetchUserPoints() {
        val userId = BeanieApplication.instance.getUserId()
        if (userId.isEmpty()) return

        lifecycleScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                user?.let {
                    // Cập nhật UI với số điểm hiện tại của user
                    view?.findViewById<TextView>(R.id.tvBeaniesCount)?.text =
                        it.presentPoints.toString()
                }
            } catch (e: Exception) {
                Log.e("RewardFragment", "Error fetching user points: ${e.message}")
                // Hiển thị giá trị mặc định nếu có lỗi
                view?.findViewById<TextView>(R.id.tvBeaniesCount)?.text = "0"
            }
        }
    }

    private fun fetchRewardsForLevel(level: String) {
        lifecycleScope.launch {
            try {
                val membership = membershipRepository.fetchMembershipByLevel(level)
                rewardsList.clear()
                membership?.rewards?.let { rewardsList.addAll(it) }
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun onButtonClicked(button: Button, level: String) {
        selectedButton?.setBackgroundResource(android.R.color.transparent)
        selectedButton?.setTextColor(resources.getColor(R.color.black, null))

        selectedButton = button
        selectedButton?.setBackgroundResource(R.color.selected_button_background)
        selectedButton?.setTextColor(resources.getColor(R.color.white, null))

        fetchRewardsForLevel(level)
    }

    private fun checkAndUpdateMembershipLevel(userId: String) {
        lifecycleScope.launch {
            try {
                val loyalPointsRequired = membershipRepository.getPointsRequiredForLevel("Loyal") ?: 0
                val vipPointsRequired = membershipRepository.getPointsRequiredForLevel("VIP") ?: 0
                Log.d("RewardFragment", "Loyal point: $loyalPointsRequired")
                Log.d ("RewardFragment2", "VIP point: $vipPointsRequired")
                val success = userRepository.updateMembershipLevel(userId, loyalPointsRequired, vipPointsRequired)
                if (success) {
                    // Optionally, refresh the UI or notify the user
                    val updatedUserInfo = userRepository.getUserMembershipInfo(userId)
                    updatedUserInfo?.let {
                        val membershipLevel = it["membershipLevel"]?.toString()?.uppercase() ?: "UNKNOWN"
                        val presentPoints = it["presentPoints"]?.toString() ?: "0"

                        // Update UI elements
                        view?.findViewById<TextView>(R.id.textLevel)?.text = membershipLevel
                        view?.findViewById<TextView>(R.id.tvBeaniesCount)?.text = presentPoints

                        val presentPointsInt = it["presentPoints"]?.toString()?.toInt() ?: 0
                        updateProgressBarAndContent(presentPointsInt, loyalPointsRequired, vipPointsRequired)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateProgressBarAndContent(currentPoints: Int, loyalPointsRequired: Int, vipPointsRequired: Int) {
        val progressBar = view?.findViewById<ProgressBar>(R.id.progressBar2)
        val textViewContent = view?.findViewById<TextView>(R.id.textViewContent)

        val nextLevelPointsRequired = when {
            currentPoints < loyalPointsRequired -> loyalPointsRequired
            currentPoints < vipPointsRequired -> vipPointsRequired
            else -> null // Already at the highest level
        }

        if (nextLevelPointsRequired != null) {
            val progress = (currentPoints * 100) / nextLevelPointsRequired
            progressBar?.progress = progress

            val pointsNeeded = nextLevelPointsRequired - currentPoints
            textViewContent?.text = "Earn $pointsNeeded more points to reach the next level!"
        } else {
            progressBar?.progress = 100
            textViewContent?.text = "You are at the highest membership level!"
        }
    }

    override fun onResume() {
        super.onResume()
        // Cập nhật điểm khi fragment được hiển thị lại
        fetchUserPoints()
    }
}