package com.bh.beanie.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp // Import nếu dùng ServerTimestamp
import java.util.Date // Import nếu dùng Date cho createdAt

@IgnoreExtraProperties
data class User(
    var username: String = "",
    var email: String = "",
    var phone: String = "",
    var dob: String = "",
    var gender: String = "",
    var avatarUrl: String? = null,
    var role: String = "customer", // <-- Thêm trường role với giá trị mặc định là "customer" (hoặc "user")

    // Thông tin tích điểm
    var points: Int = 0,                     // Tổng điểm để xét hạng
    var presentPoints: Int = 0,          // Tổng điểm hiện tại
    var membershipLevel: String = "New",  // Cấp độ thành viên "Loyal", "Vip"
    @ServerTimestamp
    var lastPointReset: Date? = null,   // Thời gian reset lần cuối

    //Time account created
    @ServerTimestamp // Firestore sẽ tự điền thời gian phía server khi tạo document
    var createdAt: Date? = null, // Kiểu Date hoặc com.google.firebase.Timestamp
) {
    // Constructor không tham số vẫn được đảm bảo do tất cả thuộc tính có giá trị mặc định.
}