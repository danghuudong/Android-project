package com.example.project_android.view.activity

import com.example.project_android.R

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.project_android.navigation.AppNavigator
import com.example.project_android.navigation.SidebarController
import com.example.project_android.network.ApiClient
import com.example.project_android.network.runApi
import com.example.project_android.utils.CustomerImageUtils
import com.example.project_android.utils.ImageUtils
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class OrderActivity : AppCompatActivity() {

    private companion object {
        const val ORDER_PAGE_SIZE = 3
    }

    private lateinit var edtSearchOrder: EditText
    private lateinit var orderFilterChips: HorizontalScrollView
    private lateinit var fabAddOrder: LinearLayout
    private lateinit var orderListContainer: LinearLayout
    private lateinit var btnLoadMoreOrders: LinearLayout
    private lateinit var cardOrder1: LinearLayout
    private lateinit var cardOrder2: LinearLayout
    private lateinit var cardOrder3: LinearLayout
    private lateinit var chipAll: TextView
    private lateinit var chipPending: TextView
    private lateinit var chipRenting: TextView
    private lateinit var chipReturned: TextView
    private lateinit var chipOverdue: TextView
    private lateinit var chipCancelled: TextView
    private lateinit var navHome: LinearLayout
    private lateinit var navOrders: LinearLayout
    private lateinit var navProducts: LinearLayout
    private lateinit var navCustomers: LinearLayout
    private var customerPhoneFilter: String? = null
    private var activeOrderQuery = ""
    private var activeOrderStatus: String? = null
    private var allOrders = JSONArray()
    private var displayedOrderCount = 0
    private var hasLoadedOrders = false
    private val searchHandler = Handler(Looper.getMainLooper())
    private val searchRunnable = Runnable {
        loadOrders(buildOrderQuery(), showToast = false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order)

        customerPhoneFilter = intent.getStringExtra("customer_phone")
        activeOrderStatus = intent.getStringExtra("order_status")?.takeIf { it.isNotBlank() }
        initViews()
        SidebarController.bindFromActivity(this)
        setupEvents()
        // Khi man danh sach duoc mo kem order_status (vd: sau khi xac nhan tra),
        // lan load dau tien phai dung filter do de hien dung tab Da tra/Dang thue.
        loadOrders(buildOrderQuery())
    }

    override fun onResume() {
        super.onResume()
        if (hasLoadedOrders) {
            loadOrders(activeOrderQuery, showToast = false)
        }
    }

    override fun onDestroy() {
        searchHandler.removeCallbacks(searchRunnable)
        super.onDestroy()
    }

    private fun initViews() {
        edtSearchOrder = findViewById(R.id.edtSearchOrder)
        orderFilterChips = findViewById(R.id.orderFilterChips)
        fabAddOrder = findViewById(R.id.fabAddOrder)
        orderListContainer = findViewById(R.id.orderListContainer)
        btnLoadMoreOrders = findViewById(R.id.tvLoadMore)
        cardOrder1 = findViewById(R.id.cardOrder1)
        cardOrder2 = findViewById(R.id.cardOrder2)
        cardOrder3 = findViewById(R.id.cardOrder3)
        chipAll = findViewById(R.id.chipAll)
        chipPending = findViewById(R.id.chipPending)
        chipRenting = findViewById(R.id.chipRenting)
        chipReturned = findViewById(R.id.chipReturned)
        chipOverdue = findViewById(R.id.chipOverdue)
        chipCancelled = findViewById(R.id.chipCancelled)
        navHome = findViewById(R.id.navHome)
        navOrders = findViewById(R.id.navOrders)
        navProducts = findViewById(R.id.navProducts)
        navCustomers = findViewById(R.id.navCustomers)

        cardOrder1.visibility = View.GONE
        cardOrder2.visibility = View.GONE
        cardOrder3.visibility = View.GONE
        btnLoadMoreOrders.visibility = View.GONE
        orderFilterChips.visibility = View.VISIBLE
        updateOrderFilterChips()
    }

    private fun setupEvents() {
        fabAddOrder.setOnClickListener { AppNavigator.openCreateOrder(this) }
        cardOrder1.setOnClickListener { AppNavigator.openOrderDetail(this, "ORD-8924") }
        cardOrder2.setOnClickListener { AppNavigator.openOrderDetail(this, "ORD-8925") }
        cardOrder3.setOnClickListener { AppNavigator.openOrderDetail(this, "ORD-8920") }
        btnLoadMoreOrders.setOnClickListener { renderNextOrders() }
        chipAll.setOnClickListener { applyOrderFilter(null) }
        chipPending.setOnClickListener { applyOrderFilter("pending") }
        chipRenting.setOnClickListener { applyOrderFilter("renting") }
        chipReturned.setOnClickListener { applyOrderFilter("returned") }
        chipOverdue.setOnClickListener { applyOrderFilter("overdue") }
        chipCancelled.setOnClickListener { applyOrderFilter("cancelled") }
        navHome.setOnClickListener { AppNavigator.openDashboard(this) }
        navOrders.setOnClickListener { loadOrders(buildOrderQuery()) }
        navProducts.setOnClickListener { AppNavigator.openProducts(this) }
        navCustomers.setOnClickListener { AppNavigator.openCustomers(this) }

        edtSearchOrder.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                loadOrders(buildOrderQuery(), showToast = true)
                hideKeyboard()
                true
            } else {
                false
            }
        }

        edtSearchOrder.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN && keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                loadOrders(buildOrderQuery(), showToast = true)
                hideKeyboard()
                true
            } else {
                false
            }
        }

        edtSearchOrder.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                searchHandler.removeCallbacks(searchRunnable)
                searchHandler.postDelayed(searchRunnable, 400)
            }
        })
    }

    private fun applyOrderFilter(status: String?) {
        activeOrderStatus = status
        updateOrderFilterChips()
        loadOrders(buildOrderQuery())
    }

    private fun buildOrderQuery(): String {
        val params = mutableListOf<String>()
        activeOrderStatus?.takeIf { it.isNotBlank() }?.let { params.add("status=$it") }

        val keyword = edtSearchOrder.text?.toString()?.trim().orEmpty()
        if (keyword.isNotEmpty()) {
            params.add("search=${encodeQueryValue(keyword)}")
        }

        return if (params.isEmpty()) "" else "?${params.joinToString("&")}"
    }

    private fun encodeQueryValue(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name())
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(edtSearchOrder.windowToken, 0)
    }

    private fun updateOrderFilterChips() {
        styleOrderFilterChip(chipAll, activeOrderStatus == null)
        styleOrderFilterChip(chipPending, activeOrderStatus == "pending")
        styleOrderFilterChip(chipRenting, activeOrderStatus == "renting")
        styleOrderFilterChip(chipReturned, activeOrderStatus == "returned")
        styleOrderFilterChip(chipOverdue, activeOrderStatus == "overdue")
        styleOrderFilterChip(chipCancelled, activeOrderStatus == "cancelled")
    }

    private fun styleOrderFilterChip(chip: TextView, isActive: Boolean) {
        chip.setBackgroundResource(if (isActive) R.drawable.bg_chip_active_dark else R.drawable.bg_chip_soft)
        chip.setTextColor(getColor(if (isActive) android.R.color.white else R.color.text_secondary))
    }

    private fun loadOrders(query: String = "", showToast: Boolean = true) {
        activeOrderQuery = query
        val phone = customerPhoneFilter
        val finalQuery = if (!phone.isNullOrBlank()) {
            val separator = if (query.isBlank()) "?" else "&"
            "$query${separator}customerPhone=$phone"
        } else {
            query
        }

        runApi(
            loadingMessage = if (showToast) "\u0110ang t\u1ea3i \u0111\u01a1n thu\u00ea..." else null,
            request = { ApiClient.get("/orders$finalQuery") }
        ) { result ->
            val orders = JSONObject(result.body).getJSONArray("data")
            renderOrders(orders)
            hasLoadedOrders = true
            if (showToast) {
                Toast.makeText(this, "\u0110\u00e3 t\u1ea3i ${orders.length()} \u0111\u01a1n thu\u00ea", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun renderOrders(orders: JSONArray) {
        // E2E ORDER LIST: SQLite tra orders + productIds -> render card dung khach/san pham/status.
        allOrders = orders
        displayedOrderCount = 0
        orderListContainer.removeAllViews()
        btnLoadMoreOrders.visibility = View.GONE

        if (orders.length() == 0) {
            orderListContainer.addView(TextView(this).apply {
                text = "Kh\u00f4ng c\u00f3 \u0111\u01a1n thu\u00ea ph\u00f9 h\u1ee3p"
                setTextColor(getColor(R.color.text_secondary))
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, dp(24), 0, dp(24))
            })
            return
        }

        renderNextOrders()
    }

    private fun renderNextOrders() {
        val nextCount = minOf(displayedOrderCount + ORDER_PAGE_SIZE, allOrders.length())

        for (i in displayedOrderCount until nextCount) {
            orderListContainer.addView(createOrderCard(allOrders.getJSONObject(i), i))
        }

        displayedOrderCount = nextCount
        btnLoadMoreOrders.visibility = if (displayedOrderCount < allOrders.length()) View.VISIBLE else View.GONE
    }

    private fun createOrderCard(order: JSONObject, index: Int): LinearLayout {
        val status = normalizeOrderStatus(order.optString("status", "renting"))
        val orderCode = order.optString("code", "")
        val products = order.optJSONArray("products")
            ?: order.optJSONArray("productIds")
            ?: JSONArray()

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundResource(orderCardBackground(status))
            alpha = if (status == "returned" || status == "overdue_history" || status == "cancelled") 0.78f else 1f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = if (index == 0) dp(18) else dp(14) }

            addView(createOrderHeader(order, status))
            addView(createOrderCustomerRow(order, status))
            addView(createOrderProductRow(order, products, status))
            addView(createOrderTotalRow(order, status))

            // E2E DETAIL: user bam card don thue -> mo man chi tiet don tuong ung.
            setOnClickListener {
                if (orderCode.isNotBlank()) {
                    AppNavigator.openOrderDetail(this@OrderActivity, orderCode)
                } else {
                    AppNavigator.openOrderDetail(this@OrderActivity)
                }
            }
        }
    }

    private fun createOrderHeader(order: JSONObject, status: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP

            addView(LinearLayout(this@OrderActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(this@OrderActivity).apply {
                    text = order.optString("code", "ORD")
                    setTextColor(orderTextColor(status))
                    textSize = 22f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                })
                addView(TextView(this@OrderActivity).apply {
                    text = formatOrderDate(order.optString("pickupDate", ""))
                    setTextColor(getColor(R.color.text_secondary))
                    textSize = 13f
                    setPadding(0, dp(2), 0, 0)
                })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            addView(TextView(this@OrderActivity).apply {
                text = statusLabel(status)
                setBackgroundResource(statusBackground(status))
                setTextColor(statusTextColor(status))
                textSize = 11f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(dp(14), dp(8), dp(14), dp(8))
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = dp(14)
                topMargin = dp(2)
            })

            addView(ImageView(this@OrderActivity).apply {
                setImageResource(R.drawable.ic_more_vertical)
                setPadding(dp(4), dp(4), dp(4), dp(4))
                setOnClickListener {
                    showOrderActionMenu(this, order, status)
                }
            }, LinearLayout.LayoutParams(dp(32), dp(32)))
        }
    }

    private fun showOrderActionMenu(anchor: View, order: JSONObject, status: String) {
        val popupMenu = android.widget.PopupMenu(this, anchor)

        // E2E EDIT ORDER MENU: chỉ cho sửa đơn đang thuê / chờ xử lý
        if (status == "pending") {
            popupMenu.menu.add(0, 3, 0, "S\u1eeda \u0111\u01a1n thu\u00ea")
            popupMenu.menu.add(0, 5, 1, "Thanh to\u00e1n")
            popupMenu.menu.add(0, 6, 2, "H\u1ee7y \u0111\u01a1n")
        }

        if (status == "renting" || status == "overdue") {
            popupMenu.menu.add(0, 4, 0, "X\u00e1c nh\u1eadn tr\u1ea3")
        }

        if (status == "returned") {
            popupMenu.menu.add(0, 2, 1, "X\u00f3a \u0111\u01a1n \u0111\u00e3 tr\u1ea3")
        }

        if (status == "cancelled") {
            popupMenu.menu.add(0, 7, 0, "Ho\u00e0n l\u1ea1i")
        }

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                2 -> {
                    confirmSoftDeleteReturnedOrder(order)
                    true
                }
                3 -> {
                    // E2E EDIT ORDER: mở màn sửa đơn thuê với order code
                    val orderCode = order.optString("code")
                    if (orderCode.isNotBlank()) {
                        AppNavigator.openEditOrder(this, orderCode)
                    }
                    true
                }
                4 -> {
                    val orderCode = order.optString("code")
                    if (orderCode.isNotBlank()) {
                        AppNavigator.openReturn(this, orderCode)
                    }
                    true
                }
                5 -> {
                    val orderCode = order.optString("code")
                    if (orderCode.isNotBlank()) {
                        AppNavigator.openPaymentDetail(this, orderCode)
                    }
                    true
                }
                6 -> {
                    confirmCancelOrder(order)
                    true
                }
                7 -> {
                    confirmRestoreCancelledOrder(order)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun confirmRestoreCancelledOrder(order: JSONObject) {
        val code = order.optString("code")
        if (code.isBlank()) return

        android.app.AlertDialog.Builder(this)
            .setTitle("Ho\u00e0n l\u1ea1i \u0111\u01a1n")
            .setMessage("\u0110\u01a1n $code s\u1ebd \u0111\u01b0\u1ee3c chuy\u1ec3n v\u1ec1 tr\u1ea1ng th\u00e1i T\u1ea0O \u0110\u01a0N v\u00e0 gi\u1eef ch\u1ed7 s\u1ea3n ph\u1ea9m trong kho.")
            .setNegativeButton("Kh\u00f4ng", null)
            .setPositiveButton("Ho\u00e0n l\u1ea1i") { _, _ -> restoreCancelledOrder(code) }
            .show()
    }

    private fun restoreCancelledOrder(code: String) {
        runApi(
            loadingMessage = "\u0110ang ho\u00e0n l\u1ea1i \u0111\u01a1n...",
            request = {
                ApiClient.patch(
                    "/orders/$code/status",
                    JSONObject().put("status", "pending")
                )
            }
        ) {
            Toast.makeText(this, "\u0110\u00e3 ho\u00e0n l\u1ea1i \u0111\u01a1n $code", Toast.LENGTH_SHORT).show()
            loadOrders(buildOrderQuery(), showToast = false)
        }
    }

    private fun confirmCancelOrder(order: JSONObject) {
        val code = order.optString("code")
        if (code.isBlank()) return

        android.app.AlertDialog.Builder(this)
            .setTitle("H\u1ee7y \u0111\u01a1n thu\u00ea")
            .setMessage("\u0110\u01a1n $code s\u1ebd \u0111\u01b0\u1ee3c h\u1ee7y v\u00e0 s\u1ea3n ph\u1ea9m trong \u0111\u01a1n s\u1ebd \u0111\u01b0\u1ee3c tr\u1ea3 l\u1ea1i kho.")
            .setNegativeButton("Kh\u00f4ng", null)
            .setPositiveButton("H\u1ee7y \u0111\u01a1n") { _, _ -> cancelOrder(code) }
            .show()
    }

    private fun cancelOrder(code: String) {
        runApi(
            loadingMessage = "\u0110ang h\u1ee7y \u0111\u01a1n thu\u00ea...",
            request = {
                ApiClient.patch(
                    "/orders/$code/status",
                    JSONObject().put("status", "cancelled")
                )
            }
        ) {
            Toast.makeText(this, "\u0110\u00e3 h\u1ee7y \u0111\u01a1n $code", Toast.LENGTH_SHORT).show()
            loadOrders(buildOrderQuery(), showToast = false)
        }
    }

    private fun confirmSoftDeleteReturnedOrder(order: JSONObject) {
        val code = order.optString("code")
        if (code.isBlank()) return

        android.app.AlertDialog.Builder(this)
            .setTitle("X\u00f3a \u0111\u01a1n \u0111\u00e3 tr\u1ea3")
            .setMessage("\u0110\u01a1n $code s\u1ebd \u0111\u01b0\u1ee3c \u1ea9n kh\u1ecfi kho \u0111\u01a1n thu\u00ea, d\u1eef li\u1ec7u v\u1eabn \u0111\u01b0\u1ee3c gi\u1eef trong h\u1ec7 th\u1ed1ng.")
            .setNegativeButton("H\u1ee7y", null)
            .setPositiveButton("X\u00f3a") { _, _ -> softDeleteReturnedOrder(code) }
            .show()
    }
    private fun softDeleteReturnedOrder(code: String) {
        runApi(
            loadingMessage = "\u0110ang x\u00f3a \u0111\u01a1n \u0111\u00e3 tr\u1ea3...",
            request = {
                ApiClient.patch(
                    "/orders/$code/status",
                    JSONObject().put("status", "deleted_returned")
                )
            }
        ) {
            Toast.makeText(this, "\u0110\u00e3 x\u00f3a \u0111\u01a1n $code", Toast.LENGTH_SHORT).show()
            loadOrders(buildOrderQuery(), showToast = false)
        }
    }
    private fun createOrderCustomerRow(order: JSONObject, status: String): LinearLayout {
        val customerName = order.optString("customerName", "Kh\u00e1ch h\u00e0ng")
        val customerPhone = order.optString("customerPhone", "")
        val isMutedOrder = status == "returned" || status == "overdue_history" || status == "cancelled"
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14) }

            addView(createCustomerAvatar(order, customerName, customerPhone, isMutedOrder), LinearLayout.LayoutParams(dp(34), dp(34)))

            addView(LinearLayout(this@OrderActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(this@OrderActivity).apply {
                    text = customerName
                    setTextColor(orderTextColor(status))
                    textSize = 15f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                })
                addView(TextView(this@OrderActivity).apply {
                    // E2E CUSTOMER PHONE: thay hang khach hang bang so dien thoai de nhan dien khi goi/xac nhan don.
                    text = customerPhone.ifBlank { "Ch\u01b0a c\u00f3 s\u1ed1 \u0111i\u1ec7n tho\u1ea1i" }
                    setTextColor(getColor(R.color.text_secondary))
                    textSize = 11f
                })
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(10)
            })
        }
    }

    private fun createCustomerAvatar(
        order: JSONObject,
        customerName: String,
        customerPhone: String,
        isMutedOrder: Boolean
    ): View {
        val avatarUrl = order.optString("customerAvatar", "")
        val customerId = order.optString("customerId", "")

        return FrameLayout(this).apply {
            setBackgroundResource(if (isMutedOrder) R.drawable.bg_avatar_chip_neutral else R.drawable.bg_avatar_chip_light)

            val avatar = ImageView(this@OrderActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundResource(if (isMutedOrder) R.drawable.bg_avatar_chip_neutral else R.drawable.bg_avatar_chip_light)
            }

            val hasCustomerPhoto = CustomerImageUtils.bindAvatar(
                avatar,
                avatarUrl,
                customerId,
                customerPhone
            )

            if (hasCustomerPhoto) {
                avatar.imageTintList = null
                avatar.clearColorFilter()
                avatar.setPadding(0, 0, 0, 0)
                avatar.clipToOutline = true
                addView(avatar, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            } else {
                addView(TextView(this@OrderActivity).apply {
                    text = initials(customerName)
                    gravity = Gravity.CENTER
                    setTextColor(if (isMutedOrder) getColor(R.color.text_secondary) else getColor(R.color.brand_primary))
                    textSize = 12f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            }
        }
    }

    private fun createOrderProductRow(order: JSONObject, products: JSONArray, status: String): LinearLayout {
        val productCount = products.length()
        val firstProduct = products.optJSONObject(0)

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14) }

            addView(createStackedProductImages(products), LinearLayout.LayoutParams(
                if (productCount <= 1) dp(46) else dp(86),
                dp(64)
            ))

            addView(LinearLayout(this@OrderActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(this@OrderActivity).apply {
                    text = if (productCount <= 1) {
                        firstProduct?.optString("name", "S\u1ea3n ph\u1ea9m thu\u00ea") ?: "S\u1ea3n ph\u1ea9m thu\u00ea"
                    } else {
                        "\u0110\u01a1n g\u1ed3m + ${productCount - 1} s\u1ea3n ph\u1ea9m"
                    }
                    setTextColor(orderTextColor(status))
                    textSize = 15f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                })

                addView(TextView(this@OrderActivity).apply {
                    text = if (productCount <= 1) {
                        "K\u00edch c\u1ee1: ${firstProduct?.optString("size", "--") ?: "--"}"
                    } else {
                        "NHI\u1ec0U K\u00cdCH C\u1ee0"
                    }
                    setTextColor(getColor(R.color.text_secondary))
                    textSize = 11f
                    setPadding(0, dp(2), 0, 0)
                })

                addView(TextView(this@OrderActivity).apply {
                    text = productTimelineText(order, status)
                    setTextColor(if (status == "overdue") getColor(R.color.accent_magenta) else getColor(R.color.text_secondary))
                    textSize = 11f
                    setTypeface(typeface, if (status == "overdue") android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                    setPadding(0, dp(4), 0, 0)
                })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(10)
            })
        }
    }

    private fun createStackedProductImages(products: JSONArray): FrameLayout {
        val count = products.length().coerceAtLeast(1)
        val displayProducts = prioritizeProductsWithImage(products)
        val visibleCount = displayProducts.length().coerceAtLeast(1).coerceAtMost(3)

        return FrameLayout(this).apply {
            for (i in visibleCount - 1 downTo 0) {
                val product = displayProducts.optJSONObject(i)
                addView(ImageView(this@OrderActivity).apply {
                    setBackgroundResource(R.drawable.bg_product_thumb)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setPadding(dp(2), dp(2), dp(2), dp(2))
                    bindProductImage(this, product?.optString("imageUrl", "").orEmpty())
                }, FrameLayout.LayoutParams(dp(46), dp(64)).apply {
                    leftMargin = dp(i * 14)
                })
            }

            if (count > 1) {
                addView(TextView(this@OrderActivity).apply {
                    text = "+${count - 1}"
                    setTextColor(getColor(R.color.brand_primary))
                    textSize = 12f
                    gravity = Gravity.CENTER
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.parseColor("#F3E7F1"))
                        setStroke(dp(1), Color.WHITE)
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                    }
                    elevation = dp(2).toFloat()
                }, FrameLayout.LayoutParams(dp(28), dp(28)).apply {
                    leftMargin = dp(50)
                    topMargin = dp(18)
                })
            }
        }
    }

    private fun prioritizeProductsWithImage(products: JSONArray): JSONArray {
        // MULTI PRODUCT THUMB:
        // Don nhieu san pham co the gom san pham seed khong co anh + san pham user vua them co anh.
        // Uu tien san pham co imageUrl len dau de card Don thue luon hien anh that neu don co bat ky anh nao.
        val withImage = mutableListOf<JSONObject>()
        val withoutImage = mutableListOf<JSONObject>()

        for (index in 0 until products.length()) {
            val product = products.optJSONObject(index) ?: continue
            if (product.optString("imageUrl", "").isNotBlank()) {
                withImage.add(product)
            } else {
                withoutImage.add(product)
            }
        }

        return JSONArray().apply {
            (withImage + withoutImage).forEach { put(it) }
        }
    }

    private fun createOrderTotalRow(order: JSONObject, status: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(18) }

            addView(LinearLayout(this@OrderActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(4), 0, 0)

                addView(TextView(this@OrderActivity).apply {
                    text = "T\u1ed4NG TI\u1ec0N"
                    setTextColor(orderTextColor(status))
                    textSize = 13f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

                addView(TextView(this@OrderActivity).apply {
                    text = formatVnd(displayTotalAmount(order, status))
                    setTextColor(getColor(R.color.brand_primary))
                    textSize = 18f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                })
            })
        }
    }

    private fun createOrderStatusRow(order: JSONObject, status: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14) }

            addView(TextView(this@OrderActivity).apply {
                text = statusLabel(status)
                setBackgroundResource(statusBackground(status))
                setTextColor(statusTextColor(status))
                textSize = 11f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(dp(12), dp(7), dp(12), dp(7))
            })

            addView(Space(this@OrderActivity), LinearLayout.LayoutParams(0, dp(1), 1f))

            addView(ImageView(this@OrderActivity).apply {
                setImageResource(R.drawable.ic_more_vertical)
                setPadding(dp(4), dp(4), dp(4), dp(4))
                setOnClickListener {
                    Toast.makeText(this@OrderActivity, "Chi ti\u1ebft ${order.optString("code")}", Toast.LENGTH_SHORT).show()
                }
            }, LinearLayout.LayoutParams(dp(32), dp(32)))
        }
    }

    private fun bindProductImage(target: ImageView, imageUrl: String) {
        // ORDER IMAGE: product.imageUrl tu SQLite co the la content:// URI.
        // Doc qua ContentResolver de card don thue van hien anh sau khi reload tu /orders.
        ImageUtils.bindImage(target, imageUrl, R.drawable.img_create_order_product_1)
    }

    private fun productTimelineText(order: JSONObject, status: String): String {
        return when (status) {
            "returned" -> "\u0110\u00e3 tr\u1ea3: ${formatShortDate(order.optString("returnDate", ""))}"
            "overdue", "overdue_history" -> "Qu\u00e1 h\u1ea1n: ${formatShortDate(order.optString("returnDate", ""))}"
            "cancelled" -> "\u0110\u00e3 h\u1ee7y"
            else -> "H\u1ea1n tr\u1ea3: ${formatShortDate(order.optString("returnDate", ""))}"
        }
    }
    private fun normalizeOrderStatus(status: String): String {
        return status
    }

    private fun initials(name: String): String {
        return name.split(" ")
            .filter { it.isNotBlank() }
            .takeLast(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifBlank { "KH" }
    }

    private fun formatOrderDate(value: String): String {
        val parts = value.split("-")
        return if (parts.size == 3) "${parts[2]} TH${parts[1]}, ${parts[0]}" else value
    }

    private fun formatShortDate(value: String): String {
        val parts = value.split("-")
        return if (parts.size == 3) "${parts[2]}/${parts[1]}" else value
    }

    private fun displayTotalAmount(order: JSONObject, status: String): Double {
        if (status == "cancelled") return 0.0
        if (status != "returned" && order.optString("paidAt", "").isNotBlank()) {
            return order.optDouble("paidAmount", 0.0)
        }

        val products = order.optJSONArray("products")
            ?: order.optJSONArray("productIds")
            ?: JSONArray()

        if (products.length() == 0) {
            return order.optDouble("totalAmount", 0.0)
        }

        var dailyRental = 0.0
        var deposit = 0.0
        for (index in 0 until products.length()) {
            val product = products.optJSONObject(index) ?: continue
            dailyRental += product.optDouble("rentalPrice", 0.0)
            deposit += product.optDouble("deposit", 0.0) * 0.5
        }

        val rentalFee = dailyRental * rentalDays(order.optString("pickupDate", ""), order.optString("returnDate", ""))
        
        if (status == "returned") {
            val penaltyAmount = order.optDouble("returnPenaltyAmount", 0.0)
            val discountAmount = order.optDouble("discountAmount", 0.0)
            return (rentalFee - discountAmount).coerceAtLeast(0.0) + penaltyAmount
        } else {
            return rentalFee + deposit
        }
    }

    private fun rentalDays(pickupDate: String, returnDate: String): Long {
        return runCatching {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val from = formatter.parse(pickupDate) ?: return@runCatching 1L
            val to = formatter.parse(returnDate) ?: return@runCatching 1L
            val diffMillis = to.time - from.time
            kotlin.math.max(1L, diffMillis / (24L * 60L * 60L * 1000L))
        }.getOrDefault(1L)
    }

    private fun formatVnd(value: Double): String {
        return "${String.format("%,.0f", value).replace(",", ".")}\u0111"
    }
    private fun orderCardBackground(status: String): Int {
        return when (status) {
            "overdue" -> R.drawable.bg_order_card_urgent
            "returned", "overdue_history", "cancelled" -> R.drawable.bg_order_card_neutral
            else -> R.drawable.bg_order_card_primary
        }
    }

    private fun orderTextColor(status: String): Int {
        return if (status == "returned" || status == "overdue_history" || status == "cancelled") {
            getColor(R.color.text_secondary)
        } else {
            getColor(R.color.text_primary)
        }
    }

    private fun statusBackground(status: String): Int {
        return when (status) {
            "overdue" -> R.drawable.bg_status_overdue
            "returned", "overdue_history", "cancelled" -> R.drawable.bg_status_returned
            else -> R.drawable.bg_status_renting
        }
    }

    private fun statusTextColor(status: String): Int {
        return when (status) {
            "overdue" -> Color.parseColor("#C2410C")
            "returned", "overdue_history", "cancelled" -> getColor(R.color.text_secondary)
            else -> getColor(R.color.brand_primary)
        }
    }

    private fun statusLabel(status: String): String {
        return when (status) {
            "pending" -> "T\u1ea0O \u0110\u01a0N"
            "cancelled" -> "\u0110\u00c3 H\u1ee6Y"
            "returned" -> "\u0110\u00c3 TR\u1ea2"
            "overdue", "overdue_history" -> "QU\u00c1 H\u1ea0N"
            else -> "\u0110ANG THU\u00ca"
        }
    }
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}

