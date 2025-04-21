package com.bh.beanie.user.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bh.beanie.BeanieApplication
import com.bh.beanie.R
import com.bh.beanie.databinding.FragmentAddEditAddressBinding
import com.bh.beanie.model.Address
import com.bh.beanie.repository.AddressRepository
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddEditAddressFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAddEditAddressBinding? = null
    private val binding get() = _binding!!

    private val addressRepository = AddressRepository()
    private var address: Address? = null
    private var isEditMode = false
    private var onAddressUpdatedListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lấy dữ liệu từ arguments
        address = arguments?.getParcelable(ARG_ADDRESS)
        isEditMode = address != null

        setupUI()
        setupListeners()
    }

    override fun getTheme(): Int {
        return R.style.BottomSheetStyle_Level2
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
            layoutParams.height = (screenHeight * 0.8).toInt()
            bottomSheet.layoutParams = layoutParams

            behavior.peekHeight = (screenHeight * 0.8).toInt()
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }

        return dialog
    }

    private fun setupUI() {
        // Đặt tiêu đề dựa trên chế độ
        binding.titleTextView.text = if (isEditMode) "Sửa địa chỉ" else "Thêm địa chỉ mới"

        // Điền thông tin nếu đang ở chế độ sửa
        if (isEditMode) {
            address?.let { addr ->
                binding.addressNameInput.setText(addr.nameAddress)
                binding.nameInput.setText(addr.name)
                binding.phoneInput.setText(addr.phoneNumber)
                binding.addressDetailInput.setText(addr.addressDetail)
                binding.defaultAddressSwitch.isChecked = addr.isDefault
                binding.deleteButton.visibility = View.VISIBLE
            }
        } else {
            binding.deleteButton.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        binding.closeButton.setOnClickListener {
            dismiss()
        }

        binding.saveButton.setOnClickListener {
            saveAddress()
        }

        binding.deleteButton.setOnClickListener {
            deleteAddress()
        }
    }

    private fun saveAddress() {
        val addressName = binding.addressNameInput.text.toString().trim()
        val name = binding.nameInput.text.toString().trim()
        val phone = binding.phoneInput.text.toString().trim()
        val addressDetail = binding.addressDetailInput.text.toString().trim()
        val isDefault = binding.defaultAddressSwitch.isChecked

        // Kiểm tra dữ liệu nhập
        if (addressName.isEmpty() || name.isEmpty() || phone.isEmpty() || addressDetail.isEmpty()) {
            Toast.makeText(context, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = BeanieApplication.instance.getUserId()
        if (userId.isEmpty()) {
            Toast.makeText(context, "Không tìm thấy thông tin người dùng, vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show()
            return
        }

        // Hiển thị trạng thái đang xử lý
        binding.progressBar.visibility = View.VISIBLE
        binding.saveButton.isEnabled = false

        if (isEditMode) {
            // Cập nhật địa chỉ
            val updatedAddress = Address(
                id = address?.id ?: "",
                nameAddress = addressName,
                name = name,
                phoneNumber = phone,
                addressDetail = addressDetail,
                isDefault = isDefault
            )

            addressRepository.updateAddress(userId, updatedAddress) { success, message ->
                requireActivity().runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.saveButton.isEnabled = true

                    if (success) {
                        Toast.makeText(context, "Cập nhật địa chỉ thành công", Toast.LENGTH_SHORT).show()
                        onAddressUpdatedListener?.invoke()
                        dismiss()
                    } else {
                        Toast.makeText(context, "Cập nhật không thành công: $message", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            // Thêm địa chỉ mới
            val newAddress = Address(
                id = "", // Sẽ được gán bởi Firestore
                nameAddress = addressName,
                name = name,
                phoneNumber = phone,
                addressDetail = addressDetail,
                isDefault = isDefault
            )

            addressRepository.addAddress(userId, newAddress) { success, message ->
                requireActivity().runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.saveButton.isEnabled = true

                    if (success) {
                        Toast.makeText(context, "Thêm địa chỉ thành công", Toast.LENGTH_SHORT).show()
                        onAddressUpdatedListener?.invoke()
                        dismiss()
                    } else {
                        Toast.makeText(context, "Thêm địa chỉ không thành công: $message", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun deleteAddress() {
        address?.let { addr ->
            // Xác nhận xóa
            binding.progressBar.visibility = View.VISIBLE
            binding.deleteButton.isEnabled = false
            binding.saveButton.isEnabled = false

            val userId = BeanieApplication.instance.getUserId()
            if (userId.isEmpty()) {
                Toast.makeText(context, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                binding.deleteButton.isEnabled = true
                binding.saveButton.isEnabled = true
                return
            }

            addressRepository.deleteAddress(userId, addr.id) { success, message ->
                requireActivity().runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.deleteButton.isEnabled = true
                    binding.saveButton.isEnabled = true

                    if (success) {
                        Toast.makeText(context, "Đã xóa địa chỉ", Toast.LENGTH_SHORT).show()
                        onAddressUpdatedListener?.invoke()
                        dismiss()
                    } else {
                        Toast.makeText(context, "Xóa không thành công: $message", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun setOnAddressUpdatedListener(listener: () -> Unit) {
        this.onAddressUpdatedListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ADDRESS = "arg_address"

        fun newInstance(address: Address? = null): AddEditAddressFragment {
            val fragment = AddEditAddressFragment()
            address?.let {
                val args = Bundle()
                args.putParcelable(ARG_ADDRESS, it)
                fragment.arguments = args
            }
            return fragment
        }
    }
}