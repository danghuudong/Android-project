package com.example.project_android.navigation

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.project_android.R

object SidebarController {

    private const val APP_PACKAGE = "com.example.project_android.view.activity"

    private enum class SidebarItem {
        OVERVIEW,
        CATEGORY_MANAGEMENT,
        DISCOUNT_CODES
    }

    fun bindFromActivity(activity: Activity) {
        val drawerLayout = activity.findViewById<DrawerLayout?>(R.id.drawerLayout) ?: return
        val btnOpen = activity.findViewById<ImageView?>(R.id.btnMenu) ?: return
        val btnClose = activity.findViewById<ImageView?>(R.id.btnDrawerMenu) ?: return
        val menuOverview = activity.findViewById<LinearLayout?>(R.id.menuOverview) ?: return
        val menuCategoryManagement = activity.findViewById<LinearLayout?>(R.id.menuCategoryManagement) ?: return
        val menuDiscountCodes = activity.findViewById<LinearLayout?>(R.id.menuDiscountCodes)

        bind(
            activity = activity,
            drawerLayout = drawerLayout,
            btnOpen = btnOpen,
            btnClose = btnClose,
            menuOverview = menuOverview,
            menuCategoryManagement = menuCategoryManagement,
            menuDiscountCodes = menuDiscountCodes
        )
        AdminAvatarController.bind(activity)
    }

    fun bind(
        activity: Activity,
        drawerLayout: DrawerLayout,
        btnOpen: ImageView,
        btnClose: ImageView,
        menuOverview: LinearLayout,
        menuCategoryManagement: LinearLayout,
        menuDiscountCodes: LinearLayout? = null
    ) {
        btnOpen.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        btnClose.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        val activeItem = activity.getActiveSidebarItem()
        applyActiveState(
            activity = activity,
            activeItem = activeItem,
            menuOverview = menuOverview,
            menuCategoryManagement = menuCategoryManagement,
            menuDiscountCodes = menuDiscountCodes
        )

        menuOverview.setOnClickListener {
            drawerLayout.closeAndNavigate(activeItem != SidebarItem.OVERVIEW) {
                activity.open("DashboardActivity")
            }
        }

        menuCategoryManagement.setOnClickListener {
            drawerLayout.closeAndNavigate(activeItem != SidebarItem.CATEGORY_MANAGEMENT) {
                activity.open("CategoryManagementActivity")
            }
        }

        menuDiscountCodes?.setOnClickListener {
            drawerLayout.closeAndNavigate(activeItem != SidebarItem.DISCOUNT_CODES) {
                activity.open("DiscountCodeActivity")
            }
        }

    }

    private fun applyActiveState(
        activity: Activity,
        activeItem: SidebarItem?,
        menuOverview: LinearLayout,
        menuCategoryManagement: LinearLayout,
        menuDiscountCodes: LinearLayout?
    ) {
        val menuItems = listOfNotNull(
            SidebarItem.OVERVIEW to menuOverview,
            SidebarItem.CATEGORY_MANAGEMENT to menuCategoryManagement,
            menuDiscountCodes?.let { SidebarItem.DISCOUNT_CODES to it }
        )

        val activeColor = ContextCompat.getColor(activity, R.color.brand_primary)
        val inactiveColor = ContextCompat.getColor(activity, R.color.text_primary)

        menuItems.forEach { (item, menuView) ->
            val isActive = item == activeItem
            menuView.setBackgroundResource(if (isActive) R.drawable.bg_sidebar_menu_active else 0)
            menuView.isSelected = isActive
            menuView.setChildTint(if (isActive) activeColor else inactiveColor)
        }
    }

    private fun LinearLayout.setChildTint(color: Int) {
        for (index in 0 until childCount) {
            when (val child = getChildAt(index)) {
                is ImageView -> child.imageTintList = ColorStateList.valueOf(color)
                is TextView -> child.setTextColor(color)
            }
        }
    }

    private fun Activity.getActiveSidebarItem(): SidebarItem? {
        return when (javaClass.simpleName) {
            "DashboardActivity" -> SidebarItem.OVERVIEW
            "CategoryManagementActivity" -> SidebarItem.CATEGORY_MANAGEMENT
            "DiscountCodeActivity" -> SidebarItem.DISCOUNT_CODES
            else -> null
        }
    }

    private fun DrawerLayout.closeAndNavigate(shouldNavigate: Boolean = true, navigate: () -> Unit) {
        closeDrawer(GravityCompat.START)
        if (shouldNavigate) {
            navigate()
        }
    }

    private fun Activity.open(activityName: String) {
        val target = Class.forName("$APP_PACKAGE.$activityName")
        startActivity(Intent(this, target))
    }

}
