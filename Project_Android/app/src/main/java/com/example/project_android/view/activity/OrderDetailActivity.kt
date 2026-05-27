package com.example.project_android.view.activity

import android.os.Bundle
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.project_android.R
import com.example.project_android.navigation.AdminAvatarController
import com.example.project_android.network.ApiClient
import com.example.project_android.network.runApi
import com.example.project_android.utils.ImageUtils
import com.example.project_android.utils.CustomerImageUtils
import org.json.JSONArray
import org.json.JSONObject

class OrderDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnConfirmPayment: AppCompatButton
    private lateinit var tvOrderCodeHeader: TextView
    private lateinit var tvOrderCodeValue: TextView
    private lateinit var tvOrderStatus: TextView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvCustomerPhone: TextView
    private lateinit var tvCustomerEmail: TextView
    private lateinit var ivCustomerAvatar: ImageView
    private lateinit var ivProductImage: ImageView
    private lateinit var tvProductSku: TextView
    private lateinit var tvProductName: TextView
    private lateinit var tvProductSize: TextView
    private lateinit var tvProductQuantity: TextView
    private lateinit var tvProductFee: TextView
    private lateinit var tvRentDate: TextView
    private lateinit var tvReturnDate: TextView
    private lateinit var timelineStepsContainer: LinearLayout
    private val tvTimelineReceivedTime: TextView by lazy { TextView(this) }
    private val tvTimelineUsingHint: TextView by lazy { TextView(this) }
    private lateinit var tvSummaryRentalLabel: TextView
    private lateinit var tvSummaryRentalValue: TextView
    private lateinit var tvSummaryDepositValue: TextView
    private lateinit var tvSummaryTotalValue: TextView
    private lateinit var rowDiscountFee: LinearLayout
    private lateinit var tvSummaryDiscountLabel: TextView
    private lateinit var tvSummaryDiscountValue: TextView
    private lateinit var productExtraListContainer: android.widget.LinearLayout
    private lateinit var rowReturnCondition: LinearLayout
    private lateinit var tvSummaryReturnCondition: TextView
    private lateinit var rowPenaltyFee: LinearLayout
    private lateinit var tvSummaryPenaltyValue: TextView
    private lateinit var rowReturnNotes: LinearLayout
    private lateinit var tvSummaryReturnNotes: TextView

    private var currentOrderCode: String = ""

    private var hasLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)
        currentOrderCode = intent.getStringExtra("order_code").orEmpty()

        initViews()
        setupEvents()
        loadOrderDetail()
    }

    // E2E RELOAD: khi quay lại từ màn sửa đơn -> tải lại chi tiết để hiện dữ liệu mới
    override fun onResume() {
        super.onResume()
        if (hasLoaded) {
            loadOrderDetail()
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment)
        tvOrderCodeHeader = findViewById(R.id.tvOrderCodeHeader)
        tvOrderCodeValue = findViewById(R.id.tvOrderCodeValue)
        tvOrderStatus = findViewById(R.id.tvOrderStatus)
        tvCustomerName = findViewById(R.id.tvCustomerName)
        tvCustomerPhone = findViewById(R.id.tvCustomerPhone)
        tvCustomerEmail = findViewById(R.id.tvCustomerEmail)
        ivCustomerAvatar = findViewById(R.id.ivCustomerAvatar)
        ivProductImage = findViewById(R.id.ivProductImage)
        tvProductSku = findViewById(R.id.tvProductSku)
        tvProductName = findViewById(R.id.tvProductName)
        tvProductSize = findViewById(R.id.tvProductSize)
        tvProductQuantity = findViewById(R.id.tvProductQuantity)
        tvProductFee = findViewById(R.id.tvProductFee)
        tvRentDate = findViewById(R.id.tvRentDate)
        tvReturnDate = findViewById(R.id.tvReturnDate)
        timelineStepsContainer = findViewById(R.id.timelineStepsContainer)
        tvSummaryRentalLabel = findViewById(R.id.tvSummaryRentalLabel)
        tvSummaryRentalValue = findViewById(R.id.tvSummaryRentalValue)
        tvSummaryDepositValue = findViewById(R.id.tvSummaryDepositValue)
        tvSummaryTotalValue = findViewById(R.id.tvSummaryTotalValue)
        rowDiscountFee = findViewById(R.id.rowDiscountFee)
        tvSummaryDiscountLabel = findViewById(R.id.tvSummaryDiscountLabel)
        tvSummaryDiscountValue = findViewById(R.id.tvSummaryDiscountValue)
        productExtraListContainer = findViewById(R.id.productExtraListContainer)
        rowReturnCondition = findViewById(R.id.rowReturnCondition)
        tvSummaryReturnCondition = findViewById(R.id.tvSummaryReturnCondition)
        rowPenaltyFee = findViewById(R.id.rowPenaltyFee)
        tvSummaryPenaltyValue = findViewById(R.id.tvSummaryPenaltyValue)
        rowReturnNotes = findViewById(R.id.rowReturnNotes)
        tvSummaryReturnNotes = findViewById(R.id.tvSummaryReturnNotes)
        btnConfirmPayment.visibility = View.GONE
    }

    private fun setupEvents() {
        AdminAvatarController.bind(this)
        btnBack.setOnClickListener { finish() }

        // (Edit Order functionality removed, replaced by Avatar)

        btnConfirmPayment.setOnClickListener {
            if (currentOrderCode.isBlank()) {
                Toast.makeText(this, "Không tìm thấy mã đơn thuê", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Thanh toan duoc mo tu menu 3 cham o danh sach don tao moi.
        }
    }

    private fun loadOrderDetail() {
        runApi(
            loadingMessage = "Đang tải chi tiết đơn thuê...",
            request = { ApiClient.get("/orders") }
        ) { result ->
            val orders = JSONObject(result.body).optJSONArray("data") ?: JSONArray()
            val order = findTargetOrder(orders, currentOrderCode)
            if (order == null) {
                Toast.makeText(this, "Không tìm thấy đơn thuê", Toast.LENGTH_SHORT).show()
                finish()
                return@runApi
            }

            bindOrder(order)
            bindCustomerInfo(order.optString("customerPhone", ""))
            hasLoaded = true
        }
    }

    private fun findTargetOrder(orders: JSONArray, code: String): JSONObject? {
        if (orders.length() == 0) return null
        if (code.isBlank()) return orders.optJSONObject(0)

        for (index in 0 until orders.length()) {
            val order = orders.optJSONObject(index) ?: continue
            if (order.optString("code", "") == code) return order
        }
        return null
    }

    private fun bindOrder(order: JSONObject) {
        val status = order.optString("status", "renting")
        val orderCode = order.optString("code", currentOrderCode.ifBlank { "--" })
        val pickupDate = order.optString("pickupDate", "")
        val returnDate = order.optString("returnDate", "")
        val actualReturnDate = order.optString("actualReturnDate", "")
        tvOrderCodeHeader.text = "Chi tiết đơn thuê"
        tvOrderCodeValue.text = orderCode
        tvOrderStatus.text = statusLabel(status)
        btnConfirmPayment.visibility = View.GONE
        tvCustomerName.text = order.optString("customerName", "Khách hàng")
        tvCustomerPhone.text = order.optString("customerPhone", "--")
        tvRentDate.text = formatDate(pickupDate)
        tvReturnDate.text = returnDateSummary(pickupDate, returnDate, actualReturnDate, status)
        tvReturnDate.textSize = when {
            status == "cancelled" -> 14f
            status == "returned" && actualReturnDate.isNotBlank() -> 14f
            else -> 18f
        }
        renderTimeline(order, status, pickupDate, returnDate, actualReturnDate)
        tvTimelineReceivedTime.text = "Đang xử lý nhận đồ"
        tvTimelineUsingHint.text = "Dự kiến trả: ${formatDate(returnDate)}"

        if (status == "cancelled") {
            tvTimelineUsingHint.text = "\u0110\u01a1n \u0111\u00e3 h\u1ee7y"
        }

        val products = order.optJSONArray("products")
            ?: order.optJSONArray("productIds")
            ?: JSONArray()
        val groupedProducts = mutableListOf<Pair<JSONObject, Int>>()
        for (i in 0 until products.length()) {
            val p = products.optJSONObject(i) ?: continue
            val id = p.optString("id")
            val existingIndex = groupedProducts.indexOfFirst { it.first.optString("id") == id }
            if (existingIndex >= 0) {
                val existing = groupedProducts[existingIndex]
                groupedProducts[existingIndex] = Pair(existing.first, existing.second + 1)
            } else {
                groupedProducts.add(Pair(p, 1))
            }
        }

        var totalRentalPrice = 0.0
        var totalDeposit = 0.0
        for (i in 0 until products.length()) {
            val p = products.optJSONObject(i) ?: continue
            totalRentalPrice += p.optDouble("rentalPrice", 0.0)
            // Tiền cọc thu của khách bằng 50% giá trị sản phẩm (trường deposit trong DB lưu giá trị sản phẩm)
            totalDeposit += p.optDouble("deposit", 0.0) * 0.5
        }
        
        val days = rentalDays(pickupDate, returnDate)
        val finalRentalPrice = totalRentalPrice * days
        val finalTotalAmount = finalRentalPrice + totalDeposit
        val discountAmount = order.optDouble("discountAmount", 0.0)
        val discountCode = order.optString("discountCode", "")

        val firstGroup = groupedProducts.firstOrNull()
        val firstProduct = firstGroup?.first
        val firstQty = firstGroup?.second ?: 1

        tvProductSku.text = "SKU: ${firstProduct?.optString("id", "--") ?: "--"}"
        val baseName = firstProduct?.optString("name", "Sản phẩm thuê") ?: "Sản phẩm thuê"
        tvProductName.text = baseName
        tvProductSize.text = "SIZE: ${firstProduct?.optString("size", "--") ?: "--"}"
        tvProductQuantity.text = firstQty.toString()
        tvProductFee.text = formatVnd(firstProduct?.optDouble("rentalPrice", 0.0) ?: 0.0)
        
        if (status == "cancelled") {
            tvSummaryRentalLabel.text = "Tiền thuê (đã hủy)"
            tvSummaryRentalValue.text = formatVnd(0.0)
            tvSummaryDepositValue.text = formatVnd(0.0)
            tvSummaryTotalValue.text = formatVnd(0.0)
            rowDiscountFee.visibility = View.GONE
            rowReturnCondition.visibility = View.GONE
            rowPenaltyFee.visibility = View.GONE
            rowReturnNotes.visibility = View.GONE
        } else {
            tvSummaryRentalLabel.text = "Tiền thuê ($days ngày)"
            tvSummaryRentalValue.text = formatVnd(finalRentalPrice)
            tvSummaryDepositValue.text = formatVnd(totalDeposit)

            if (discountAmount > 0.0) {
                rowDiscountFee.visibility = View.VISIBLE
                tvSummaryDiscountLabel.text = if (discountCode.isBlank()) {
                    "Giảm giá"
                } else {
                    "Giảm giá ($discountCode)"
                }
                tvSummaryDiscountValue.text = "- ${formatVnd(discountAmount)}"
            } else {
                rowDiscountFee.visibility = View.GONE
            }

            // Hiển thị thông tin trả hàng + phí phạt trong Tóm tắt tài chính
            val returnCondition = order.optString("returnCondition", "")
            val penaltyAmount = order.optDouble("returnPenaltyAmount", 0.0)
            val returnNotes = order.optString("returnNotes", "")

            if (status == "returned" && returnCondition.isNotBlank()) {
                // Hiển thị tình trạng trả
                rowReturnCondition.visibility = View.VISIBLE
                tvSummaryReturnCondition.text = when (returnCondition) {
                    "normal" -> "Bình thường"
                    "dirty_damaged" -> "Bẩn / Hư hỏng"
                    "lost" -> "Mất đồ"
                    else -> returnCondition
                }

                // Hiển thị phí phạt nếu có
                if (penaltyAmount > 0.0) {
                    rowPenaltyFee.visibility = View.VISIBLE
                    tvSummaryPenaltyValue.text = formatVnd(penaltyAmount)
                } else {
                    rowPenaltyFee.visibility = View.GONE
                }

                // Hiển thị ghi chú nếu có
                if (returnNotes.isNotBlank()) {
                    rowReturnNotes.visibility = View.VISIBLE
                    tvSummaryReturnNotes.text = returnNotes
                } else {
                    rowReturnNotes.visibility = View.GONE
                }
            } else {
                rowReturnCondition.visibility = View.GONE
                rowPenaltyFee.visibility = View.GONE
                rowReturnNotes.visibility = View.GONE
            }

            if (status == "returned") {
                tvSummaryTotalValue.text = formatVnd(
                    (finalRentalPrice - discountAmount).coerceAtLeast(0.0) + penaltyAmount
                )
            } else if (order.optString("paidAt", "").isNotBlank()) {
                tvSummaryTotalValue.text = formatVnd(order.optDouble("paidAmount", finalTotalAmount))
            } else {
                tvSummaryTotalValue.text = formatVnd(finalTotalAmount) // finalRentalPrice + totalDeposit
            }
        }

        ImageUtils.bindImage(
            ivProductImage,
            firstProduct?.optString("imageUrl", "").orEmpty(),
            R.drawable.img_create_order_product_1
        )

        // Fix E2E: Xóa trắng danh sách cũ và render tất cả các nhóm sản phẩm còn lại
        productExtraListContainer.removeAllViews()
        for (i in 1 until groupedProducts.size) {
            val (p, qty) = groupedProducts[i]
            productExtraListContainer.addView(createExtraProductCard(p, qty))
        }
    }

    /**
     * Đơn hàng chỉ lưu tên và số điện thoại, nên tra customer theo số điện thoại
     * để cập nhật email và ảnh đại diện mới nhất trong card khách hàng.
     */
    private fun bindCustomerInfo(phone: String) {
        ivCustomerAvatar.imageTintList = ColorStateList.valueOf(getColor(R.color.text_secondary))
        ivCustomerAvatar.setPadding(dp(10), dp(10), dp(10), dp(10))
        ImageUtils.bindImage(
            ivCustomerAvatar,
            "",
            android.R.drawable.ic_menu_myplaces
        )

        if (phone.isBlank()) {
            tvCustomerEmail.text = "--"
            return
        }

        runApi(
            loadingMessage = null,
            request = { ApiClient.get("/customers?search=$phone") }
        ) { result ->
            val customers = JSONObject(result.body).optJSONArray("data") ?: JSONArray()
            for (index in 0 until customers.length()) {
                val customer = customers.optJSONObject(index) ?: continue
                if (customer.optString("phone", "") == phone) {
                    tvCustomerEmail.text = customer.optString("email", "").ifBlank { "--" }
                    val hasCustomerPhoto = CustomerImageUtils.bindAvatar(
                        ivCustomerAvatar,
                        customer.optString("avatar", ""),
                        customer.optString("id", ""),
                        customer.optString("phone", "")
                    )

                    if (hasCustomerPhoto) {
                        ivCustomerAvatar.imageTintList = null
                        ivCustomerAvatar.clearColorFilter()
                        ivCustomerAvatar.setPadding(0, 0, 0, 0)
                        ivCustomerAvatar.clipToOutline = true
                    }
                    return@runApi
                }
            }

            tvCustomerEmail.text = "--"
        }
    }

    private fun renderTimeline(
        order: JSONObject,
        status: String,
        pickupDate: String,
        returnDate: String,
        actualReturnDate: String
    ) {
        timelineStepsContainer.removeAllViews()

        addTimelineStep(
            marker = "\u2713",
            title = "Đã tạo đơn thuê",
            detail = timelineCreatedAt(order, pickupDate),
            active = true
        )

        val pickupDisplay = formatDate(pickupDate)
        val returnDisplay = formatDate(returnDate)
        when (status) {
            "pending" -> {
                addTimelineStep("•", "Đang xử lý nhận đồ", "Dự kiến nhận: $pickupDisplay", active = true)
                addTimelineStep("•", "Chờ khách sử dụng", "Dự kiến trả: $returnDisplay", active = false)
            }
            "cancelled" -> {
                addTimelineStep("✓", "Đã hủy đơn", "Sản phẩm đã được trả lại kho", active = true)
                addTimelineStep("•", "Không thuê", "Khách hàng hủy đơn", active = false)
            }
            "returned" -> {
                addTimelineStep("\u2713", "Đã nhận đồ", "Ngày nhận: $pickupDisplay", active = true)
                addTimelineStep("\u2713", "Đã trả đồ", "Ngày trả: $returnDisplay", active = true)
            }
            "overdue", "overdue_history" -> {
                addTimelineStep("\u2713", "Đã nhận đồ", "Ngày nhận: $pickupDisplay", active = true)
                addTimelineStep("!", "Quá hạn trả đồ", "Hạn trả: $returnDisplay", active = true)
            }
            else -> {
                addTimelineStep("\u2713", "Đã nhận đồ", "Ngày nhận: $pickupDisplay", active = true)
                addTimelineStep("•", "Khách đang sử dụng", "Dự kiến trả: $returnDisplay", active = true)
            }
        }
    }

    private fun addTimelineStep(marker: String, title: String, detail: String, active: Boolean) {
        val row = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = if (timelineStepsContainer.childCount == 0) 0 else dp(20)
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
        }

        row.addView(TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(22), LinearLayout.LayoutParams.WRAP_CONTENT)
            text = marker
            gravity = Gravity.CENTER
            setTextColor(getColor(if (active) R.color.brand_primary else R.color.text_secondary))
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
        })

        row.addView(LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            orientation = LinearLayout.VERTICAL

            addView(TextView(this@OrderDetailActivity).apply {
                text = title
                setTextColor(getColor(if (active) R.color.text_primary else R.color.text_secondary))
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
            })

            addView(TextView(this@OrderDetailActivity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(3) }
                text = detail
                setTextColor(getColor(R.color.text_secondary))
                textSize = 12f
            })
        })

        timelineStepsContainer.addView(row)
    }

    private fun statusLabel(status: String): String {
        if (status == "cancelled") return "\u0110\u00e3 h\u1ee7y"
        return when (status) {
            "returned" -> "Đã trả"
            "overdue", "overdue_history" -> "Quá hạn"
            "pending" -> "Tạo đơn"
            else -> "Đang thuê"
        }
    }

    private fun formatDate(value: String): String {
        val parts = value.split("-")
        return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else value
    }

    private fun formatDateCompact(value: String): String {
        val parts = value.split("-")
        return if (parts.size == 3) {
            "${parts[2].trimStart('0')}/${parts[1].trimStart('0')}/${parts[0]}"
        } else {
            value
        }
    }

    private fun returnDateSummary(pickupDate: String, returnDate: String, actualReturnDate: String, status: String): String {
        if (status == "cancelled") {
            return "Đơn đã hủy\nDự kiến trả: ${formatDate(returnDate)}"
        }

        if (status != "returned" || actualReturnDate.isBlank()) {
            return formatDate(returnDate)
        }

        // Neu khach tra dung ngay hen, hien ngay tra thuc te theo format ngan gon.
        if (dateDiffDays(returnDate, actualReturnDate) == 0L) {
            return "Trả đúng hạn: ${formatDateCompact(actualReturnDate)}"
        }

        return "Hạn: ${formatDate(returnDate)}\n${returnTimingLabel(returnDate, actualReturnDate)}: ${formatDate(actualReturnDate)}"
    }

    private fun returnTimingLabel(returnDate: String, actualReturnDate: String): String {
        val diffDays = dateDiffDays(returnDate, actualReturnDate)
        return when {
            diffDays == null -> "Trả thực tế"
            diffDays < 0 -> "Trả trước"
            diffDays > 0 -> "Trả muộn"
            else -> "Trả đúng hạn"
        }
    }

    private fun dateDiffDays(expectedDate: String, actualDate: String): Long? {
        return runCatching {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val expected = formatter.parse(expectedDate) ?: return@runCatching null
            val actual = formatter.parse(actualDate) ?: return@runCatching null
            (actual.time - expected.time) / (24L * 60L * 60L * 1000L)
        }.getOrNull()
    }

    private fun timelineCreatedAt(order: JSONObject, fallbackPickupDate: String): String {
        val raw = order.optString("createdAt", "")
        if (raw.isNotBlank() && raw.length >= 16) {
            val date = raw.substring(0, 10)
            return "${formatDate(date)} • ${raw.substring(11, 16)}"
        }
        return "${formatDate(fallbackPickupDate)} • 09:00"
    }

    private fun rentalDays(pickupDate: String, returnDate: String): Long {
        return runCatching {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val from = formatter.parse(pickupDate) ?: return@runCatching 1L
            val to = formatter.parse(returnDate) ?: return@runCatching 1L
            val diffMillis = to.time - from.time
            kotlin.math.max(1L, diffMillis / (24L * 60L * 60L * 1000L))
        }.getOrDefault(1)
    }

    private fun formatVnd(value: Double): String {
        return "${String.format("%,.0f", value).replace(",", ".")}đ"
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun createExtraProductCard(product: org.json.JSONObject, quantity: Int): android.view.View {
        val card = android.widget.LinearLayout(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14) }
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_form_card)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }

        val imageView = android.widget.ImageView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(dp(112), dp(148))
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(R.drawable.bg_product_image_box)
            com.example.project_android.utils.ImageUtils.bindImage(
                this,
                product.optString("imageUrl", "").orEmpty(),
                R.drawable.img_create_order_product_1
            )
        }
        card.addView(imageView)

        val detail = android.widget.LinearLayout(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(0, dp(148), 1f).apply {
                marginStart = dp(18)
            }
            orientation = android.widget.LinearLayout.VERTICAL
        }
        card.addView(detail)

        detail.addView(TextView(this).apply {
            text = "SKU: ${product.optString("id", "--")}"
            setTextColor(getColor(R.color.text_secondary))
            textSize = 10f
            letterSpacing = 0.12f
        }, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        detail.addView(TextView(this).apply {
            text = product.optString("name", "Sản phẩm thuê")
            setTextColor(getColor(R.color.text_primary))
            textSize = 16f
        }, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) })

        val sizeContainer = android.widget.LinearLayout(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
            orientation = android.widget.LinearLayout.HORIZONTAL
        }
        detail.addView(sizeContainer)

        sizeContainer.addView(TextView(this).apply {
            text = "SIZE: ${product.optString("size", "--")}"
            setTextColor(getColor(R.color.text_primary))
            textSize = 11f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setBackgroundResource(R.drawable.bg_create_order_size_chip)
            setPadding(dp(10), 0, dp(10), 0)
        }, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            dp(26)
        ))

        val qtyContainer = android.widget.LinearLayout(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        detail.addView(qtyContainer)

        qtyContainer.addView(TextView(this).apply {
            text = "SỐ LƯỢNG:"
            setTextColor(getColor(R.color.text_secondary))
            textSize = 10f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        qtyContainer.addView(TextView(this).apply {
            text = quantity.toString()
            setTextColor(getColor(R.color.text_primary))
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setBackgroundResource(R.drawable.bg_create_order_input)
        }, android.widget.LinearLayout.LayoutParams(dp(48), dp(28)).apply {
            marginStart = dp(8)
        })

        detail.addView(android.widget.Space(this), android.widget.LinearLayout.LayoutParams(dp(1), 0, 1f))

        detail.addView(TextView(this).apply {
            text = formatVnd(product.optDouble("rentalPrice", 0.0))
            setTextColor(getColor(R.color.brand_primary))
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.END
        }, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        return card
    }
}
