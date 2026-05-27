package com.example.project_android.view.activity

import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.project_android.R
import com.example.project_android.navigation.AdminAvatarController
import com.example.project_android.navigation.AppNavigator
import com.example.project_android.network.ApiClient
import com.example.project_android.network.runApi
import com.example.project_android.utils.ImageUtils
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class ReturnProcessActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var ivReturnProductImage: ImageView
    private lateinit var tvReturnOrderCode: TextView
    private lateinit var tvReturnLateStatus: TextView
    private lateinit var tvReturnProductName: TextView
    private lateinit var tvReturnCustomerName: TextView
    private lateinit var tvReturnPickupDate: TextView
    private lateinit var tvReturnDueDateLabel: TextView
    private lateinit var tvReturnDueDate: TextView
    private lateinit var optionNormal: LinearLayout
    private lateinit var optionDirty: LinearLayout
    private lateinit var optionLost: LinearLayout
    private lateinit var financialSummaryCard: LinearLayout
    private lateinit var tvPenaltyLabel: TextView
    private lateinit var edtPenaltyAmount: EditText
    private lateinit var tvPenaltyHint: TextView
    private lateinit var edtReturnNotes: EditText
    private lateinit var btnConfirmReturn: Button

    private var currentOrderCode: String? = null
    private var currentCondition = "normal"
    private var currentPenalty = 0.0
    private var currentProductPrice = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_return_process)

        initViews()
        setupEvents()
        currentOrderCode = intent.getStringExtra("order_code")
        if (currentOrderCode == null) {
            loadFirstOrder()
        } else {
            loadReturnOrder()
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        ivReturnProductImage = findViewById(R.id.ivReturnProductImage)
        tvReturnOrderCode = findViewById(R.id.tvReturnOrderCode)
        tvReturnLateStatus = findViewById(R.id.tvReturnLateStatus)
        tvReturnProductName = findViewById(R.id.tvReturnProductName)
        tvReturnCustomerName = findViewById(R.id.tvReturnCustomerName)
        tvReturnPickupDate = findViewById(R.id.tvReturnPickupDate)
        tvReturnDueDateLabel = findViewById(R.id.tvReturnDueDateLabel)
        tvReturnDueDate = findViewById(R.id.tvReturnDueDate)
        optionNormal = findViewById(R.id.optionNormal)
        optionDirty = findViewById(R.id.optionDirty)
        optionLost = findViewById(R.id.optionLost)
        financialSummaryCard = findViewById(R.id.financialSummaryCard)
        tvPenaltyLabel = findViewById(R.id.tvPenaltyLabel)
        edtPenaltyAmount = findViewById(R.id.edtPenaltyAmount)
        tvPenaltyHint = findViewById(R.id.tvPenaltyHint)
        edtReturnNotes = findViewById(R.id.edtReturnNotes)
        btnConfirmReturn = findViewById(R.id.btnConfirmReturn)
        updateConditionSelection()
        updatePenaltyVisibility()
    }

    private fun setupEvents() {
        AdminAvatarController.bind(this)
        btnBack.setOnClickListener { finish() }
        optionNormal.setOnClickListener { selectCondition("normal", 0.0) }
        optionDirty.setOnClickListener { selectCondition("dirty_damaged", currentProductPrice * 0.15) }
        optionLost.setOnClickListener { selectCondition("lost", currentProductPrice) }
        btnConfirmReturn.setOnClickListener { confirmReturn() }
    }

    private fun selectCondition(condition: String, penalty: Double) {
        currentCondition = condition
        currentPenalty = penalty
        
        // Cập nhật số tiền vào ô EditText (format 2 chữ số thập phân)
        if (penalty > 0) {
            edtPenaltyAmount.setText(String.format(Locale.US, "%.2f", penalty))
        } else {
            edtPenaltyAmount.setText("0")
        }
        
        // Cập nhật câu hint cho Tóm tắt tài chính
        tvPenaltyHint.text = when(condition) {
            "dirty_damaged" -> "Gợi ý: Phí sửa chữa, vệ sinh (15% giá trị)"
            "lost" -> "Gợi ý: Bồi thường 100% do mất sản phẩm"
            else -> "Sản phẩm tốt, không phát sinh phí phạt"
        }
        
        // Cho phép sửa tiền phạt nếu không phải là Normal
        edtPenaltyAmount.isEnabled = condition != "normal"
        
        updateConditionSelection()
        updatePenaltyVisibility()
        val label = when (condition) {
            "normal" -> "Bình thường"
            "dirty_damaged" -> "Quá bẩn / Hư hỏng"
            else -> "Mất đồ"
        }
        Toast.makeText(this, "Tình trạng: $label", Toast.LENGTH_SHORT).show()
    }

    private fun updateConditionSelection() {
        optionNormal.setBackgroundResource(
            if (currentCondition == "normal") R.drawable.bg_condition_selected else R.drawable.bg_condition_card
        )
        optionDirty.setBackgroundResource(
            if (currentCondition == "dirty_damaged") R.drawable.bg_condition_selected else R.drawable.bg_condition_card
        )
        optionLost.setBackgroundResource(
            if (currentCondition == "lost") R.drawable.bg_condition_selected else R.drawable.bg_condition_card
        )
    }

    private fun updatePenaltyVisibility() {
        if (currentCondition == "normal") {
            financialSummaryCard.visibility = View.GONE
        } else {
            financialSummaryCard.visibility = View.VISIBLE
            tvPenaltyLabel.visibility = View.VISIBLE
            edtPenaltyAmount.visibility = View.VISIBLE
            tvPenaltyHint.visibility = View.VISIBLE
        }
    }

    private fun confirmReturn() {
        val orderCode = currentOrderCode
        if (orderCode == null) {
            Toast.makeText(this, "Chưa có đơn hàng để trả", Toast.LENGTH_SHORT).show()
            return
        }

        // Lấy số tiền từ ô nhập (nếu nhân viên có chỉnh sửa)
        val finalPenalty = edtPenaltyAmount.text.toString().trim().toDoubleOrNull() ?: 0.0

        val requestBody = JSONObject().apply {
            put("orderCode", orderCode)
            put("condition", currentCondition)
            put("penaltyAmount", finalPenalty)
            put("notes", edtReturnNotes.text.toString().trim())
        }

        // Tạo phiếu trả và cập nhật đơn sang trạng thái đã trả trong SQLite.
        runApi(
            loadingMessage = "Đang xác nhận trả hàng...",
            request = { ApiClient.post("/returns", requestBody) }
        ) {
            Toast.makeText(this, "Đã xác nhận trả hàng", Toast.LENGTH_SHORT).show()
            AppNavigator.openOrdersWithStatus(this, "returned")
            finish()
        }
    }

    private fun loadFirstOrder() {
        runApi(
            request = { ApiClient.get("/orders") }
        ) { result ->
            val orders = JSONObject(result.body).optJSONArray("data") ?: JSONArray()
            val order = findFirstReturnableOrder(orders)

            if (order == null) {
                Toast.makeText(this, "Không có đơn đang thuê để trả", Toast.LENGTH_SHORT).show()
                return@runApi
            }

            bindReturnOrder(order)
        }
    }

    private fun loadReturnOrder() {
        runApi(
            request = { ApiClient.get("/orders") }
        ) { result ->
            val orders = JSONObject(result.body).optJSONArray("data") ?: JSONArray()
            val order = findOrderByCode(orders, currentOrderCode.orEmpty())
            if (order == null) {
                Toast.makeText(this, "Không tìm thấy đơn trả", Toast.LENGTH_SHORT).show()
                finish()
                return@runApi
            }

            bindReturnOrder(order)
        }
    }

    private fun bindReturnOrder(order: JSONObject) {
        currentOrderCode = order.optString("code", currentOrderCode.orEmpty())

        val products = order.optJSONArray("products")
            ?: order.optJSONArray("productIds")
            ?: JSONArray()
        val firstProduct = products.optJSONObject(0)
        
        // Lấy giá trị sản phẩm từ JSON để tính toán động
        // Theo nghiệp vụ: Trường deposit trong DB lưu giá trị thực của sản phẩm (100%).
        // Tiền cọc thu của khách chỉ là 50% của deposit, nhưng khi phạt thì phạt trên 100% giá trị (tức là toàn bộ deposit).
        val deposit = firstProduct?.optDouble("deposit", 0.0) ?: 0.0
        currentProductPrice = deposit

        val pickupDate = order.optString("pickupDate", "")
        val returnDate = order.optString("returnDate", "")
        val returnDiffDays = returnDiffDays(returnDate)
        val customerPhone = order.optString("customerPhone", "")

        tvReturnOrderCode.text = "Đơn #${order.optString("code", "--")}"
        tvReturnProductName.text = firstProduct?.optString("name", "Sản phẩm thuê") ?: "Sản phẩm thuê"
        tvReturnCustomerName.text = order.optString("customerName", "Khách hàng")
        tvReturnPickupDate.text = formatDisplayDate(pickupDate)
        tvReturnLateStatus.text = returnTimingLabel(returnDiffDays)
        applyReturnTimingStyle(returnDiffDays)
        bindReturnDueDate(returnDate, returnDiffDays)

        bindCustomerAvatar(customerPhone)
    }

    private fun bindCustomerAvatar(phone: String) {
        ivReturnProductImage.setImageResource(android.R.drawable.ic_menu_myplaces)
        ivReturnProductImage.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#5A5368"))
        ivReturnProductImage.setPadding(dp(24), dp(24), dp(24), dp(24))
        if (phone.isBlank()) return

        runApi(
            loadingMessage = null,
            request = { ApiClient.get("/customers?search=$phone") }
        ) { result ->
            val customers = JSONObject(result.body).optJSONArray("data") ?: JSONArray()
            for (index in 0 until customers.length()) {
                val customer = customers.optJSONObject(index) ?: continue
                if (customer.optString("phone", "") == phone) {
                    val avatar = customer.optString("avatar", "")
                    ImageUtils.bindImage(
                        ivReturnProductImage,
                        avatar,
                        android.R.drawable.ic_menu_myplaces
                    )
                    if (avatar.isNotBlank()) {
                        ivReturnProductImage.imageTintList = null
                        ivReturnProductImage.clearColorFilter()
                        ivReturnProductImage.setPadding(0, 0, 0, 0)
                        ivReturnProductImage.clipToOutline = true
                    }
                    return@runApi
                }
            }
        }
    }

    private fun findOrderByCode(orders: JSONArray, code: String): JSONObject? {
        for (index in 0 until orders.length()) {
            val order = orders.optJSONObject(index) ?: continue
            if (order.optString("code") == code) return order
        }
        return null
    }

    private fun findFirstReturnableOrder(orders: JSONArray): JSONObject? {
        for (index in 0 until orders.length()) {
            val order = orders.optJSONObject(index) ?: continue
            val status = order.optString("status")
            if (status == "renting" || status == "overdue") return order
        }
        return null
    }

    private fun returnDiffDays(returnDate: String): Long? {
        return runCatching {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val due = formatter.parse(returnDate) ?: return@runCatching null
            val today = formatter.parse(todayStorageDate()) ?: return@runCatching null
            (today.time - due.time) / (24L * 60L * 60L * 1000L)
        }.getOrNull()
    }

    private fun returnTimingLabel(diffDays: Long?): String {
        return when {
            diffDays == null -> "KHÔNG XÁC ĐỊNH"
            diffDays < 0 -> "TRẢ TRƯỚC"
            diffDays > 0 -> "QUÁ HẠN"
            else -> "ĐÚNG HẠN"
        }
    }

    private fun returnDueDateText(returnDate: String, diffDays: Long?): String {
        val displayDate = formatDisplayDate(returnDate)
        return when {
            diffDays == null -> displayDate
            diffDays < 0 -> "$displayDate (Trả sớm ${kotlin.math.abs(diffDays)} ngày)"
            diffDays > 0 -> "$displayDate (Trễ $diffDays ngày)"
            else -> displayDate
        }
    }

    private fun bindReturnDueDate(returnDate: String, diffDays: Long?) {
        val displayDate = formatDisplayDate(returnDate)
        val dueText = returnDueDateText(returnDate, diffDays)
        if (diffDays == null || diffDays == 0L) {
            tvReturnDueDate.text = dueText
            return
        }

        tvReturnDueDate.text = SpannableString(dueText).apply {
            val dateEnd = displayDate.length.coerceAtMost(dueText.length)
            val suffixColor = if (diffDays < 0) Color.parseColor("#047857") else Color.parseColor("#DB2777")

            setSpan(
                ForegroundColorSpan(if (diffDays > 0) Color.parseColor("#DB2777") else Color.parseColor("#1F1B2D")),
                0,
                dateEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                ForegroundColorSpan(suffixColor),
                dateEnd,
                dueText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun applyReturnTimingStyle(diffDays: Long?) {
        val isOverdue = diffDays != null && diffDays > 0
        val textColor = if (isOverdue) Color.parseColor("#DC2626") else Color.parseColor("#047857")
        val backgroundColor = if (isOverdue) Color.parseColor("#FFE1D6") else Color.parseColor("#DCFCE7")

        tvReturnLateStatus.setTextColor(textColor)
        tvReturnPickupDate.setTextColor(Color.parseColor("#1F1B2D"))
        tvReturnDueDateLabel.setTextColor(Color.parseColor("#1F1B2D"))
        tvReturnDueDate.setTextColor(Color.parseColor("#1F1B2D"))
        tvReturnLateStatus.background = roundedBackground(backgroundColor)
    }

    private fun roundedBackground(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(12).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun todayStorageDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
    }

    private fun formatDisplayDate(value: String): String {
        val parts = value.split("-")
        return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else value.ifBlank { "--" }
    }
}
