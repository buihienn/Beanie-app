package com.bh.beanie.user.fragment

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bh.beanie.BeanieApplication
import com.bh.beanie.R
import com.bh.beanie.databinding.FragmentSelectAddressBinding
import com.bh.beanie.databinding.ItemAddressBinding
import com.bh.beanie.model.Address
import com.bh.beanie.repository.AddressRepository
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SelectAddressFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentSelectAddressBinding? = null
    private val binding get() = _binding!!

    private val addressList = mutableListOf<Address>()
    private lateinit var addressAdapter: AddressAdapter
    private val addressRepository = AddressRepository()

    private var addressSelectedListener: ((Address) -> Unit)? = null

    fun setAddressSelectedListener(listener: (Address) -> Unit) {
        this.addressSelectedListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Thiết lập các thành phần UI
        setupUI()

        // Thiết lập RecyclerView
        setupRecyclerView()

        // Tải địa chỉ từ Firebase
        loadAddresses()
    }

    override fun getTheme(): Int {
        return R.style.BottomSheetStyle_Level1
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheet = (dialogInterface as BottomSheetDialog)
                .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return@setOnShowListener

            val behavior = BottomSheetBehavior.from(bottomSheet)

            // Đặt chiều cao hiển thị là 90% màn hình
            val displayMetrics = requireContext().resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels

            val layoutParams = bottomSheet.layoutParams
            layoutParams.height = (screenHeight * 0.9).toInt()
            bottomSheet.layoutParams = layoutParams

            behavior.peekHeight = (screenHeight * 0.9).toInt()
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }

        return dialog
    }

    private fun setupUI() {
        // Thiết lập nút đóng
        binding.closeButton.setOnClickListener {
            dismiss()
        }

        // Thiết lập nút thêm địa chỉ mới
        binding.addNewAddressButton.setOnClickListener {
            showAddEditAddressFragment()
        }
    }

    private fun setupRecyclerView() {
        addressAdapter = AddressAdapter(addressList)
        binding.addressRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = addressAdapter
        }
    }

    private fun loadAddresses() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE

        val userId = BeanieApplication.instance.getUserId()
        if (userId.isEmpty()) {
            showEmptyState("Không tìm thấy người dùng, vui lòng đăng nhập lại")
            return
        }

        addressRepository.getAddresses(userId) { addresses ->
            requireActivity().runOnUiThread {
                binding.progressBar.visibility = View.GONE

                if (addresses.isEmpty()) {
                    showEmptyState("Chưa có địa chỉ nào")
                } else {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.addressRecyclerView.visibility = View.VISIBLE

                    addressList.clear()
                    addressList.addAll(addresses)
                    addressAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun showEmptyState(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.addressRecyclerView.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.emptyStateText.text = message
    }

    private fun showAddEditAddressFragment(address: Address? = null) {
        val fragment = AddEditAddressFragment.newInstance(address)
        fragment.setOnAddressUpdatedListener {
            loadAddresses()
        }
        fragment.show(parentFragmentManager, "AddEditAddressFragment")
    }

    inner class AddressAdapter(private val addresses: List<Address>) :
        RecyclerView.Adapter<AddressAdapter.AddressViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
            val binding = ItemAddressBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return AddressViewHolder(binding)
        }

        override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
            val address = addresses[position]
            holder.bind(address)
        }

        override fun getItemCount(): Int = addresses.size

        inner class AddressViewHolder(private val binding: ItemAddressBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(address: Address) {
                binding.apply {
                    // Thiết lập tiêu đề địa chỉ
                    addressTitle.text = address.nameAddress

                    // Hiển thị "Default address" nếu là địa chỉ mặc định
                    defaultAddressText.visibility = if (address.isDefault) View.VISIBLE else View.GONE

                    // Thiết lập thông tin người nhận
                    recipientNameText.text = address.name
                    phoneNumberText.text = address.phoneNumber
                    addressDetailText.text = address.addressDetail

                    // Thiết lập sự kiện chỉnh sửa
                    editButton.setOnClickListener {
                        showAddEditAddressFragment(address)
                    }

                    // Thiết lập sự kiện chọn địa chỉ
                    root.setOnClickListener {
                        val userId = BeanieApplication.instance.getUserId()
                        saveSelectedAddress(userId, address)
                        addressSelectedListener?.invoke(address)
                        dismiss()
                    }
                }
            }
        }
    }

    private fun saveSelectedAddress(userId: String, address: Address) {
        val sharedPreferences = requireActivity().getSharedPreferences("BeaniePref", Context.MODE_PRIVATE)
        sharedPreferences.edit {
            // Lưu thông tin địa chỉ
            putString("selected_user_id", userId)
            putString("selected_address_id", address.id)
            putString("selected_address_name", address.name)
            putString("selected_address_phone", address.phoneNumber)
            putString("selected_address_detail", address.addressDetail)
            putBoolean("selected_address_default", address.isDefault)

            // Lưu địa chỉ dưới dạng String để hiển thị
            putString("selected_address_display", address.addressDetail)
        }
    }

    companion object {
        fun newInstance() = SelectAddressFragment()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}