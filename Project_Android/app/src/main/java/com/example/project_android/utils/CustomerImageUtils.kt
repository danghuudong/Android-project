package com.example.project_android.utils

import android.widget.ImageView
import com.example.project_android.R

/**
 * Đồng bộ ảnh đại diện khách hàng ở các màn hình.
 *
 * Ảnh người dùng chọn luôn được ưu tiên. Các hồ sơ demo ban đầu chưa lưu URI
 * trong SQLite nên dùng ảnh catalog cục bộ tương ứng thay cho icon trống.
 */
object CustomerImageUtils {
    fun bindAvatar(
        target: ImageView,
        avatarUrl: String,
        customerId: String = "",
        phone: String = ""
    ): Boolean {
        val demoAvatar = demoAvatarRes(customerId, phone)
        val fallbackRes = if (avatarUrl.isBlank()) {
            demoAvatar ?: android.R.drawable.ic_menu_myplaces
        } else {
            android.R.drawable.ic_menu_myplaces
        }

        ImageUtils.bindImage(
            target,
            avatarUrl,
            fallbackRes
        )
        return avatarUrl.isNotBlank() || demoAvatar != null
    }

    private fun demoAvatarRes(customerId: String, phone: String): Int? {
        return when {
            customerId == "c1" || phone == "0901112222" -> R.drawable.customer_demo_thu_ha
            customerId == "c2" || phone == "0903334444" -> R.drawable.customer_demo_van_nam
            customerId == "c3" || phone == "0905556666" -> R.drawable.customer_demo_tuyet_mai
            customerId == "c4" || phone == "0907778888" -> R.drawable.customer_demo_minh_hoang
            customerId == "c5" || phone == "0909990000" -> R.drawable.customer_demo_lan_anh
            else -> null
        }
    }
}
