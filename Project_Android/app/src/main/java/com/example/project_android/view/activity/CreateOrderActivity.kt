package com.example.project_android.view.activity

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.project_android.R
import com.example.project_android.navigation.AdminAvatarController
import com.example.project_android.navigation.AppNavigator
import com.example.project_android.network.ApiClient
import com.example.project_android.network.runApi
import com.example.project_android.utils.CurrencyUtils
import com.example.project_android.utils.CustomerImageUtils
import com.example.project_android.utils.ImageUtils
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

private data class SelectedProduct(
    val id: String,
    val name: String,
    val code: String,
    var size: String,
    val quantity: Int,
    val rentalPrice: Double,
    val deposit: Double,
    val imageUrl: String,
    var selectedQuantity: Int = 1
)

class CreateOrderActivity : AppCompatActivity() {

    // ===== EDIT MODE: nếu có edit_order_code -> đang sửa đơn, không phải tạo mới =====
    private var editOrderCode: String? = null
    private val isEditMode get() = !editOrderCode.isNullOrBlank()

    private var waitingForAddedProduct = false
    private var lastHandledAddedProductId: String? = null
    private var suppressCustomerSearch = false

    private val addProductLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val cachedProductId = consumeReturnedProductId()
            val productId = result.data?.getStringExtra("product_id")
                ?: cachedProductId
            waitingForAddedProduct = false
            handleAddedProductResult(productId)
        } else {
            waitingForAddedProduct = false
        }
    }

    private val addCustomerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val customerId = result.data?.getStringExtra("customer_id")
                ?: consumeReturnedCustomerId()
            handleAddedCustomerResult(customerId)
        }
    }

    private lateinit var btnClose: ImageView
    private lateinit var edtSearchCustomer: EditText
    private lateinit var customerEleanor: TextView
    private lateinit var customerMarcus: TextView
    private lateinit var customerSearchResultsContainer: LinearLayout
    private lateinit var customerInfoCard: LinearLayout
    private lateinit var tvSelectedCustomerName: TextView
    private lateinit var tvSelectedCustomerPhone: TextView
    private lateinit var tvSelectedCustomerEmail: TextView
    private lateinit var btnAddProduct: TextView
    private lateinit var productCard1: LinearLayout
    private lateinit var productExtraListContainer: LinearLayout
    private lateinit var imgSelectedProduct: ImageView
    private lateinit var tvSelectedProductName: TextView
    private lateinit var tvSelectedProductCode: TextView
    private lateinit var etSelectedProductQuantity: EditText
    private lateinit var tvSelectedProductSize: TextView
    private lateinit var tvSelectedProductPrice: TextView
    private lateinit var btnRemoveSelectedProduct: TextView
    private lateinit var cardPickupDate: LinearLayout
    private lateinit var cardReturnDate: LinearLayout
    private lateinit var tvPickupDate: TextView
    private lateinit var tvReturnDate: TextView
    private lateinit var tvRentalDays: TextView
    private lateinit var tvRentalSubtotal: TextView
    private lateinit var tvDeposit: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnConfirmBooking: Button
    private lateinit var tvScreenTitle: TextView

    private var selectedCustomerName = "Nguyễn Thị Thu Hà"
    private var selectedCustomerPhone = "0901112222"
    private var selectedCustomerEmail = "thuha.n@vidu.vn"
    private var selectedCustomerId = "c1"
    private var selectedProductId: String? = null
    private var selectedRentalPrice = 0.0
    private var selectedDeposit = 0.0
    private val selectedProducts = mutableListOf<SelectedProduct>()
    private var updatingQuantityInput = false
    private var pickupDate = "2023-10-12"
    private var returnDate = "2023-10-15"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_order)

        // E2E EDIT MODE: detect xem user đang tạo mới hay sửa đơn cũ
        editOrderCode = intent.getStringExtra("edit_order_code")

        initViews()
        applyCustomerFromIntent()
        setupEvents()

        // Nếu đang edit -> load dữ liệu đơn cũ lên form
        if (isEditMode) {
            loadOrderForEdit(editOrderCode!!)
        }
    }

    override fun onResume() {
        super.onResume()
        if (waitingForAddedProduct) {
            val productId = consumeReturnedProductId()
            if (productId.isNullOrBlank()) {
                waitingForAddedProduct = false
            } else {
                handleAddedProductResult(productId)
            }
        }
    }

    private fun initViews() {
        btnClose = findViewById(R.id.btnClose)
        edtSearchCustomer = findViewById(R.id.edtSearchCustomer)
        customerEleanor = findViewById(R.id.customerEleanor)
        customerMarcus = findViewById(R.id.customerMarcus)
        customerSearchResultsContainer = findViewById(R.id.customerSearchResultsContainer)
        customerInfoCard = findViewById(R.id.customerInfoCard)
        tvSelectedCustomerName = findViewById(R.id.tvSelectedCustomerName)
        tvSelectedCustomerPhone = findViewById(R.id.tvSelectedCustomerPhone)
        tvSelectedCustomerEmail = findViewById(R.id.tvSelectedCustomerEmail)
        btnAddProduct = findViewById(R.id.btnAddProduct)
        productCard1 = findViewById(R.id.productCard1)
        productExtraListContainer = findViewById(R.id.productExtraListContainer)
        imgSelectedProduct = findViewById(R.id.imgSelectedProduct)
        tvSelectedProductName = findViewById(R.id.tvSelectedProductName)
        tvSelectedProductCode = findViewById(R.id.tvSelectedProductCode)
        etSelectedProductQuantity = findViewById(R.id.etSelectedProductQuantity)
        configureQuantityInput(etSelectedProductQuantity)
        etSelectedProductQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (updatingQuantityInput) return
                val product = selectedProducts.firstOrNull() ?: return
                val quantity = s?.toString()?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                if (product.selectedQuantity != quantity) {
                    product.selectedQuantity = quantity
                    updatePaymentSummary()
                }
            }
        })
        tvSelectedProductSize = findViewById(R.id.tvSelectedProductSize)
        tvSelectedProductPrice = findViewById(R.id.tvSelectedProductPrice)
        btnRemoveSelectedProduct = findViewById(R.id.btnRemoveSelectedProduct)
        cardPickupDate = findViewById(R.id.cardPickupDate)
        cardReturnDate = findViewById(R.id.cardReturnDate)
        tvPickupDate = findViewById(R.id.tvPickupDate)
        tvReturnDate = findViewById(R.id.tvReturnDate)
        tvRentalDays = findViewById(R.id.tvRentalDays)
        tvRentalSubtotal = findViewById(R.id.tvRentalSubtotal)
        tvDeposit = findViewById(R.id.tvDeposit)
        tvTotal = findViewById(R.id.tvTotal)
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking)
        tvScreenTitle = findViewById(R.id.tvScreenTitle)

        resetCreateOrderState()
        edtSearchCustomer.setText(selectedCustomerName)
        updateCustomerInfo()
        clearSelectedProduct(showToast = false)
        updateDateLabels()
        updatePaymentSummary()

        // E2E EDIT UI: đổi tiêu đề + nút bấm nếu đang ở chế độ sửa
        if (isEditMode) {
            tvScreenTitle.text = "Sửa đơn thuê"
            btnConfirmBooking.text = "Cập nhật đơn thuê"
        }
    }

    private fun applyCustomerFromIntent() {
        val customerId = intent.getStringExtra("customer_id")
        val customerName = intent.getStringExtra("customer_name")
        val customerPhone = intent.getStringExtra("customer_phone")
        val customerEmail = intent.getStringExtra("customer_email")

        if (!customerName.isNullOrBlank() && !customerPhone.isNullOrBlank()) {
            selectedCustomerId = customerId?.takeIf { it.isNotBlank() } ?: selectedCustomerId
            selectedCustomerName = customerName
            selectedCustomerPhone = customerPhone
            selectedCustomerEmail = customerEmail?.takeIf { it.isNotBlank() } ?: "Chưa có email"
            edtSearchCustomer.setText(customerName)
            updateCustomerInfo()
        }
    }

    private fun resetCreateOrderState() {
        // RESET CREATE ORDER: man tao don moi phai bat dau rong, user tu chon khach + san pham.
        selectedCustomerName = ""
        selectedCustomerPhone = ""
        selectedCustomerEmail = ""
        selectedCustomerId = ""
        selectedProductId = null
        selectedRentalPrice = 0.0
        selectedDeposit = 0.0
        selectedProducts.clear()
        pickupDate = todayStorageDate()
        returnDate = ""
    }

    private fun setupEvents() {
        AdminAvatarController.bind(this)
        btnClose.setOnClickListener { finish() }
        setupCustomerSearchInput()

        customerInfoCard.setOnClickListener {
            AppNavigator.openCustomerDetail(this, selectedCustomerId)
        }

        btnAddProduct.setOnClickListener {
            addProductLauncher.launch(
                Intent(this, ProductActivity::class.java)
                    .putExtra("select_product_for_order", true)
            )
        }

        productCard1.setOnClickListener {
            openSelectedProductDetailAt(0)
        }

        btnRemoveSelectedProduct.setOnClickListener {
            removeSelectedProductAt(0)
        }

        customerEleanor.setOnClickListener {
            selectedCustomerName = "Nguyễn Thị Thu Hà"
            selectedCustomerPhone = "0901112222"
            selectedCustomerId = "c1"
            selectedCustomerEmail = "thuha.n@vidu.vn"
            edtSearchCustomer.setText(selectedCustomerName)
            updateCustomerInfo()
            Toast.makeText(this, "Đã chọn khách quen", Toast.LENGTH_SHORT).show()
        }

        customerMarcus.setOnClickListener {
            openAddCustomerForOrder()
        }

        customerEleanor.setOnClickListener {
            searchExistingCustomers()
        }

        cardPickupDate.setOnClickListener {
            showRentalDatePicker(isPickupDate = true)
            return@setOnClickListener
            pickupDate = "2023-10-12"
            updateDateLabels()
            updatePaymentSummary()
            Toast.makeText(this, "Ngày thuê: ${formatDisplayDate(pickupDate)}", Toast.LENGTH_SHORT).show()
        }

        cardReturnDate.setOnClickListener {
            showRentalDatePicker(isPickupDate = false)
            return@setOnClickListener
            returnDate = "2023-10-15"
            updateDateLabels()
            updatePaymentSummary()
            Toast.makeText(this, "Ngày trả: ${formatDisplayDate(returnDate)}", Toast.LENGTH_SHORT).show()
        }

        // E2E SUBMIT: bấm nút -> gọi create hoặc update tùy mode
        btnConfirmBooking.setOnClickListener {
            if (isEditMode) updateOrder() else createOrder()
        }
    }

    private fun updateCustomerInfo() {
        // E2E CUSTOMER INFO: dữ liệu khách đang chọn được render lại trên card trước khi tạo order.
        customerInfoCard.visibility = if (selectedCustomerName.isBlank() && selectedCustomerPhone.isBlank()) {
            View.GONE
        } else {
            View.VISIBLE
        }
        tvSelectedCustomerName.text = selectedCustomerName
        tvSelectedCustomerPhone.text = selectedCustomerPhone
        tvSelectedCustomerEmail.text = selectedCustomerEmail.ifBlank { "Chua co email" }
    }

    private fun setupCustomerSearchInput() {
        // E2E LIVE CUSTOMER SEARCH: user go ten khong dau/viet thuong -> hien list khach da co ngay tren man tao don.
        edtSearchCustomer.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (suppressCustomerSearch) return

                val keyword = s?.toString()?.trim().orEmpty()
                if (keyword.isBlank()) {
                    customerSearchResultsContainer.removeAllViews()
                    customerSearchResultsContainer.visibility = View.GONE
                    return
                }

                loadCustomersForSearch(openAddWhenEmpty = false)
            }
        })
    }

    private fun loadCustomersForSearch(openAddWhenEmpty: Boolean) {
        // E2E CUSTOMER FILTER: lay danh sach khach -> filter trong app bang chuoi lower-case + bo dau.
        val keyword = edtSearchCustomer.text.toString().trim()
        val normalizedKeyword = normalizeSearchText(keyword)

        if (normalizedKeyword.isBlank()) {
            customerSearchResultsContainer.removeAllViews()
            customerSearchResultsContainer.visibility = View.GONE
            return
        }

        runApi(
            loadingMessage = null,
            request = { ApiClient.get("/customers") }
        ) { result ->
            val customers = JSONObject(result.body).getJSONArray("data")
            val matchedCustomers = JSONArray()

            for (i in 0 until customers.length()) {
                val customer = customers.getJSONObject(i)
                val searchableText = listOf(
                    customer.optString("fullName", ""),
                    customer.optString("phone", ""),
                    customer.optString("email", "")
                ).joinToString(" ")

                if (normalizeSearchText(searchableText).contains(normalizedKeyword)) {
                    matchedCustomers.put(customer)
                }
            }

            if (matchedCustomers.length() == 0) {
                customerSearchResultsContainer.removeAllViews()
                customerSearchResultsContainer.visibility = View.GONE
                if (openAddWhenEmpty) {
                    Toast.makeText(this, "Khong co khach hang phu hop, hay them moi", Toast.LENGTH_SHORT).show()
                    openAddCustomerForOrder()
                }
                return@runApi
            }

            renderCustomerSearchResults(matchedCustomers)
        }
    }

    private fun renderCustomerSearchResults(customers: JSONArray) {
        // E2E CUSTOMER LIST: ket qua search dung item_customer nhu man Khach hang, bam item thi bind vao card duoi.
        customerSearchResultsContainer.removeAllViews()
        customerSearchResultsContainer.visibility = View.VISIBLE

        for (i in 0 until customers.length()) {
            customerSearchResultsContainer.addView(createCustomerSearchItem(customers.getJSONObject(i)))
        }
    }

    private fun createCustomerSearchItem(customer: JSONObject): View {
        val itemView = LayoutInflater.from(this).inflate(
            R.layout.item_customer,
            customerSearchResultsContainer,
            false
        )

        val name = customer.optString("fullName", "Khach hang")
        val email = customer.optString("email", "")
        val phone = customer.optString("phone", "")
        val id = customer.optString("id", customer.optString("_id", ""))
        val status = customer.optString("status", "active")
        val dressSize = customer.optString("dressShirtSize", customer.optString("dressSize", "")).ifEmpty { "--" }
        val shoeSize = customer.optString("shoeSize", "").ifEmpty { "--" }

        itemView.findViewById<TextView>(R.id.tvName).text = name
        itemView.findViewById<TextView>(R.id.tvEmail).text = email
        itemView.findViewById<TextView>(R.id.tvPhone).text = phone
        itemView.findViewById<TextView>(R.id.tvDressSize).text = "VAY/AO: $dressSize"
        itemView.findViewById<TextView>(R.id.tvShoeSize).text = "GIAY: $shoeSize"
        itemView.findViewById<ImageView>(R.id.btnDelete).visibility = View.GONE
        val avatar = itemView.findViewById<ImageView>(R.id.imgCustomerAvatar)
        if (CustomerImageUtils.bindAvatar(avatar, customer.optString("avatar", ""), id, phone)) {
            avatar.imageTintList = null
            avatar.clearColorFilter()
            avatar.setPadding(0, 0, 0, 0)
            avatar.clipToOutline = true
        }

        val tvStatus = itemView.findViewById<TextView>(R.id.tvStatus)
        if (status == "active") {
            tvStatus.text = "DANG HOAT DONG"
            tvStatus.setBackgroundResource(R.drawable.bg_status_ready)
            tvStatus.setTextColor(android.graphics.Color.parseColor("#4432E6"))
        } else {
            tvStatus.text = "NGUNG HOAT DONG"
            tvStatus.setBackgroundResource(R.drawable.bg_status_neutral)
            tvStatus.setTextColor(android.graphics.Color.parseColor("#747474"))
        }

        itemView.setOnClickListener {
            applySelectedCustomer(customer)
            customerSearchResultsContainer.removeAllViews()
            customerSearchResultsContainer.visibility = View.GONE
            Toast.makeText(this, "Da chon khach hang", Toast.LENGTH_SHORT).show()
        }

        return itemView
    }

    private fun normalizeSearchText(value: String): String {
        val withoutAccent = Normalizer.normalize(value.lowercase(Locale("vi", "VN")), Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace("đ", "d")
        return withoutAccent.trim()
    }

    private fun searchExistingCustomers() {
        loadCustomersForSearch(openAddWhenEmpty = true)
        return
        // E2E CUSTOMER SEARCH: user nhap ten/SDT/email -> bam Khach quen -> tim khach da co trong SQLite.
        val keyword = edtSearchCustomer.text.toString().trim()
        val query = if (keyword.isBlank()) {
            ""
        } else {
            "?search=${URLEncoder.encode(keyword, "UTF-8")}"
        }

        runApi(
            loadingMessage = "Dang tim khach hang...",
            request = { ApiClient.get("/customers$query") }
        ) { result ->
            val customers = JSONObject(result.body).getJSONArray("data")

            if (customers.length() == 0) {
                Toast.makeText(this, "Khong co khach hang phu hop, hay them moi", Toast.LENGTH_SHORT).show()
                AppNavigator.openAddCustomer(this)
                return@runApi
            }

            if (customers.length() == 1) {
                applySelectedCustomer(customers.getJSONObject(0))
                Toast.makeText(this, "Da chon khach quen", Toast.LENGTH_SHORT).show()
                return@runApi
            }

            showCustomerPicker(customers)
        }
    }

    private fun showCustomerPicker(customers: JSONArray) {
        // E2E CUSTOMER PICKER: co nhieu ket qua -> user chon dung khach -> render vao card thong tin.
        val labels = Array(customers.length()) { index ->
            val customer = customers.getJSONObject(index)
            val name = customer.optString("fullName", "Khach hang")
            val phone = customer.optString("phone", "Chua co SDT")
            "$name\n$phone"
        }

        AlertDialog.Builder(this)
            .setTitle("Chon khach quen")
            .setItems(labels) { dialog, which ->
                applySelectedCustomer(customers.getJSONObject(which))
                Toast.makeText(this, "Da chon khach quen", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Them moi") { dialog, _ ->
                dialog.dismiss()
                openAddCustomerForOrder()
            }
            .show()
    }

    private fun applySelectedCustomer(customer: JSONObject) {
        // E2E CUSTOMER SELECT: customer tu SQLite -> cap nhat state -> card + request tao order dung khach.
        selectedCustomerId = customer.optString("_id", customer.optString("id", selectedCustomerId))
        selectedCustomerName = customer.optString("fullName", selectedCustomerName)
        selectedCustomerPhone = customer.optString("phone", selectedCustomerPhone)
        selectedCustomerEmail = customer.optString("email", "").takeIf { it.isNotBlank() } ?: "Chua co email"
        suppressCustomerSearch = true
        edtSearchCustomer.setText(selectedCustomerName)
        edtSearchCustomer.setSelection(edtSearchCustomer.text.length)
        suppressCustomerSearch = false
        updateCustomerInfo()
    }

    private fun openAddCustomerForOrder() {
        // E2E ADD CUSTOMER: user bam Them moi -> tao khach -> quay lai man tao don de bind vao card thong tin.
        addCustomerLauncher.launch(
            Intent(this, AddCustomerActivity::class.java)
                .putExtra("return_customer_to_order", true)
        )
    }

    private fun handleAddedCustomerResult(customerId: String?) {
        if (customerId.isNullOrBlank()) return

        runApi(
            loadingMessage = "Dang tai khach hang vua them...",
            request = { ApiClient.get("/customers/$customerId/detail") }
        ) { result ->
            val customer = JSONObject(result.body)
                .getJSONObject("data")
                .getJSONObject("customer")
            applySelectedCustomer(customer)
            customerSearchResultsContainer.removeAllViews()
            customerSearchResultsContainer.visibility = View.GONE
            Toast.makeText(this, "Da them khach hang vao don thue", Toast.LENGTH_SHORT).show()
        }
    }

    private fun consumeReturnedCustomerId(): String? {
        val prefs = getSharedPreferences("create_order_return", MODE_PRIVATE)
        val customerId = prefs.getString("added_customer_id", null)
        if (!customerId.isNullOrBlank()) {
            prefs.edit().remove("added_customer_id").apply()
        }
        return customerId
    }

    private fun handleAddedProductResult(productId: String?) {
        // E2E ADD PRODUCT RESULT: quay lại từ màn thêm sản phẩm -> ưu tiên load đúng product_id vừa tạo.
        if (productId.isNullOrBlank()) return
        lastHandledAddedProductId = productId
        waitingForAddedProduct = false
        loadProductById(productId)
    }

    private fun consumeReturnedProductId(): String? {
        // Fallback khi ActivityResult không trả data: đọc product_id vừa lưu từ AddProductActivity.
        val prefs = getSharedPreferences("create_order_return", MODE_PRIVATE)
        val productId = prefs.getString("added_product_id", null)
        if (!productId.isNullOrBlank()) {
            prefs.edit().remove("added_product_id").apply()
        }
        return productId
    }

    private fun showProductPickerDialog() {
        runApi(
            loadingMessage = "Đang tải danh sách sản phẩm...",
            request = { ApiClient.get("/products?sort=newest") }
        ) { result ->
            val products = JSONObject(result.body).getJSONArray("data")
            if (products.length() == 0) {
                Toast.makeText(this, "Chưa có sản phẩm trong kho", Toast.LENGTH_SHORT).show()
                return@runApi
            }

            val dialog = AlertDialog.Builder(this)
                .setTitle("Chọn sản phẩm cho đơn thuê")
                .create()

            val listContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(12), dp(18), dp(12))
            }

            for (index in 0 until products.length()) {
                listContainer.addView(createProductPickerItem(products.getJSONObject(index), dialog))
            }

            dialog.setView(ScrollView(this).apply { addView(listContainer) })
            dialog.show()
        }
    }

    private fun createProductPickerItem(product: JSONObject, dialog: AlertDialog): LinearLayout {
        val quantity = product.optInt("quantity", 0)
        val canRent = quantity > 0

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            alpha = if (canRent) 1f else 0.55f
            isEnabled = canRent
            isClickable = canRent
            setPadding(0, dp(10), 0, dp(10))
            setOnClickListener {
                dialog.dismiss()
                showSizePickerForNewProduct(product)
            }

            val image = ImageView(this@CreateOrderActivity).apply {
                setBackgroundResource(R.drawable.bg_product_image_box)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            addView(image, LinearLayout.LayoutParams(dp(58), dp(76)))
            bindProductImage(image, product.optString("imageUrl", ""))

            addView(LinearLayout(this@CreateOrderActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(this@CreateOrderActivity).apply {
                    text = product.optString("name", "Sản phẩm")
                    setTextColor(getColor(R.color.text_primary))
                    textSize = 15f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    maxLines = 2
                })
                addView(TextView(this@CreateOrderActivity).apply {
                    text = "${stockLabel(quantity)}"
                    setTextColor(getColor(if (canRent) R.color.text_secondary else R.color.accent_magenta))
                    textSize = 12f
                    setPadding(0, dp(4), 0, 0)
                })
                addView(TextView(this@CreateOrderActivity).apply {
                    text = CurrencyUtils.formatVnd(product.optDouble("rentalPrice", 0.0))
                    setTextColor(getColor(R.color.brand_primary))
                    textSize = 14f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(0, dp(4), 0, 0)
                })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(12)
            })
        }
    }

    private fun showSizePickerForNewProduct(product: JSONObject) {
        val sizes = arrayOf("S", "M", "L", "XL", "XXL")
        AlertDialog.Builder(this)
            .setTitle("Chọn SIZE cho ${product.optString("name", "Sản phẩm")}")
            .setItems(sizes) { _, which ->
                product.put("size", sizes[which])
                addSelectedProduct(product, showToast = true)
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun loadFirstProduct(index: Int = 0) {
        // E2E PRODUCT: lấy productId thật từ SQLite -> bind lên card -> request tạo order có reference hợp lệ.
        runApi(
            loadingMessage = "Đang tải sản phẩm...",
            request = { ApiClient.get("/products") }
        ) { result ->
            val products = JSONObject(result.body).getJSONArray("data")
            if (products.length() == 0) {
                clearSelectedProduct(showToast = false)
                Toast.makeText(this, "Chưa có sản phẩm trong database", Toast.LENGTH_SHORT).show()
                return@runApi
            }

            val safeIndex = index.coerceAtMost(products.length() - 1)
            val product = products.getJSONObject(safeIndex)
            addSelectedProduct(product, replaceExisting = true, showToast = false)
            return@runApi
            selectedProductId = product.optString("_id", product.optString("id"))
            selectedRentalPrice = product.optDouble("rentalPrice", 0.0)
            selectedDeposit = product.optDouble("deposit", 0.0)

            tvSelectedProductName.text = product.optString("name", "Váy Lụa Silk Midnight")
            tvSelectedProductCode.text = "ID: ${product.optString("id", "SKU-2024-089").take(12)}"
            tvSelectedProductSize.text = "SIZE: ${product.optString("size", "M").substringBefore(",")}"
            tvSelectedProductPrice.text = CurrencyUtils.formatVnd(selectedRentalPrice)

            bindProductImage(imgSelectedProduct, product.optString("imageUrl", ""))

            updatePaymentSummary()
            Toast.makeText(this, "Đã chọn ${product.optString("name")}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadNewestProduct() {
        // E2E ADD PRODUCT: sau khi them do moi -> lay san pham moi nhat trong SQLite -> hien thi vao card thue.
        runApi(
            loadingMessage = "Đang tải sản phẩm mới...",
            request = { ApiClient.get("/products?sort=newest") }
        ) { result ->
            val products = JSONObject(result.body).getJSONArray("data")
            if (products.length() == 0) {
                clearSelectedProduct(showToast = false)
                Toast.makeText(this, "Chưa có sản phẩm trong database", Toast.LENGTH_SHORT).show()
                return@runApi
            }

            val product = products.getJSONObject(0)
            bindSelectedProduct(product)
            Toast.makeText(this, "Đã thêm ${product.optString("name")} vào đơn thuê", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProductById(productId: String) {
        // E2E ADD PRODUCT: AddProductActivity tra product_id -> man tao don load dung san pham vua them.
        runApi(
            loadingMessage = "Đang tải sản phẩm vừa thêm...",
            request = { ApiClient.get("/products/$productId/detail") }
        ) { result ->
            val product = JSONObject(result.body)
                .getJSONObject("data")
                .getJSONObject("product")
            bindSelectedProduct(product)
            Toast.makeText(this, "Đã thêm ${product.optString("name")} vào đơn thuê", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindSelectedProduct(product: JSONObject) {
        // E2E PRODUCT CARD: du lieu product tu SQLite -> render len card san pham thue.
        addSelectedProduct(product)
        return
        productCard1.visibility = View.VISIBLE
        selectedProductId = product.optString("_id", product.optString("id"))
        selectedRentalPrice = product.optDouble("rentalPrice", 0.0)
        selectedDeposit = product.optDouble("deposit", 0.0)

        tvSelectedProductName.text = product.optString("name", "Váy Lụa Silk Midnight")
        tvSelectedProductCode.text = "ID: ${product.optString("id", "SKU-2024-089").take(12)}"
        tvSelectedProductSize.text = "SIZE: ${product.optString("size", "M").substringBefore(",")}"
        tvSelectedProductPrice.text = CurrencyUtils.formatVnd(selectedRentalPrice)

        bindProductImage(imgSelectedProduct, product.optString("imageUrl", ""))

        updatePaymentSummary()
    }

    private fun addSelectedProduct(
        product: JSONObject,
        replaceExisting: Boolean = false,
        showToast: Boolean = false
    ) {
        // E2E PRODUCT LIST: them san pham vao danh sach thue, khong ghi de san pham da co.
        val item = product.toSelectedProduct()
        if (item.id.isBlank()) return

        if (replaceExisting) {
            selectedProducts.clear()
        } else {
            val existing = selectedProducts.find { it.id == item.id && it.size == item.size }
            if (existing != null) {
                existing.selectedQuantity += 1
                renderSelectedProducts()
                if (showToast) {
                    Toast.makeText(this, "Đã tăng số lượng ${item.name} (${item.size})", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }

        selectedProducts.add(item)
        renderSelectedProducts()

        if (showToast) {
            Toast.makeText(this, "Đã thêm ${item.name} vào đơn thuê", Toast.LENGTH_SHORT).show()
        }
    }

    private fun JSONObject.toSelectedProduct(): SelectedProduct {
        val productId = optString("_id", optString("id"))
        return SelectedProduct(
            id = productId,
            name = optString("name", "Váy Lụa Silk Midnight"),
            code = optString("id", productId).take(12),
            size = optString("size", "M").substringBefore(","),
            quantity = optInt("quantity", 0),
            rentalPrice = optDouble("rentalPrice", 0.0),
            deposit = optDouble("deposit", 0.0),
            imageUrl = optString("imageUrl", "")
        )
    }

    private fun renderSelectedProducts() {
        productExtraListContainer.removeAllViews()

        if (selectedProducts.isEmpty()) {
            selectedProductId = null
            selectedRentalPrice = 0.0
            selectedDeposit = 0.0
            productCard1.visibility = View.GONE
            updatePaymentSummary()
            return
        }

        productCard1.visibility = View.VISIBLE
        bindFirstProductCard(selectedProducts.first())
        selectedProducts.drop(1).forEachIndexed { index, product ->
            productExtraListContainer.addView(createExtraProductCard(product, index + 1))
        }
        updatePaymentSummary()
    }

    private fun bindFirstProductCard(product: SelectedProduct) {
        selectedProductId = product.id
        selectedRentalPrice = product.rentalPrice
        selectedDeposit = product.deposit

        tvSelectedProductName.text = product.name
        tvSelectedProductCode.text = "ID: ${product.code}"
        updatingQuantityInput = true
        etSelectedProductQuantity.setText(product.selectedQuantity.toString())
        etSelectedProductQuantity.setSelection(etSelectedProductQuantity.text.length)
        updatingQuantityInput = false
        tvSelectedProductSize.text = "SIZE: ${product.size}"
        tvSelectedProductSize.setOnClickListener { showSizePickerDialog(product) }
        tvSelectedProductPrice.text = CurrencyUtils.formatVnd(product.rentalPrice)
        bindProductImage(imgSelectedProduct, product.imageUrl)
    }

    private fun createExtraProductCard(product: SelectedProduct, index: Int): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            isClickable = true
            isFocusable = true
            minimumHeight = dp(184)
            setBackgroundResource(R.drawable.bg_create_order_product_card)
            elevation = 0.5f
            setOnClickListener { openSelectedProductDetailAt(index) }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        }

        card.addView(View(this).apply {
            setBackgroundResource(R.drawable.bg_create_order_product_strip)
        }, LinearLayout.LayoutParams(dp(4), LinearLayout.LayoutParams.MATCH_PARENT))

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(184)
            setPadding(dp(14), dp(14), dp(18), dp(14))
        }
        card.addView(content, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val image = ImageView(this).apply {
            setBackgroundResource(R.drawable.bg_product_image_box)
            clipToOutline = true
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        content.addView(image, LinearLayout.LayoutParams(dp(106), dp(156)))
        bindProductImage(image, product.imageUrl)

        val detail = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            minimumHeight = dp(156)
        }
        content.addView(detail, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(18)
        })

        val titleFrame = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
        }
        detail.addView(titleFrame, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(56)
        ))

        titleFrame.addView(TextView(this).apply {
            text = product.name
            setTextColor(getColor(R.color.text_primary))
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.TOP or Gravity.START
            includeFontPadding = true
            maxLines = 2
            minLines = 2
            isSingleLine = false
        }, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).apply {
            marginEnd = dp(36)
        })

        titleFrame.addView(TextView(this).apply {
            text = "×"
            setTextColor(getColor(R.color.customer_search_hint))
            textSize = 28f
            gravity = Gravity.CENTER
            setOnClickListener { removeSelectedProductAt(index) }
        }, FrameLayout.LayoutParams(dp(28), dp(28), Gravity.TOP or Gravity.END))

        detail.addView(TextView(this).apply {
            text = "ID: ${product.code}"
            setTextColor(getColor(R.color.text_primary))
            textSize = 14f
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        val quantityContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, 0)
        }
        detail.addView(quantityContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        quantityContainer.addView(TextView(this).apply {
            text = "SỐ LƯỢNG:"
            setTextColor(getColor(R.color.text_secondary))
            textSize = 10f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        quantityContainer.addView(EditText(this).apply {
            configureQuantityInput(this)
            setText(product.selectedQuantity.toString())
            setSelection(text.length)

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val quantity = s?.toString()?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    if (product.selectedQuantity != quantity) {
                        product.selectedQuantity = quantity
                        updatePaymentSummary()
                    }
                }
            })
        }, LinearLayout.LayoutParams(dp(48), dp(28)).apply {
            marginStart = dp(8)
        })

        detail.addView(Space(this), LinearLayout.LayoutParams(dp(1), 0, 1f))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        detail.addView(bottom, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        bottom.addView(TextView(this).apply {
            text = "SIZE: ${product.size}"
            setTextColor(getColor(R.color.brand_primary))
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_create_order_size_chip)
            setPadding(dp(12), 0, dp(12), 0)
            setOnClickListener { showSizePickerDialog(product) }
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dp(24)
        ))

        bottom.addView(TextView(this).apply {
            text = CurrencyUtils.formatVnd(product.rentalPrice)
            setTextColor(getColor(R.color.brand_primary))
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = dp(20)
        })

        return card
    }

    private fun configureQuantityInput(input: EditText) {
        input.apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_create_order_input)
            setTextColor(getColor(R.color.text_primary))
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setSingleLine(true)
            includeFontPadding = false
            setPadding(0, 0, 0, 0)
            minimumHeight = 0
            minHeight = 0
        }
    }

    private fun openSelectedProductDetailAt(index: Int) {
        // E2E PRODUCT DETAIL: user bam vao san pham trong don -> mo man chi tiet dung product_id.
        val product = selectedProducts.getOrNull(index) ?: return
        if (product.id.isBlank()) return
        AppNavigator.openProductDetail(this, product.id)
    }

    private fun bindProductImage(target: ImageView, imageUrl: String) {
        ImageUtils.bindImage(target, imageUrl, R.drawable.img_create_order_product_1)
    }

    private fun showSizePickerDialog(product: SelectedProduct) {
        val sizes = arrayOf("S", "M", "L", "XL", "XXL")
        AlertDialog.Builder(this)
            .setTitle("Chọn SIZE cho ${product.name}")
            .setItems(sizes) { _, which ->
                product.size = sizes[which]
                renderSelectedProducts()
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun stockLabel(quantity: Int): String {
        return if (quantity > 0) "Còn $quantity sản phẩm" else "Hết hàng"
    }

    private fun removeSelectedProductAt(index: Int) {
        if (index !in selectedProducts.indices) return
        selectedProducts.removeAt(index)
        renderSelectedProducts()
        Toast.makeText(this, "Đã xóa sản phẩm khỏi đơn thuê", Toast.LENGTH_SHORT).show()
    }

    private fun clearSelectedProduct(showToast: Boolean = true) {
        // E2E REMOVE PRODUCT: user bam x -> chi bo san pham khoi don dang tao, khong xoa khoi SQLite.
        selectedProducts.clear()
        renderSelectedProducts()

        if (showToast) {
            Toast.makeText(this, "Đã xóa sản phẩm khỏi đơn thuê", Toast.LENGTH_SHORT).show()
        }
        return
        selectedProductId = null
        selectedRentalPrice = 0.0
        selectedDeposit = 0.0
        productCard1.visibility = View.GONE
        updatePaymentSummary()

        if (showToast) {
            Toast.makeText(this, "Đã xóa sản phẩm khỏi đơn thuê", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createOrder() {
        if (selectedCustomerName.isBlank() || selectedCustomerPhone.isBlank()) {
            Toast.makeText(this, "Hay chon khach hang truoc", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedProducts.isEmpty()) {
            Toast.makeText(this, "Hãy chọn sản phẩm trước", Toast.LENGTH_SHORT).show()
            return
        }

        if (returnDate.isBlank()) {
            Toast.makeText(this, "Hay chon ngay tra", Toast.LENGTH_SHORT).show()
            return
        }

        val requestBody = JSONObject().apply {
            put("customerName", selectedCustomerName)
            put("customerPhone", selectedCustomerPhone)
            put("productIds", JSONArray().apply { 
                selectedProducts.forEach { p -> repeat(p.selectedQuantity) { put(p.id) } } 
            })
            put("productSizes", JSONArray().apply {
                selectedProducts.forEach { p -> repeat(p.selectedQuantity) { put(p.size) } }
            })
            put("pickupDate", pickupDate)
            put("returnDate", returnDate)
            // Don moi tao se nam o tab "TAO DON".
            // Khi user xac nhan thanh toan, don moi chuyen sang "DANG THUE".
            put("status", "pending")

            // Fix E2E: Gửi kèm tổng tiền thực tế trên UI (bao gồm số ngày + cọc) thay vì để DB tự tính sai
            val rentalDays = rentalDays()
            val totalRental = selectedProducts.sumOf { it.rentalPrice * it.selectedQuantity } * rentalDays
            // Tiền cọc thu của khách bằng 50% giá trị sản phẩm
            val totalDeposit = selectedProducts.sumOf { it.deposit * 0.5 * it.selectedQuantity }
            put("totalAmount", totalRental + totalDeposit)
        }

        runApi(
            loadingMessage = "Đang tạo đơn thuê...",
            request = { ApiClient.post("/orders", requestBody) }
        ) {
            Toast.makeText(this, "Tạo đơn thuê thành công", Toast.LENGTH_SHORT).show()
            AppNavigator.openOrdersWithStatus(this, "pending")
            finish()
        }
    }

    // ===== E2E UPDATE ORDER =====
    // User sửa thông tin đơn thuê cũ -> validate -> gọi PUT /orders/:code -> quay lại màn danh sách.
    private fun updateOrder() {
        if (selectedCustomerName.isBlank() || selectedCustomerPhone.isBlank()) {
            Toast.makeText(this, "Hãy chọn khách hàng trước", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedProducts.isEmpty()) {
            Toast.makeText(this, "Hãy chọn sản phẩm trước", Toast.LENGTH_SHORT).show()
            return
        }

        if (returnDate.isBlank()) {
            Toast.makeText(this, "Hãy chọn ngày trả", Toast.LENGTH_SHORT).show()
            return
        }

        val requestBody = JSONObject().apply {
            put("customerName", selectedCustomerName)
            put("customerPhone", selectedCustomerPhone)
            put("productIds", JSONArray().apply { 
                selectedProducts.forEach { p -> repeat(p.selectedQuantity) { put(p.id) } } 
            })
            put("productSizes", JSONArray().apply {
                selectedProducts.forEach { p -> repeat(p.selectedQuantity) { put(p.size) } }
            })
            put("pickupDate", pickupDate)
            put("returnDate", returnDate)
            
            // Fix E2E: Gửi kèm tổng tiền thực tế trên UI (bao gồm số ngày + cọc) thay vì để DB tự tính sai
            val rentalDays = rentalDays()
            val totalRental = selectedProducts.sumOf { it.rentalPrice * it.selectedQuantity } * rentalDays
            // Tiền cọc thu của khách bằng 50% giá trị sản phẩm
            val totalDeposit = selectedProducts.sumOf { it.deposit * 0.5 * it.selectedQuantity }
            put("totalAmount", totalRental + totalDeposit)
        }

        runApi(
            loadingMessage = "Đang cập nhật đơn thuê...",
            request = { ApiClient.put("/orders/${editOrderCode}", requestBody) }
        ) {
            Toast.makeText(this, "Cập nhật đơn thuê thành công", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // ===== E2E LOAD ORDER FOR EDIT =====
    // Khi mở edit mode -> tải đơn thuê theo code -> điền lại toàn bộ form (khách, sản phẩm, ngày).
    private fun loadOrderForEdit(orderCode: String) {
        runApi(
            loadingMessage = "Đang tải đơn thuê...",
            request = { ApiClient.get("/orders") }
        ) { result ->
            val orders = JSONObject(result.body).optJSONArray("data") ?: JSONArray()

            // Tìm đơn thuê theo code
            var targetOrder: JSONObject? = null
            for (i in 0 until orders.length()) {
                val order = orders.optJSONObject(i) ?: continue
                if (order.optString("code") == orderCode) {
                    targetOrder = order
                    break
                }
            }

            if (targetOrder == null) {
                Toast.makeText(this, "Không tìm thấy đơn thuê", Toast.LENGTH_SHORT).show()
                finish()
                return@runApi
            }

            // --- Điền lại thông tin khách hàng ---
            selectedCustomerName = targetOrder.optString("customerName", "")
            selectedCustomerPhone = targetOrder.optString("customerPhone", "")
            selectedCustomerEmail = "" // Email không lưu trong order, sẽ load riêng

            suppressCustomerSearch = true
            edtSearchCustomer.setText(selectedCustomerName)
            suppressCustomerSearch = false
            updateCustomerInfo()

            // Load email từ customer nếu có phone
            if (selectedCustomerPhone.isNotBlank()) {
                loadCustomerEmailByPhone(selectedCustomerPhone)
            }

            // --- Điền lại ngày thuê / ngày trả ---
            pickupDate = targetOrder.optString("pickupDate", todayStorageDate())
            returnDate = targetOrder.optString("returnDate", "")
            updateDateLabels()

            // --- Điền lại danh sách sản phẩm ---
            val products = targetOrder.optJSONArray("products")
                ?: targetOrder.optJSONArray("productIds")
                ?: JSONArray()

            selectedProducts.clear()
            for (i in 0 until products.length()) {
                val product = products.optJSONObject(i) ?: continue
                addSelectedProduct(product, replaceExisting = false, showToast = false)
            }
        }
    }

    // Load email khách hàng theo số điện thoại (vì order chỉ lưu name + phone)
    private fun loadCustomerEmailByPhone(phone: String) {
        runApi(
            loadingMessage = null,
            request = { ApiClient.get("/customers?search=$phone") }
        ) { result ->
            val customers = JSONObject(result.body).optJSONArray("data") ?: JSONArray()
            for (i in 0 until customers.length()) {
                val customer = customers.optJSONObject(i) ?: continue
                if (customer.optString("phone") == phone) {
                    selectedCustomerEmail = customer.optString("email", "").ifBlank { "Chưa có email" }
                    selectedCustomerId = customer.optString("id", customer.optString("_id", ""))
                    updateCustomerInfo()
                    break
                }
            }
        }
    }

    private fun showRentalDatePicker(isPickupDate: Boolean) {
        // E2E RENTAL DATE: user bam ngay thue/ngay tra -> chon ngay tren DatePicker -> cap nhat tien thue.
        val currentValue = if (isPickupDate) pickupDate else returnDate
        val calendar = parseDateToCalendar(currentValue)

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = formatStorageDate(year, month, dayOfMonth)

                if (isPickupDate) {
                    pickupDate = selectedDate
                    if (returnDate.isNotBlank() && parseDateMillis(returnDate) < parseDateMillis(pickupDate)) {
                        returnDate = selectedDate
                    }
                } else {
                    returnDate = selectedDate
                    if (parseDateMillis(returnDate) < parseDateMillis(pickupDate)) {
                        Toast.makeText(this, "Ngay tra phai sau hoac bang ngay thue", Toast.LENGTH_SHORT).show()
                        returnDate = pickupDate
                    }
                }

                updateDateLabels()
                updatePaymentSummary()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun parseDateToCalendar(value: String): Calendar {
        val calendar = Calendar.getInstance()
        val parts = value.split("-")
        if (parts.size == 3) {
            calendar.set(
                parts[0].toIntOrNull() ?: calendar.get(Calendar.YEAR),
                (parts[1].toIntOrNull() ?: (calendar.get(Calendar.MONTH) + 1)) - 1,
                parts[2].toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)
            )
        }
        return calendar
    }

    private fun formatStorageDate(year: Int, month: Int, dayOfMonth: Int): String {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
    }

    private fun parseDateMillis(value: String): Long {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }


    private fun updateDateLabels() {
        tvPickupDate.text = formatDisplayDate(pickupDate)
        tvReturnDate.text = if (returnDate.isBlank()) "Chon ngay tra" else formatDisplayDate(returnDate)
    }

    private fun updatePaymentSummary() {
        val days = rentalDays()
        val subtotal = selectedProducts.sumOf { it.rentalPrice * it.selectedQuantity } * days
        // Tiền cọc thu của khách bằng 50% giá trị sản phẩm
        val deposit = selectedProducts.sumOf { it.deposit * 0.5 * it.selectedQuantity }
        val total = subtotal + deposit

        tvRentalDays.text = "Tiền thuê ($days ngày)"
        tvRentalSubtotal.text = CurrencyUtils.formatVnd(subtotal)
        tvDeposit.text = CurrencyUtils.formatVnd(deposit)
        tvTotal.text = CurrencyUtils.formatVnd(total)
    }

    private fun rentalDays(): Int {
        return try {
            if (returnDate.isBlank()) return 1
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val start = parser.parse(pickupDate)?.time ?: return 1
            val end = parser.parse(returnDate)?.time ?: return 1
            val diffDays = ((end - start) / (24 * 60 * 60 * 1000)).toInt()
            max(diffDays, 1)
        } catch (e: Exception) {
            1
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun todayStorageDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
    }

    private fun formatDisplayDate(value: String): String {
        val parts = value.split("-")
        return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else value
    }
}
