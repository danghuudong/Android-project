package com.example.project_android.view.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.project_android.R
import com.example.project_android.model.dao.DiscountCodeDao
import com.example.project_android.model.database.DatabaseHelper
import com.example.project_android.model.entity.DiscountCode
import com.example.project_android.navigation.AppNavigator
import com.example.project_android.navigation.SidebarController
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DiscountCodeActivity : AppCompatActivity() {

    private val pageSize = 3
    private var allDiscountCodes: List<DiscountCode> = emptyList()
    private var filteredDiscountCodes: List<DiscountCode> = emptyList()
    private var displayedDiscountCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discount_code)

        // Gắn hamburger với sidebar dùng chung của toàn app.
        SidebarController.bindFromActivity(this)

        findViewById<LinearLayout>(R.id.fabAddDiscount).setOnClickListener {
            AppNavigator.openAddDiscountCode(this)
        }

        findViewById<LinearLayout>(R.id.btnLoadMoreDiscounts).setOnClickListener {
            renderNextDiscountCodes()
        }

        setupDiscountSearch()
        bindDeleteMenu(R.id.btnDiscountMoreAtelier, "ATELIER20")
        bindDeleteMenu(R.id.btnDiscountMoreSummer, "SUMMER24")
        bindDeleteMenu(R.id.btnDiscountMoreSpring, "SPRING23")
    }

    override fun onResume() {
        super.onResume()
        loadDiscountCodes()
    }

    private fun bindStats(discounts: List<DiscountCode>) {
        val runningCount = discounts.count { isRunning(it) }
        val totalUsed = discounts.sumOf { it.usedCount }
        val totalSaved = discounts.sumOf { it.totalSaved }
        val expiringCount = discounts.count { isExpiringSoon(it) }

        bindStat(R.id.statActive, "ĐANG CHẠY", runningCount.toString())
        bindStat(R.id.statUsed, "ĐÃ DÙNG", formatNumber(totalUsed.toDouble()))
        bindStat(R.id.statSaving, "TIẾT KIỆM", formatCompactVnd(totalSaved))
        bindStat(R.id.statExpiring, "SẮP HẾT HẠN", expiringCount.toString().padStart(2, '0'), R.color.accent_magenta)
    }

    private fun bindStat(rootId: Int, label: String, value: String, valueColor: Int = R.color.brand_primary) {
        val root = findViewById<LinearLayout>(rootId)
        root.findViewById<TextView>(R.id.tvStatLabel).text = label
        root.findViewById<TextView>(R.id.tvStatValue).apply {
            text = value
            setTextColor(ContextCompat.getColor(this@DiscountCodeActivity, valueColor))
        }
    }

    private fun loadDiscountCodes() {
        allDiscountCodes = DiscountCodeDao(DatabaseHelper(this).readableDatabase).getAll()
        applySearchFilter(findViewById<EditText>(R.id.edtSearchDiscountCode).text.toString())
        bindStats(allDiscountCodes)
    }

    private fun setupDiscountSearch() {
        findViewById<EditText>(R.id.edtSearchDiscountCode).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applySearchFilter(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun applySearchFilter(query: String) {
        val keyword = query.trim()
        filteredDiscountCodes = if (keyword.isBlank()) {
            allDiscountCodes
        } else {
            allDiscountCodes.filter { discount ->
                discount.code.contains(keyword, ignoreCase = true)
            }
        }

        displayedDiscountCount = 0
        val container = findViewById<LinearLayout>(R.id.discountUserListContainer)
        container.removeAllViews()

        renderNextDiscountCodes()
    }

    private fun renderNextDiscountCodes() {
        val container = findViewById<LinearLayout>(R.id.discountUserListContainer)
        val nextCount = minOf(displayedDiscountCount + pageSize, filteredDiscountCodes.size)

        for (index in displayedDiscountCount until nextCount) {
            container.addView(createDiscountCard(filteredDiscountCodes[index]))
        }

        displayedDiscountCount = nextCount
        findViewById<LinearLayout>(R.id.btnLoadMoreDiscounts).visibility =
            if (displayedDiscountCount < filteredDiscountCodes.size) View.VISIBLE else View.GONE
    }

    private fun createDiscountCard(discount: DiscountCode): android.view.View {
        val container = findViewById<LinearLayout>(R.id.discountUserListContainer)
        val card = LayoutInflater.from(this).inflate(
            R.layout.item_discount_code_created,
            container,
            false
        )
        val expired = isExpired(discount.endDate)
        val percentUsed = if (discount.usageLimit > 0) {
            ((discount.usedCount * 100) / discount.usageLimit).coerceIn(0, 100)
        } else {
            0
        }

        card.alpha = if (expired) 0.6f else 1f
        card.findViewById<TextView>(R.id.tvCreatedCode).text = discount.code
        card.findViewById<TextView>(R.id.tvCreatedDiscountTitle).text = discountTitle(discount)
        card.findViewById<TextView>(R.id.tvCreatedProgramName).text = rewardDescription(discount)
        card.findViewById<TextView>(R.id.tvCreatedMaximumDiscount).text =
            "Giảm tối đa: ${formatNumber(discount.maximumDiscount)}đ"
        card.findViewById<TextView>(R.id.tvCreatedStartDate).text = "Bắt đầu: ${discount.startDate}"
        card.findViewById<TextView>(R.id.tvCreatedEndDate).text =
            if (expired) "Hết hạn: ${discount.endDate}" else "Hạn dùng: ${discount.endDate}"
        card.findViewById<TextView>(R.id.tvCreatedUsage).text =
            "Đã dùng: ${discount.usedCount}/${discount.usageLimit}"
        card.findViewById<TextView>(R.id.tvCreatedUsagePercent).text = "$percentUsed%"

        card.findViewById<TextView>(R.id.tvCreatedStatus).apply {
            text = if (expired) "ĐÃ HẾT HẠN" else "ĐANG HOẠT ĐỘNG"
            setBackgroundResource(
                if (expired) R.drawable.bg_discount_status_expired else R.drawable.bg_discount_status_active
            )
            setTextColor(
                ContextCompat.getColor(
                    this@DiscountCodeActivity,
                    if (expired) R.color.discount_error_text else android.R.color.white
                )
            )
        }

        card.findViewById<android.view.View>(R.id.viewCreatedProgress).layoutParams =
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, percentUsed.toFloat())
        card.findViewById<android.view.View>(R.id.viewCreatedProgressRemaining).layoutParams =
            LinearLayout.LayoutParams(0, 1, (100 - percentUsed).toFloat())

        card.findViewById<ImageView>(R.id.btnCreatedDiscountMore).setOnClickListener { anchor ->
            showDiscountMenu(anchor, discount)
        }
        card.setOnClickListener {
            AppNavigator.openDiscountCodeDetail(this, discount.id)
        }
        return card
    }

    private fun showDiscountMenu(anchor: View, discount: DiscountCode) {
        android.widget.PopupMenu(this, anchor).apply {
            menu.add(0, MENU_EDIT, 0, "Sửa mã giảm giá")
            menu.add(0, MENU_DELETE, 1, "Xóa mã giảm giá")
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    MENU_EDIT -> AppNavigator.openEditDiscountCode(this@DiscountCodeActivity, discount.id)
                    MENU_DELETE -> confirmDeleteDiscount(discount)
                }
                true
            }
            show()
        }
    }

    private fun confirmDeleteDiscount(discount: DiscountCode) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Xóa mã giảm giá")
            .setMessage("Bạn có chắc muốn xóa mã ${discount.code} không?")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Xóa") { _, _ ->
                val deleted = DiscountCodeDao(DatabaseHelper(this).writableDatabase).delete(discount.id)
                if (deleted) {
                    Toast.makeText(this, "Đã xóa mã giảm giá", Toast.LENGTH_SHORT).show()
                    loadDiscountCodes()
                } else {
                    Toast.makeText(this, "Không thể xóa mã giảm giá", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun discountTitle(discount: DiscountCode): String {
        val amount = if (discount.discountType == "percent") {
            "${formatNumber(discount.discountValue)}%"
        } else {
            "${formatNumber(discount.discountValue)}đ"
        }
        return if (discount.minimumOrder > 0) {
            "Giảm $amount cho đơn từ ${formatNumber(discount.minimumOrder)}đ"
        } else {
            "Giảm $amount cho đơn thuê"
        }
    }

    private fun rewardDescription(discount: DiscountCode): String {
        if (discount.requiredTotalSpent < 0) {
            return "${discount.programName}\nKhông cấp tự động qua email"
        }
        val threshold = if (discount.requiredTotalSpent == 0.0) {
            "Mốc khởi đầu"
        } else {
            "Tổng chi tiêu từ ${formatNumber(discount.requiredTotalSpent)}đ"
        }
        return "${discount.programName}\nGửi email: $threshold"
    }

    private fun formatNumber(value: Double): String {
        return String.format(Locale.US, "%,.0f", value).replace(",", ".")
    }

    private fun formatCompactVnd(value: Double): String {
        if (value < 1_000_000) return "${formatNumber(value)}đ"
        val millions = value / 1_000_000
        val formatted = if (millions % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f", millions)
        } else {
            String.format(Locale.US, "%.1f", millions).replace(".", ",")
        }
        return "${formatted}tr"
    }

    private fun isRunning(discount: DiscountCode): Boolean {
        return discount.status == "active" &&
            discount.usedCount < discount.usageLimit &&
            isAvailableToday(discount.startDate, discount.endDate)
    }

    private fun isExpiringSoon(discount: DiscountCode): Boolean {
        if (!isRunning(discount)) return false
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")).apply {
            isLenient = false
        }
        val endDate = runCatching { formatter.parse(discount.endDate) }.getOrNull() ?: return false
        val today = runCatching { formatter.parse(formatter.format(Date())) }.getOrNull() ?: return false
        val limit = Calendar.getInstance().apply {
            time = today
            add(Calendar.DAY_OF_YEAR, 30)
        }.time
        return !endDate.before(today) && !endDate.after(limit)
    }

    private fun isAvailableToday(startDateValue: String, endDateValue: String): Boolean {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")).apply {
            isLenient = false
        }
        val startDate = runCatching { formatter.parse(startDateValue) }.getOrNull() ?: return false
        val endDate = runCatching { formatter.parse(endDateValue) }.getOrNull() ?: return false
        val today = runCatching { formatter.parse(formatter.format(Date())) }.getOrNull() ?: return false
        return !today.before(startDate) && !today.after(endDate)
    }

    private fun isExpired(date: String): Boolean {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
        val endDate = runCatching { formatter.parse(date) }.getOrNull() ?: return false
        val today = runCatching { formatter.parse(formatter.format(Date())) }.getOrNull() ?: return false
        return endDate.before(today)
    }

    private fun bindDeleteMenu(viewId: Int, code: String) {
        findViewById<ImageView>(viewId).setOnClickListener {
            Toast.makeText(this, "Xóa mã $code", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val MENU_EDIT = 1
        private const val MENU_DELETE = 2
    }
}
