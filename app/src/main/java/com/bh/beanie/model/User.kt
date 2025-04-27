package com.bh.beanie.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class User(
    var username: String = "",
    var email: String = "",
    var phone: String = "",
    var dob: String = "",
    var gender: String = "",
    var avatarUrl: String? = null,
    var role: String = "customer",

    // Thông tin tích điểm
    var points: Int = 0,                     // Tổng điểm để xét hạng
    var presentPoints: Int = 0,          // Tổng điểm hiện tại
    var membershipLevel: String = "New",  // Cấp độ thành viên "Loyal", "Vip"
    @ServerTimestamp
    var lastPointReset: Date? = null,   // Thời gian reset lần cuối

    // Field to track last lucky wheel spin
    @ServerTimestamp
    var lastSpinDate: Date? = null,    // Track the date of the user's last spin

    //Time account created
    @ServerTimestamp
    var createdAt: Date? = null,
) {
    // Constructor không tham số vẫn được đảm bảo do tất cả thuộc tính có giá trị mặc định.
}