package com.bh.beanie.model // Hoặc package bạn đã tạo

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

    //Time account created
    @ServerTimestamp // Firestore sẽ tự điền thời gian phía server khi tạo document
    var createdAt: Date? = null // Kiểu Date hoặc com.google.firebase.Timestamp


) {
    // Constructor không tham số vẫn được đảm bảo do tất cả thuộc tính có giá trị mặc định.
}