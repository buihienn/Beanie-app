package com.bh.beanie.user.fragment

import android.content.Context
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bh.beanie.databinding.ItemAddressBinding
import com.bh.beanie.databinding.FragmentSelectAddressBinding
import com.bh.beanie.model.Address
import androidx.core.content.edit

/**
 * A fragment that shows a list of addresses as a modal bottom sheet.
 *
 * You can show this modal bottom sheet from your activity like this:
 * <pre>
 *    SelectAddressFragment.newInstance().show(supportFragmentManager, "dialog")
 * </pre>
 */
class SelectAddressFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentSelectAddressBinding? = null
    private val binding get() = _binding!!

    private val addressList = mutableListOf<Address>()
    private lateinit var addressAdapter: AddressAdapter

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

        // Tạo dữ liệu mẫu
        createSampleAddresses()

        // Thiết lập RecyclerView
        setupRecyclerView()
    }

    private fun setupUI() {
        // Thiết lập nút đóng
        binding.closeButton.setOnClickListener {
            dismiss()
        }

        // Thiết lập nút thêm địa chỉ mới
        binding.addNewAddressButton.setOnClickListener {
            // TODO: Xử lý thêm địa chỉ mới
        }
    }

    private fun createSampleAddresses() {
        addressList.apply {
            add(Address(1, "Address A","Vinh", "0334435678", "227 Nguyễn Văn Cừ, Phường 4, Quận 5, TP.HCM", true))
            add(Address(2, "Address B","Vinh", "0334435678", "227 Nguyễn Văn Cừ, Phường 4, Quận 5, TP.HCM", false))
        }
    }

    private fun setupRecyclerView() {
        addressAdapter = AddressAdapter(addressList)
        binding.addressRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = addressAdapter
        }
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
                    addressTitle.text = "Address ${address.id}"

                    // Hiển thị "Default address" nếu là địa chỉ mặc định
                    if (address.isDefault) {
                        defaultAddressText.visibility = View.VISIBLE
                    } else {
                        defaultAddressText.visibility = View.GONE
                    }

                    // Thiết lập thông tin người nhận
                    recipientNameText.text = address.name
                    phoneNumberText.text = address.phoneNumber
                    addressDetailText.text = address.addressDetail

                    // Thiết lập sự kiện chỉnh sửa
                    editButton.setOnClickListener {
                        // TODO: Xử lý chỉnh sửa địa chỉ
                    }

                    // Thiết lập sự kiện chọn địa chỉ
                    root.setOnClickListener {
                        // TODO: Xử lý chọn địa chỉ và đóng dialog
                        // Lưu địa chỉ vào SharedPreferences
                        saveSelectedAddress(address)

                        // Thông báo cho activity biết địa chỉ đã được chọn
                        addressSelectedListener?.invoke(address)

                        dismiss()
                    }
                }
            }
        }
    }

    private fun saveSelectedAddress(address: Address) {
        val sharedPreferences = requireActivity().getSharedPreferences("BeaniePref", Context.MODE_PRIVATE)
        sharedPreferences.edit() {
            // Lưu thông tin địa chỉ
            putInt("selected_address_id", address.id)
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