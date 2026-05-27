package com.example.project_android.view.activity

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.example.project_android.R
import com.example.project_android.model.dao.DiscountCodeDao
import com.example.project_android.model.dao.OrderDao
import com.example.project_android.model.database.DatabaseHelper
import com.example.project_android.navigation.AppNavigator
import com.example.project_android.navigation.AdminAvatarController
import com.example.project_android.network.ApiClient
import com.example.project_android.network.runApi
import com.example.project_android.utils.ImageUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class PaymentDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnConfirmPayment: AppCompatButton
    private lateinit var ivPaymentProductImage: ImageView
    private lateinit var tvPaymentOrderCode: TextView
    private lateinit var tvPaymentCustomerName: TextView
    private lateinit var tvPaymentCustomerPhone: TextView
    private lateinit var tvPaymentProductName: TextView
    private lateinit var tvPaymentProductSku: TextView
    private lateinit var tvPaymentRentalPeriod: TextView
    private lateinit var tvPaymentRentalLabel: TextView
    private lateinit var tvPaymentRentalValue: TextView
    private lateinit var tvPaymentDepositValue: TextView
    private lateinit var tvPaymentPickupDateValue: TextView
    private lateinit var tvPaymentActualReturnDateValue: TextView
    private lateinit var tvPaymentTotalValue: TextView
    private lateinit var tvPaymentDepositHint: TextView
    private lateinit var edtPaymentDiscountCode: EditText
    private lateinit var btnApplyPaymentDiscount: AppCompatButton
    private lateinit var tvPaymentDiscountMessage: TextView
    private lateinit var layoutPaymentDiscountApplied: LinearLayout
    private lateinit var tvPaymentDiscountValue: TextView

    private var currentOrderCode = ""
    private var currentCustomerPhone = ""
    private var currentRentalAmount = 0.0
    private var currentTotalBeforeDiscount = 0.0
    private var appliedDiscountCode = ""
    private var appliedDiscountAmount = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_detail)
        currentOrderCode = intent.getStringExtra("order_code").orEmpty()

        initViews()
        setupEvents()
        loadPaymentDetail()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment)
        ivPaymentProductImage = findViewById(R.id.ivPaymentProductImage)
        tvPaymentOrderCode = findViewById(R.id.tvPaymentOrderCode)
        tvPaymentCustomerName = findViewById(R.id.tvPaymentCustomerName)
        tvPaymentCustomerPhone = findViewById(R.id.tvPaymentCustomerPhone)
        tvPaymentProductName = findViewById(R.id.tvPaymentProductName)
        tvPaymentProductSku = findViewById(R.id.tvPaymentProductSku)
        tvPaymentRentalPeriod = findViewById(R.id.tvPaymentRentalPeriod)
        tvPaymentRentalLabel = findViewById(R.id.tvPaymentRentalLabel)
        tvPaymentRentalValue = findViewById(R.id.tvPaymentRentalValue)
        tvPaymentDepositValue = findViewById(R.id.tvPaymentDepositValue)
        tvPaymentPickupDateValue = findViewById(R.id.tvPaymentPickupDateValue)
        tvPaymentActualReturnDateValue = findViewById(R.id.tvPaymentActualReturnDateValue)
        tvPaymentTotalValue = findViewById(R.id.tvPaymentTotalValue)
        tvPaymentDepositHint = findViewById(R.id.tvPaymentDepositHint)
        edtPaymentDiscountCode = findViewById(R.id.edtPaymentDiscountCode)
        btnApplyPaymentDiscount = findViewById(R.id.btnApplyPaymentDiscount)
        tvPaymentDiscountMessage = findViewById(R.id.tvPaymentDiscountMessage)
        layoutPaymentDiscountApplied = findViewById(R.id.layoutPaymentDiscountApplied)
        tvPaymentDiscountValue = findViewById(R.id.tvPaymentDiscountValue)
    }

    private fun setupEvents() {
        AdminAvatarController.bind(this)
        btnBack.setOnClickListener { finish() }
        btnApplyPaymentDiscount.setOnClickListener { applyDiscountCode() }
        btnConfirmPayment.setOnClickListener { confirmPayment() }
    }

    private fun confirmPayment() {
        if (currentOrderCode.isBlank()) {
            Toast.makeText(this, "Không tìm thấy mã đơn thuê", Toast.LENGTH_SHORT).show()
            return
        }

        val enteredCode = edtPaymentDiscountCode.text.toString().trim().uppercase()
        if (enteredCode.isNotBlank() && enteredCode != appliedDiscountCode) {
            applyDiscountCode()
            Toast.makeText(this, "Vui lòng áp dụng mã trước khi thanh toán", Toast.LENGTH_SHORT).show()
            return
        }

        runApi(
            loadingMessage = "Đang đồng bộ thanh toán...",
            request = {
                // Chỉ khi xác nhận mới ghi mã và cộng thống kê giảm giá vào SQLite.
                ApiClient.post(
                    "/orders/$currentOrderCode/payment",
                    JSONObject()
                        .put("rentalAmount", currentRentalAmount)
                        .put("totalBeforeDiscount", currentTotalBeforeDiscount)
                        .put("discountCode", appliedDiscountCode)
                )
            }
        ) {
            Toast.makeText(this, "Đã xác nhận thanh toán", Toast.LENGTH_SHORT).show()
            AppNavigator.openOrdersWithStatus(this, "renting")
            finish()
        }
    }

    private fun loadPaymentDetail() {
        runApi(
            loadingMessage = null,
            request = { ApiClient.get("/orders") }
        ) { result ->
            val orders = JSONObject(result.body).optJSONArray("data") ?: JSONArray()
            val order = findTargetOrder(orders, currentOrderCode)
            if (order == null) {
                Toast.makeText(this, "Không tìm thấy đơn thuê", Toast.LENGTH_SHORT).show()
                finish()
                return@runApi
            }

            currentOrderCode = order.optString("code", currentOrderCode)
            bindPayment(order)
        }
    }

    private fun findTargetOrder(orders: JSONArray, code: String): JSONObject? {
        if (orders.length() == 0) return null
        if (code.isBlank()) return orders.optJSONObject(0)

        for (index in 0 until orders.length()) {
            val order = orders.optJSONObject(index) ?: continue
            if (order.optString("code", "") == code) return order
        }
        return orders.optJSONObject(0)
    }

    private fun bindPayment(order: JSONObject) {
        val pickupDate = order.optString("pickupDate", "")
        val returnDate = order.optString("returnDate", "")
        val products = order.optJSONArray("products")
            ?: order.optJSONArray("productIds")
            ?: JSONArray()
        val firstProduct = products.optJSONObject(0)
        val dailyRental = sumProducts(products, "rentalPrice").ifZero {
            firstProduct?.optDouble("rentalPrice", order.optDouble("totalAmount", 0.0))
                ?: order.optDouble("totalAmount", 0.0)
        }
        // Tiền cọc hoàn lại không tham gia tính giảm giá.
        val deposit = (sumProducts(products, "deposit").ifZero {
            firstProduct?.optDouble("deposit", 0.0) ?: 0.0
        }) * 0.5
        val days = rentalDays(pickupDate, returnDate)
        val rentalTotal = dailyRental * days
        val total = rentalTotal + deposit
        val customerName = order.optString("customerName", "Khách hàng")
        val customerPhone = order.optString("customerPhone", "--")
        currentCustomerPhone = customerPhone

        tvPaymentOrderCode.text = "Hóa đơn #${order.optString("code", "--")}"
        tvPaymentCustomerName.text = customerName
        tvPaymentCustomerPhone.text = customerPhone
        tvPaymentProductName.text = customerName
        tvPaymentProductSku.text = customerPhone
        tvPaymentRentalPeriod.visibility = View.GONE
        tvPaymentRentalLabel.text = "Phí thuê ($days ngày)"
        tvPaymentRentalValue.text = formatVnd(rentalTotal)
        tvPaymentDepositValue.text = formatVnd(deposit)
        tvPaymentPickupDateValue.text = formatDisplayDate(pickupDate)
        tvPaymentActualReturnDateValue.text = formatDisplayDate(returnDate)
        tvPaymentDepositHint.text = "Bao gồm ${formatVnd(deposit)} tiền cọc hoàn lại"

        currentRentalAmount = rentalTotal
        currentTotalBeforeDiscount = total
        appliedDiscountCode = order.optString("discountCode", "")
        appliedDiscountAmount = order.optDouble("discountAmount", 0.0)
        edtPaymentDiscountCode.setText(appliedDiscountCode)
        if (appliedDiscountCode.isNotBlank()) {
            showAppliedDiscount("Đã áp dụng mã $appliedDiscountCode")
        } else {
            hideAppliedDiscount()
        }

        val paymentConfirmed = order.optString("paidAt", "").isNotBlank()
        edtPaymentDiscountCode.isEnabled = !paymentConfirmed
        btnApplyPaymentDiscount.isEnabled = !paymentConfirmed
        btnConfirmPayment.isEnabled = !paymentConfirmed
        if (paymentConfirmed) {
            btnConfirmPayment.text = "Đã xác nhận thanh toán"
        }
        updatePaymentTotal()
        bindCustomerAvatar(customerPhone)
    }

    private fun applyDiscountCode() {
        val code = edtPaymentDiscountCode.text.toString().trim()
        if (code.isBlank()) {
            appliedDiscountCode = ""
            appliedDiscountAmount = 0.0
            hideAppliedDiscount()
            updatePaymentTotal()
            return
        }

        val database = DatabaseHelper(this).readableDatabase
        val customerTotalSpent = OrderDao(database)
            .summaryByCustomerPhone(currentCustomerPhone)
            .optDouble("totalSpent", 0.0)
        val result = DiscountCodeDao(database)
            .calculateForCustomer(code, currentRentalAmount, customerTotalSpent)
        if (!result.isValid) {
            appliedDiscountCode = ""
            appliedDiscountAmount = 0.0
            layoutPaymentDiscountApplied.visibility = View.GONE
            showDiscountMessage(result.errorMessage.orEmpty(), true)
            updatePaymentTotal()
            return
        }

        appliedDiscountCode = result.discount?.code.orEmpty()
        appliedDiscountAmount = result.amount
        edtPaymentDiscountCode.setText(appliedDiscountCode)
        showAppliedDiscount("Áp dụng thành công mã $appliedDiscountCode")
        updatePaymentTotal()
    }

    private fun showAppliedDiscount(message: String) {
        layoutPaymentDiscountApplied.visibility = View.VISIBLE
        tvPaymentDiscountValue.text = "- ${formatVnd(appliedDiscountAmount)}"
        showDiscountMessage(message, false)
    }

    private fun hideAppliedDiscount() {
        layoutPaymentDiscountApplied.visibility = View.GONE
        tvPaymentDiscountMessage.visibility = View.GONE
    }

    private fun showDiscountMessage(message: String, isError: Boolean) {
        tvPaymentDiscountMessage.visibility = View.VISIBLE
        tvPaymentDiscountMessage.text = message
        tvPaymentDiscountMessage.setTextColor(
            ContextCompat.getColor(
                this,
                if (isError) R.color.discount_error_text else R.color.discount_success_text
            )
        )
    }

    private fun updatePaymentTotal() {
        tvPaymentTotalValue.text = formatVnd(
            (currentTotalBeforeDiscount - appliedDiscountAmount).coerceAtLeast(0.0)
        )
    }

    private fun bindCustomerAvatar(phone: String) {
        ivPaymentProductImage.setImageResource(android.R.drawable.ic_menu_myplaces)
        if (phone.isBlank()) return

        runApi(
            loadingMessage = null,
            request = { ApiClient.get("/customers?search=$phone") }
        ) { result ->
            val customers = JSONObject(result.body).optJSONArray("data") ?: JSONArray()
            for (index in 0 until customers.length()) {
                val customer = customers.optJSONObject(index) ?: continue
                if (customer.optString("phone", "") == phone) {
                    ImageUtils.bindImage(
                        ivPaymentProductImage,
                        customer.optString("avatar", ""),
                        android.R.drawable.ic_menu_myplaces
                    )
                    return@runApi
                }
            }
        }
    }

    private fun rentalDays(pickupDate: String, returnDate: String): Long {
        return runCatching {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val from = formatter.parse(pickupDate) ?: return@runCatching 1L
            val to = formatter.parse(returnDate) ?: return@runCatching 1L
            val diffMillis = to.time - from.time
            kotlin.math.max(1L, diffMillis / (24L * 60L * 60L * 1000L))
        }.getOrDefault(1L)
    }

    private fun sumProducts(products: JSONArray, key: String): Double {
        var total = 0.0
        for (index in 0 until products.length()) {
            total += products.optJSONObject(index)?.optDouble(key, 0.0) ?: 0.0
        }
        return total
    }

    private fun Double.ifZero(fallback: () -> Double): Double {
        return if (this == 0.0) fallback() else this
    }

    private fun formatDisplayDate(value: String): String {
        val parts = value.split("-")
        return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else value.ifBlank { "--" }
    }

    private fun formatVnd(value: Double): String {
        return "${String.format(Locale.US, "%,.0f", value).replace(",", ".")}đ"
    }
}
