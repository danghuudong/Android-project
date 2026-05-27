package com.example.project_android.navigation

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.PopupMenu
import com.example.project_android.R
import com.example.project_android.view.activity.MainActivity

object AdminAvatarController {

    private const val MENU_LOGOUT = 1

    /**
     * Gắn hành động tài khoản cho avatar admin có trong layout XML.
     */
    fun bind(activity: Activity) {
        val avatar = activity.findViewById<View?>(R.id.btnAdminAvatar) ?: return
        bind(activity, avatar)
    }

    /**
     * Dùng cho các header được dựng bằng Kotlin thay vì layout XML.
     */
    fun bind(activity: Activity, avatar: View) {
        avatar.setOnClickListener { anchor ->
            PopupMenu(activity, anchor).apply {
                menu.add(0, MENU_LOGOUT, 0, "Đăng xuất")
                setOnMenuItemClickListener { item ->
                    if (item.itemId == MENU_LOGOUT) {
                        logout(activity)
                        true
                    } else {
                        false
                    }
                }
                show()
            }
        }
    }

    /**
     * Thông tin đã tick "Ghi nhớ đăng nhập" vẫn giữ để điền lại tại form login.
     */
    private fun logout(activity: Activity) {
        activity.startActivity(Intent(activity, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}
