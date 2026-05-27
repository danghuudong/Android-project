package com.example.project_android.view.activity

import com.example.project_android.R

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.util.Log
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.project_android.navigation.AppNavigator
import com.example.project_android.navigation.SidebarController
import com.example.project_android.network.ApiClient
import com.example.project_android.network.runApi
import com.example.project_android.utils.ImageUtils
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.Normalizer

class ProductActivity : AppCompatActivity() {

    private companion object {
        const val PAGE_SIZE = 20
    }

    private lateinit var fabAddProduct: LinearLayout
    private lateinit var cardProduct1: LinearLayout
    private lateinit var cardProduct2: LinearLayout
    private lateinit var cardProduct3: LinearLayout
    private lateinit var cardProduct4: LinearLayout
    private lateinit var productListContainer: LinearLayout
    private lateinit var productHeader: ConstraintLayout
    private lateinit var btnLoadMoreProducts: LinearLayout
    private lateinit var txtLoadMoreProducts: TextView
    private lateinit var tvProductSubtitle: TextView
    private lateinit var edtSearchProduct: EditText
    private lateinit var btnFilter: LinearLayout
    private lateinit var btnSort: LinearLayout
    private lateinit var btnBack: ImageView
    private lateinit var btnMenu: ImageView
    private lateinit var navHome: LinearLayout
    private lateinit var navOrders: LinearLayout
    private lateinit var navProducts: LinearLayout
    private lateinit var navCustomers: LinearLayout

    private var allProducts = JSONArray()
    private var displayedProductCount = 0
    private var currentQuery = ""
    private var productCategoryOptions: List<Pair<String?, String>> = emptyList()

    // Filter & Sort state
    private var activeFilterStatus: String? = null
    private var activeFilterCategory: String? = null
    private var activeSortKey: String? = null
    private var selectProductForOrder = false
    
    private val searchHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val searchRunnable = Runnable {
        loadProducts(buildProductQuery(), showToast = false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product)

        selectProductForOrder = intent.getBooleanExtra("select_product_for_order", false)
        initViews()
        SidebarController.bindFromActivity(this)
        setupEvents()
        loadCategoryOptions()
        loadProducts()
    }

    override fun onResume() {
        super.onResume()
        loadCategoryOptions()
        loadProducts(buildProductQuery(), showToast = false)
    }

    private fun initViews() {
        fabAddProduct = findViewById(R.id.fabAddProduct)
        cardProduct1 = findViewById(R.id.cardProduct1)
        cardProduct2 = findViewById(R.id.cardProduct2)
        cardProduct3 = findViewById(R.id.cardProduct3)
        cardProduct4 = findViewById(R.id.cardProduct4)
        productListContainer = findViewById(R.id.productListContainer)
        productHeader = findViewById(R.id.productHeader)
        btnLoadMoreProducts = findViewById(R.id.btnLoadMoreProducts)
        txtLoadMoreProducts = findViewById(R.id.txtLoadMoreProducts)
        tvProductSubtitle = findViewById(R.id.tvProductSubtitle)
        edtSearchProduct = findViewById(R.id.edtSearchProduct)
        btnFilter = findViewById(R.id.btnFilter)
        btnSort = findViewById(R.id.btnSort)
        btnBack = findViewById(R.id.btnBack)
        btnMenu = findViewById(R.id.btnMenu)
        navHome = findViewById(R.id.navHome)
        navOrders = findViewById(R.id.navOrders)
        navProducts = findViewById(R.id.navProducts)
        navCustomers = findViewById(R.id.navCustomers)

        hideStaticProductCards()
        if (selectProductForOrder) {
            btnBack.visibility = View.VISIBLE
            btnMenu.visibility = View.GONE
            fabAddProduct.visibility = View.GONE
            productHeader.layoutParams = productHeader.layoutParams.apply {
                height = ConstraintLayout.LayoutParams.WRAP_CONTENT
            }
            productHeader.minHeight = dp(82)
            productHeader.setPadding(
                productHeader.paddingLeft,
                productHeader.paddingTop,
                productHeader.paddingRight,
                dp(4)
            )
            tvProductSubtitle.text = "Chọn sản phẩm còn hàng để thêm vào đơn thuê"
        }
    }

