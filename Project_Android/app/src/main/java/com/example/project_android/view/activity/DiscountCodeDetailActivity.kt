package com.example.project_android.view.activity

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.project_android.R
import com.example.project_android.model.dao.DiscountCodeDao
import com.example.project_android.model.database.DatabaseHelper
import com.example.project_android.model.entity.DiscountCode
import com.example.project_android.navigation.AdminAvatarController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiscountCodeDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discount_code_detail)

        AdminAvatarController.bind(this)
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        val discountId = intent.getStringExtra(EXTRA_DISCOUNT_ID) ?: run {
            finish()
            return
        }
        val discount = DiscountCodeDao(DatabaseHelper(this).readableDatabase).getById(discountId) ?: run {
            finish()
            return
        }

        bindDiscountDetail(discount)
    }

    private fun bindDiscountDetail(discount: DiscountCode) {
        val valueLabel = discountValueLabel(discount)
        val expired = isExpired(discount.endDate) || discount.status != "active"
        val usagePercent = if (discount.usageLimit > 0) {
            ((discount.usedCount * 100) / discount.usageLimit).coerceIn(0, 100)
        } else {
            0
        }

        findViewById<TextView>(R.id.tvStatus).apply {
            text = if (expired) "ĐÃ HẾT HẠN" else "ĐANG HOẠT ĐỘNG"
            setBackgroundResource(
                if (expired) R.drawable.bg_discount_status_expired else R.drawable.bg_discount_status_active
            )
            setTextColor(
                ContextCompat.getColor(
                    this@DiscountCodeDetailActivity,
                    if (expired) R.color.discount_error_text else android.R.color.white
                )
            )
        }
        findViewById<TextView>(R.id.tvCode).text = discount.code
        findViewById<TextView>(R.id.tvDiscountSummary).text = "Giảm $valueLabel trên tổng hóa đơn"
        findViewById<TextView>(R.id.tvDiscountValue).text = valueLabel
        findViewById<TextView>(R.id.tvUsedCount).text = discount.usedCount.toString()
        findViewById<TextView>(R.id.tvUsageLimit).text = "/ ${discount.usageLimit}"
        findViewById<TextView>(R.id.tvTotalSaved).text = "${formatNumber(discount.totalSaved)} VND"
        findViewById<TextView>(R.id.tvRevenue).text = "${formatNumber(discount.generatedRevenue)} VND"
        findViewById<TextView>(R.id.tvProgramName).text = discount.programName
        findViewById<TextView>(R.id.tvDiscountType).text =
            if (discount.discountType == "percent") "Phần trăm (%)" else "Số tiền (VND)"
        findViewById<TextView>(R.id.tvMinimumOrder).text = "${formatNumber(discount.minimumOrder)} VND"
        findViewById<TextView>(R.id.tvMaximumDiscount).text = "${formatNumber(discount.maximumDiscount)} VND"
        findViewById<TextView>(R.id.tvLimit).text = "${discount.usageLimit} lượt áp dụng"
        findViewById<TextView>(R.id.tvStartDate).text = discount.startDate
        findViewById<TextView>(R.id.tvEndDate).text = discount.endDate

        findViewById<View>(R.id.viewUsageProgress).layoutParams =
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, usagePercent.toFloat())
        findViewById<View>(R.id.viewUsageRemaining).layoutParams =
            LinearLayout.LayoutParams(0, 1, (100 - usagePercent).toFloat())

    }

    private fun discountValueLabel(discount: DiscountCode): String {
        return if (discount.discountType == "percent") {
            "${formatNumber(discount.discountValue)}%"
        } else {
            "${formatNumber(discount.discountValue)}đ"
        }
    }

    private fun formatNumber(value: Double): String {
        return String.format(Locale.US, "%,.0f", value).replace(",", ".")
    }

    private fun isExpired(date: String): Boolean {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
        val endDate = runCatching { formatter.parse(date) }.getOrNull() ?: return false
        val today = runCatching { formatter.parse(formatter.format(Date())) }.getOrNull() ?: return false
        return endDate.before(today)
    }

    companion object {
        const val EXTRA_DISCOUNT_ID = "discount_id"
    }
}
