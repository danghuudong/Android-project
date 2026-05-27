package com.example.project_android.view.activity

import android.app.DatePickerDialog
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.project_android.R
import com.example.project_android.model.dao.DiscountCodeDao
import com.example.project_android.model.database.DatabaseHelper
import com.example.project_android.navigation.AdminAvatarController
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddDiscountCodeActivity : AppCompatActivity() {

    private lateinit var edtPromotionName: EditText
    private lateinit var edtDiscountCode: EditText
    private lateinit var edtDiscountValue: EditText
    private lateinit var edtMinimumOrder: EditText
    private lateinit var edtMaximumDiscount: EditText
    private lateinit var edtUsageLimit: EditText
    private lateinit var edtStartDate: EditText
    private lateinit var edtEndDate: EditText
    private lateinit var btnPercentType: TextView
    private lateinit var btnFixedType: TextView
    private lateinit var tvValueUnit: TextView

    private var discountType = TYPE_PERCENT
    private var discountId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_discount_code)

        initViews()
        setupEvents()
        loadDiscountForEditing()
    }

    private fun initViews() {
        edtPromotionName = findViewById(R.id.edtPromotionName)
        edtDiscountCode = findViewById(R.id.edtDiscountCode)
        edtDiscountValue = findViewById(R.id.edtDiscountValue)
        edtMinimumOrder = findViewById(R.id.edtMinimumOrder)
        edtMaximumDiscount = findViewById(R.id.edtMaximumDiscount)
        edtUsageLimit = findViewById(R.id.edtUsageLimit)
        edtStartDate = findViewById(R.id.edtStartDate)
        edtEndDate = findViewById(R.id.edtEndDate)
        btnPercentType = findViewById(R.id.btnPercentType)
        btnFixedType = findViewById(R.id.btnFixedType)
        tvValueUnit = findViewById(R.id.tvValueUnit)
    }

    private fun setupEvents() {
        AdminAvatarController.bind(this)
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }
        btnPercentType.setOnClickListener { selectDiscountType(TYPE_PERCENT) }
        btnFixedType.setOnClickListener { selectDiscountType(TYPE_FIXED) }
        edtStartDate.setOnClickListener { showDatePicker(edtStartDate) }
        edtEndDate.setOnClickListener { showDatePicker(edtEndDate) }
        findViewById<LinearLayout>(R.id.btnSaveDiscount).setOnClickListener { validateAndSave() }
    }

    private fun loadDiscountForEditing() {
        discountId = intent.getStringExtra(EXTRA_DISCOUNT_ID)
        val id = discountId ?: return
        val discount = DiscountCodeDao(DatabaseHelper(this).readableDatabase).getById(id) ?: run {
            finish()
            return
        }

        findViewById<TextView>(R.id.tvDiscountFormTitle).text = "Sửa mã giảm giá"
        findViewById<TextView>(R.id.tvSaveDiscountLabel).text = "Lưu thay đổi"
        edtPromotionName.setText(discount.programName)
        edtDiscountCode.setText(discount.code)
        edtDiscountValue.setText(editableDiscountValue(discount.discountValue, discount.discountType))
        edtMinimumOrder.setText(formatNumber(discount.minimumOrder))
        edtMaximumDiscount.setText(formatNumber(discount.maximumDiscount))
        edtUsageLimit.setText(discount.usageLimit.toString())
        edtStartDate.setText(discount.startDate)
        edtEndDate.setText(discount.endDate)
        selectDiscountType(discount.discountType)
    }

    private fun selectDiscountType(type: String) {
        discountType = type
        val isPercent = type == TYPE_PERCENT

        btnPercentType.setBackgroundResource(if (isPercent) R.drawable.bg_bottom_nav_active else 0)
        btnFixedType.setBackgroundResource(if (isPercent) 0 else R.drawable.bg_bottom_nav_active)
        btnPercentType.setTextColor(color(if (isPercent) android.R.color.white else R.color.text_primary))
        btnFixedType.setTextColor(color(if (isPercent) R.color.text_primary else android.R.color.white))
        tvValueUnit.text = if (isPercent) "%" else "đ"
        edtMaximumDiscount.isEnabled = isPercent
        edtMaximumDiscount.alpha = if (isPercent) 1f else 0.6f
        if (!isPercent && edtDiscountValue.text.toString().isNotBlank()) {
            edtMaximumDiscount.setText(formatNumber(readCurrencyAmount(edtDiscountValue)))
        }
        edtDiscountValue.inputType = if (isPercent) {
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        } else {
            InputType.TYPE_CLASS_NUMBER
        }
    }

    private fun showDatePicker(target: EditText) {
        val today = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance().apply { set(year, month, day) }
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
                target.setText(format.format(selectedDate.time))
            },
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun validateAndSave() {
        if (!requireText(edtPromotionName, "Vui lòng nhập tên chương trình")) return
        if (!requireText(edtDiscountCode, "Vui lòng nhập mã giảm giá")) return
        if (!requireText(edtDiscountValue, "Vui lòng nhập giá trị giảm")) return
        if (!requireText(edtMinimumOrder, "Vui lòng nhập giá trị đơn tối thiểu")) return
        if (discountType == TYPE_PERCENT && !requireText(edtMaximumDiscount, "Vui lòng nhập mức giảm tối đa")) return
        if (!requireText(edtUsageLimit, "Vui lòng nhập số lượng sử dụng")) return
        if (!requireText(edtStartDate, "Vui lòng chọn ngày bắt đầu")) return
        if (!requireText(edtEndDate, "Vui lòng chọn ngày kết thúc")) return

        val discountValue = if (discountType == TYPE_PERCENT) {
            readPercentValue()
        } else {
            readCurrencyAmount(edtDiscountValue)
        }
        if (discountType == TYPE_PERCENT && discountValue > 100) {
            edtDiscountValue.error = "Phần trăm giảm không được lớn hơn 100"
            edtDiscountValue.requestFocus()
            return
        }
        val maximumDiscount = if (discountType == TYPE_PERCENT) {
            readCurrencyAmount(edtMaximumDiscount)
        } else {
            discountValue
        }
        if (maximumDiscount <= 0) {
            edtMaximumDiscount.error = "Mức giảm tối đa phải lớn hơn 0"
            edtMaximumDiscount.requestFocus()
            return
        }
        if (discountType == TYPE_FIXED) {
            edtMaximumDiscount.setText(formatNumber(maximumDiscount))
        }

        try {
            val dao = DiscountCodeDao(DatabaseHelper(this).writableDatabase)
            val id = discountId
            if (id == null) {
                dao.insert(
                    programName = edtPromotionName.text.toString(),
                    code = edtDiscountCode.text.toString(),
                    discountType = discountType,
                    discountValue = discountValue,
                    minimumOrder = readCurrencyAmount(edtMinimumOrder),
                    maximumDiscount = maximumDiscount,
                    usageLimit = edtUsageLimit.text.toString().trim().toIntOrNull() ?: 0,
                    startDate = edtStartDate.text.toString(),
                    endDate = edtEndDate.text.toString()
                )
                Toast.makeText(this, "Đã lưu mã giảm giá", Toast.LENGTH_SHORT).show()
            } else {
                val updated = dao.update(
                    id = id,
                    programName = edtPromotionName.text.toString(),
                    code = edtDiscountCode.text.toString(),
                    discountType = discountType,
                    discountValue = discountValue,
                    minimumOrder = readCurrencyAmount(edtMinimumOrder),
                    maximumDiscount = maximumDiscount,
                    usageLimit = edtUsageLimit.text.toString().trim().toIntOrNull() ?: 0,
                    startDate = edtStartDate.text.toString(),
                    endDate = edtEndDate.text.toString()
                )
                if (!updated) {
                    Toast.makeText(this, "Không tìm thấy mã giảm giá để cập nhật", Toast.LENGTH_SHORT).show()
                    return
                }
                Toast.makeText(this, "Đã cập nhật mã giảm giá", Toast.LENGTH_SHORT).show()
            }
            finish()
        } catch (e: SQLiteConstraintException) {
            edtDiscountCode.error = "Mã giảm giá này đã tồn tại"
            edtDiscountCode.requestFocus()
        } catch (e: Exception) {
            Toast.makeText(this, "Không thể lưu mã giảm giá", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requireText(input: EditText, message: String): Boolean {
        if (input.text.toString().trim().isNotEmpty()) return true
        input.error = message
        input.requestFocus()
        return false
    }

    private fun readPercentValue(): Double {
        return edtDiscountValue.text.toString()
            .trim()
            .replace(",", ".")
            .toDoubleOrNull()
            ?: 0.0
    }

    private fun readCurrencyAmount(input: EditText): Double {
        return input.text.toString()
            .trim()
            .replace(".", "")
            .replace(",", "")
            .toDoubleOrNull()
            ?: 0.0
    }

    private fun formatNumber(value: Double): String {
        return String.format(Locale.US, "%,.0f", value).replace(",", ".")
    }

    private fun editableDiscountValue(value: Double, type: String): String {
        if (type != TYPE_PERCENT) return formatNumber(value)
        return if (value % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f", value)
        } else {
            value.toString().replace(".", ",")
        }
    }

    private fun color(colorId: Int): Int = ContextCompat.getColor(this, colorId)

    companion object {
        const val EXTRA_DISCOUNT_ID = "discount_id"
        private const val TYPE_PERCENT = "percent"
        private const val TYPE_FIXED = "fixed"
    }
}