    private fun setupEvents() {
        btnBack.setOnClickListener { finish() }
        fabAddProduct.setOnClickListener { AppNavigator.openAddProduct(this) }
        btnFilter.setOnClickListener { showFilterDialog() }
        btnSort.setOnClickListener { showSortDialog() }
        btnLoadMoreProducts.setOnClickListener { renderNextProducts() }
        navHome.setOnClickListener { AppNavigator.openDashboard(this) }
        navOrders.setOnClickListener { AppNavigator.openOrders(this) }
        navProducts.setOnClickListener { loadProducts(buildProductQuery()) }
        navCustomers.setOnClickListener { AppNavigator.openCustomers(this) }

        edtSearchProduct.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                loadProducts(buildProductQuery(), showToast = true)
                hideKeyboard()
                true
            } else {
                false
            }
        }

        edtSearchProduct.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN && keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                loadProducts(buildProductQuery(), showToast = true)
                hideKeyboard()
                true
            } else {
                false
            }
        }


        edtSearchProduct.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                searchHandler.removeCallbacks(searchRunnable)
                searchHandler.postDelayed(searchRunnable, 500)
            }
        })

        cardProduct1.setOnClickListener { Toast.makeText(this, "Mở chi tiết sản phẩm #1", Toast.LENGTH_SHORT).show() }
        cardProduct2.setOnClickListener { Toast.makeText(this, "Mở chi tiết sản phẩm #2", Toast.LENGTH_SHORT).show() }
        cardProduct3.setOnClickListener { Toast.makeText(this, "Mở chi tiết sản phẩm #3", Toast.LENGTH_SHORT).show() }
        cardProduct4.setOnClickListener { Toast.makeText(this, "Mở chi tiết sản phẩm #4", Toast.LENGTH_SHORT).show() }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(edtSearchProduct.windowToken, 0)
    }

    private fun loadCategoryOptions() {
        runApi(
            loadingMessage = null,
            request = { ApiClient.get("/categories") }
        ) { result ->
            val data = JSONObject(result.body).getJSONArray("data")
            val options = mutableListOf<Pair<String?, String>>()

            for (index in 0 until data.length()) {
                val name = data.getJSONObject(index).optString("name").trim()
                if (name.isNotEmpty()) options.add(name to name)
            }

            productCategoryOptions = options

            // E2E SYNC: neu danh muc dang filter da bi doi ten/xoa, reset filter de list khong rong sai ly do.
            if (activeFilterCategory != null && productCategoryOptions.none { it.first == activeFilterCategory }) {
                activeFilterCategory = null
                updateFilterSortLabels()
                loadProducts(buildProductQuery(), showToast = false)
            }
        }
    }

    // ── Filter Dialog ──
    private fun showFilterDialog() {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.WHITE)
            setPadding(dp(24), dp(24), dp(24), dp(20))
        }

        // Title
        root.addView(TextView(this).apply {
            text = "Bộ lọc sản phẩm"
            setTextColor(getColor(R.color.text_primary))
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
        })

        // ── Status filter ──
        root.addView(TextView(this).apply {
            text = "TRẠNG THÁI"
            setTextColor(getColor(R.color.text_secondary))
            textSize = 11f
            setTypeface(typeface, Typeface.BOLD)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(20)
        })

        val statusOptions = listOf(
            null to "Tất cả",
            "available" to "Sẵn sàng",
            "renting" to "Đang thuê",
            "cleaning" to "Đang giặt ủi"
        )
        var selectedStatus = activeFilterStatus

        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
        }
        root.addView(statusRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(10)
        })

        val statusChips = mutableListOf<TextView>()
        statusOptions.forEach { (value, label) ->
            val chip = createChip(label, selectedStatus == value)
            statusChips.add(chip)
            statusRow.addView(chip, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = dp(8)
            })
            chip.setOnClickListener {
                selectedStatus = value
                statusChips.forEachIndexed { i, c -> styleChip(c, statusOptions[i].first == selectedStatus) }
            }
        }

        // ── Category filter ──
        root.addView(TextView(this).apply {
            text = "DANH MỤC"
            setTextColor(getColor(R.color.text_secondary))
            textSize = 11f
            setTypeface(typeface, Typeface.BOLD)
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(18)
        })

        val categoryOptions = listOf(
            null to "Tất cả",
        ) + productCategoryOptions
        var selectedCategory = activeFilterCategory

        val catRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
        }
        root.addView(catRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(10)
        })

        // Wrap categories in a HorizontalScrollView
        val catScroll = android.widget.HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val catInner = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        catScroll.addView(catInner)
        catRow.addView(catScroll)

        val catChips = mutableListOf<TextView>()
        categoryOptions.forEach { (value, label) ->
            val chip = createChip(label, selectedCategory == value)
            catChips.add(chip)
            catInner.addView(chip, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = dp(8)
            })
            chip.setOnClickListener {
                selectedCategory = value
                catChips.forEachIndexed { i, c -> styleChip(c, categoryOptions[i].first == selectedCategory) }
            }
        }

        // ── Action buttons ──
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        root.addView(actions, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(24)
        })

        // Reset button
        actions.addView(TextView(this).apply {
            text = "Đặt lại"
            setTextColor(getColor(R.color.text_secondary))
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setOnClickListener {
                activeFilterStatus = null
                activeFilterCategory = null
                applyFilterSort()
                dialog.dismiss()
            }
        })

        // Apply button
        actions.addView(TextView(this).apply {
            text = "Áp dụng"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(getColor(R.color.brand_primary))
                cornerRadius = dp(8).toFloat()
            }
            setPadding(dp(20), dp(10), dp(20), dp(10))
            setOnClickListener {
                activeFilterStatus = selectedStatus
                activeFilterCategory = selectedCategory
                applyFilterSort()
                dialog.dismiss()
            }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            marginStart = dp(12)
        })

        dialog.setContentView(root)
        dialog.window?.setLayout(
            android.view.WindowManager.LayoutParams.MATCH_PARENT,
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    // ── Sort Dialog ──
    private fun showSortDialog() {
        val sortOptions = listOf(
            null to "Mặc định",
            "newest" to "Mới nhất",
            "oldest" to "Cũ nhất",
            "price_asc" to "Giá: Thấp → Cao",
            "price_desc" to "Giá: Cao → Thấp"
        )

        val currentIndex = sortOptions.indexOfFirst { it.first == activeSortKey }.coerceAtLeast(0)
        val labels = sortOptions.map { it.second }.toTypedArray()

        android.app.AlertDialog.Builder(this)
            .setTitle("Sắp xếp sản phẩm")
            .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                activeSortKey = sortOptions[which].first
                applyFilterSort()
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // ── Build query from state ──
    private fun applyFilterSort() {
        loadProducts(buildProductQuery())

        // Update button labels to reflect active state
        updateFilterSortLabels()
    }

    private fun buildProductQuery(): String {
        val params = mutableListOf<String>()

        val searchText = edtSearchProduct.text?.toString()?.trim().orEmpty()
        if (searchText.isNotBlank()) {
            params.add("search=${encodeQueryValue(searchText)}")
        }
        activeFilterStatus?.let { params.add("status=${encodeQueryValue(it)}") }
        activeFilterCategory?.let { params.add("category=${encodeQueryValue(it)}") }
        activeSortKey?.let { params.add("sort=${encodeQueryValue(it)}") }

        return if (params.isEmpty()) "" else "?${params.joinToString("&")}"
    }

    private fun encodeQueryValue(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name())
    }

    private fun updateFilterSortLabels() {
        val filterLabel = (btnFilter.getChildAt(0) as? TextView)
        val sortLabel = (btnSort.getChildAt(0) as? TextView)

        if (activeFilterStatus != null || activeFilterCategory != null) {
            filterLabel?.text = "Bộ lọc ✓"
        } else {
            filterLabel?.text = "Bộ lọc"
        }

        val sortText = when (activeSortKey) {
            "newest" -> "Mới nhất"
            "oldest" -> "Cũ nhất"
            "price_asc" -> "Giá ↑"
            "price_desc" -> "Giá ↓"
            else -> "Sắp xếp"
        }
        sortLabel?.text = sortText
    }

    // ── Chip helpers ──
    private fun createChip(label: String, isActive: Boolean): TextView {
        return TextView(this).apply {
            text = label
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            styleChip(this, isActive)
        }
    }

    private fun styleChip(chip: TextView, isActive: Boolean) {
        if (isActive) {
            chip.setTextColor(android.graphics.Color.WHITE)
            chip.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(getColor(R.color.brand_primary))
                cornerRadius = dp(16).toFloat()
            }
        } else {
            chip.setTextColor(getColor(R.color.text_primary))
            chip.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.TRANSPARENT)
                setStroke(dp(1), android.graphics.Color.parseColor("#D0CDE1"))
                cornerRadius = dp(16).toFloat()
            }
        }
    }

    private fun loadProducts(query: String = "", showToast: Boolean = true) {
        currentQuery = query
        displayedProductCount = 0
        productListContainer.removeAllViews()
        btnLoadMoreProducts.visibility = View.GONE

        runApi(
            loadingMessage = if (showToast) "Đang tải sản phẩm..." else null,
            request = { ApiClient.get("/products$currentQuery") }
        ) { result ->
            allProducts = JSONObject(result.body).getJSONArray("data")
            Log.d("ProductSearch", "DB returned ${allProducts.length()} products for query='$currentQuery'")
            tvProductSubtitle.text = if (selectProductForOrder) {
                "Chọn sản phẩm còn hàng để thêm vào đơn thuê"
            } else {
                "Kho lưu trữ gồm ${allProducts.length()} mặt hàng"
            }
            if (allProducts.length() == 0) {
                showEmptyProductsMessage()
                if (showToast) {
                    Toast.makeText(this, "Không tìm thấy sản phẩm phù hợp", Toast.LENGTH_SHORT).show()
                }
                return@runApi
            }
            renderNextProducts()
            if (showToast) {
                Toast.makeText(this, "Đã tải ${allProducts.length()} sản phẩm từ SQLite", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEmptyProductsMessage() {
        productListContainer.addView(TextView(this).apply {
            text = "Không tìm thấy sản phẩm phù hợp"
            setTextColor(getColor(R.color.text_secondary))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, dp(28), 0, dp(28))
        })
        updateLoadMoreState()
    }

    private fun renderNextProducts() {
        val nextCount = minOf(displayedProductCount + PAGE_SIZE, allProducts.length())

        for (index in displayedProductCount until nextCount) {
            productListContainer.addView(createProductCard(allProducts.getJSONObject(index), index))
        }

        displayedProductCount = nextCount
        updateLoadMoreState()
    }

    private fun createProductCard(product: JSONObject, index: Int): LinearLayout {
        val status = product.optString("status", "available")
        val quantity = product.optInt("quantity", 0)

        return LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = if (index == 0) dp(24) else dp(16)
            }
            setBackgroundResource(R.drawable.bg_product_card)
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(14))
            setOnClickListener {
                val productId = product.optString("id", "")
                if (selectProductForOrder) {
                    selectProductForOrder(product)
                } else if (productId.isNotBlank()) {
                    AppNavigator.openProductDetail(this@ProductActivity, productId)
                }
            }

            addView(createProductImageSection(product, status, quantity))
            addView(createProductInfoRow(product, status, quantity))
            addView(createSizeChip(product.optString("size", "")))
        }
    }

    private fun selectProductForOrder(product: JSONObject) {
        val productId = product.optString("id", "")
        if (productId.isBlank()) return

        if (product.optInt("quantity", 0) <= 0) {
            Toast.makeText(this, "Sản phẩm đã hết hàng", Toast.LENGTH_SHORT).show()
            return
        }

        setResult(Activity.RESULT_OK, Intent().putExtra("product_id", productId))
        finish()
    }

    private fun createProductImageSection(product: JSONObject, status: String, quantity: Int): FrameLayout {
        val imageUrl = product.optString("imageUrl", "")
        val displayStatus = if (quantity <= 0) "out_of_stock" else status
        
        return FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(220)
            )

            addView(ImageView(this@ProductActivity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP

                // imageUrl được ưu tiên; nếu chưa có ảnh thì dùng placeholder chung.
                ImageUtils.bindImage(this, imageUrl, android.R.drawable.ic_menu_gallery)
            })

            addView(ImageView(this@ProductActivity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    dp(32), dp(32)
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    setMargins(dp(12), dp(12), dp(12), dp(12))
                }
                setImageResource(R.drawable.ic_more_vertical)
                setColorFilter(getColor(R.color.text_primary))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#E6FFFFFF"))
                    cornerRadius = dp(16).toFloat()
                }
                setPadding(dp(6), dp(6), dp(6), dp(6))
                setOnClickListener { view ->
                    val popupMenu = android.widget.PopupMenu(this@ProductActivity, view)
                    popupMenu.menu.add(0, 1, 0, "Sửa sản phẩm")
                    popupMenu.menu.add(0, 2, 0, "Xóa sản phẩm")
                    popupMenu.setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            1 -> AppNavigator.openEditProduct(this@ProductActivity, product.optString("id", ""))
                            2 -> deleteProduct(product.optString("id", ""))
                        }
                        true
                    }
                    popupMenu.show()
                }
            })

            addView(TextView(this@ProductActivity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.END
                    setMargins(dp(10), dp(10), dp(10), dp(10))
                }
                setBackgroundResource(if (displayStatus == "available") R.drawable.bg_status_ready else R.drawable.bg_status_neutral)
                setPadding(dp(10), dp(6), dp(10), dp(6))
                text = getStatusText(displayStatus)
                setTextColor(getColor(if (displayStatus == "available") R.color.brand_primary else R.color.text_secondary))
                textSize = 10f
                setTypeface(typeface, Typeface.BOLD)
            })
        }
    }

    private fun createProductInfoRow(product: JSONObject, status: String, quantity: Int): LinearLayout {
        return LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), dp(14), 0)

            addView(View(this@ProductActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(3), dp(70)).apply {
                    marginEnd = dp(10)
                }
                setBackgroundResource(if (status == "available") R.drawable.bg_product_status_ready_bar else R.drawable.bg_product_status_laundry_bar)
            })

            addView(TextView(this@ProductActivity).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = product.optString("name", "Sản phẩm")
                setTextColor(getColor(R.color.text_primary))
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
            })

            addView(LinearLayout(this@ProductActivity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.END
                orientation = LinearLayout.VERTICAL

                // Giữ đơn giá và đơn vị sát nhau, tránh "/ngày" bị trôi theo chiều rộng badge tồn kho.
                addView(LinearLayout(this@ProductActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    gravity = Gravity.BOTTOM
                    orientation = LinearLayout.HORIZONTAL

                    addView(TextView(this@ProductActivity).apply {
                        text = formatPrice(product.optDouble("rentalPrice", 0.0))
                        setTextColor(getColor(R.color.brand_primary))
                        textSize = 16f
                        setTypeface(typeface, Typeface.BOLD)
                    })

                    addView(TextView(this@ProductActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            marginStart = dp(4)
                            bottomMargin = dp(1)
                        }
                        text = "/ngày"
                        setTextColor(getColor(R.color.text_secondary))
                        textSize = 11f
                    })
                })

                addView(createStockChip(quantity))
            })
        }
    }

    private fun createSizeChip(size: String): TextView {
        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dp(13)
                topMargin = -dp(24)
            }
            setBackgroundResource(R.drawable.bg_size_chip)
            setPadding(dp(10), dp(5), dp(10), dp(5))
            text = "KÍCH CỠ $size"
            setTextColor(getColor(R.color.text_secondary))
            textSize = 11f
        }
    }

    private fun createStockChip(quantity: Int): TextView {
        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
            setBackgroundResource(if (quantity > 0) R.drawable.bg_status_ready else R.drawable.bg_status_neutral)
            setPadding(dp(10), dp(5), dp(10), dp(5))
            text = if (quantity > 0) "CÒN $quantity SẢN PHẨM" else "HẾT HÀNG"
            setTextColor(getColor(if (quantity > 0) R.color.brand_primary else R.color.text_secondary))
            textSize = 11f
            setTypeface(typeface, Typeface.BOLD)
        }
    }

    private fun updateLoadMoreState() {
        val remainingCount = allProducts.length() - displayedProductCount

        if (remainingCount > 0) {
            btnLoadMoreProducts.visibility = View.VISIBLE
            txtLoadMoreProducts.text = "TẢI THÊM SẢN PHẨM ($remainingCount)"
        } else {
            btnLoadMoreProducts.visibility = View.GONE
        }
    }

    private fun hideStaticProductCards() {
        cardProduct1.visibility = View.GONE
        cardProduct2.visibility = View.GONE
        cardProduct3.visibility = View.GONE
        cardProduct4.visibility = View.GONE
    }

    private fun getStatusText(status: String): String {
        return when (status) {
            "available" -> "SẴN SÀNG"
            "out_of_stock" -> "HẾT HÀNG"
            "cleaning" -> "ĐANG GIẶT ỦI"
            "renting" -> "ĐANG THUÊ"
            "maintenance" -> "BẢO TRÌ"
            else -> status.uppercase()
        }
    }

    private fun formatPrice(value: Double): String {
        return "${String.format("%,.0f", value)}đ"
    }

    private fun deleteProduct(id: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn xóa sản phẩm này không? Hành động này không thể hoàn tác.")
            .setPositiveButton("Xóa") { _, _ ->
                runApi(
                    loadingMessage = "Đang xóa sản phẩm...",
                    request = { ApiClient.delete("/products/$id") }
                ) {
                    Toast.makeText(this, "Đã xóa sản phẩm thành công", Toast.LENGTH_SHORT).show()
                    loadProducts(currentQuery, showToast = false)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}

