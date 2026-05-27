package com.example.project_android.view.activity

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.project_android.R
import com.example.project_android.navigation.AdminAvatarController
import com.example.project_android.network.ApiClient
import com.example.project_android.network.runApi
import com.example.project_android.utils.CurrencyUtils
import com.example.project_android.utils.ImageUtils
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class CategoryDetailActivity : AppCompatActivity() {

    // --- Dá»¯ liá»‡u truyá»n tá»« CategoryManagementActivity qua Intent ---
    private var categoryId = ""
    private var categoryName = ""
    private var categoryDesc = ""
    private var categoryIcon = ""
    private var categoryCount = 0

    // --- View tham chiáº¿u ---
    private lateinit var tvCategoryName: TextView
    private lateinit var tvBadgeCount: TextView
    private lateinit var tvDescription: TextView
    private lateinit var ivCategoryIcon: ImageView
    private lateinit var productListContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Láº¥y dá»¯ liá»‡u Category tá»« Intent
        categoryId = intent.getStringExtra("CATEGORY_ID") ?: ""
        categoryName = intent.getStringExtra("CATEGORY_NAME") ?: "Danh mục"
        categoryDesc = intent.getStringExtra("CATEGORY_DESC") ?: ""
        categoryIcon = intent.getStringExtra("CATEGORY_ICON") ?: ""
        categoryCount = intent.getIntExtra("CATEGORY_COUNT", 0)

        // Dá»±ng giao diá»‡n báº±ng code (giá»‘ng style ProductDetailActivity)
        setContentView(buildUI())

        // Gáº¯n dá»¯ liá»‡u Header
        bindCategoryHeader()

        // Gá»i API láº¥y danh sÃ¡ch sáº£n pháº©m theo tÃªn danh má»¥c
        loadProductsByCategory()
    }

    // ======================================================================
    //  PHáº¦N 1: Dá»°NG GIAO DIá»†N (buildUI)
    // ======================================================================
    private fun buildUI(): View {
        val root = FrameLayout(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@CategoryDetailActivity, R.color.screen_background))
        }

        val scrollView = ScrollView(this).apply { isFillViewport = true }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(60), 0, dp(24))
        }
        scrollView.addView(content)
        root.addView(scrollView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // CÃ¡c pháº§n giao diá»‡n
        content.addView(buildCategoryHeader())
        content.addView(buildSectionTitle())
        content.addView(buildProductList())

        // Top bar overlay (náº±m Ä‘Ã¨ lÃªn trÃªn cÃ¹ng) â€” KHÃ”NG Sá»¬A
        root.addView(buildTopBar())

        return root
    }

    // â”€â”€ Top Bar â€” GIá»® NGUYÃŠN â”€â”€
    private fun buildTopBar(): View {
        return FrameLayout(this).apply {
            setPadding(dp(12), dp(10), dp(16), dp(10))
            setBackgroundColor(ContextCompat.getColor(this@CategoryDetailActivity, R.color.screen_background))

            // NÃºt quay láº¡i
            val backButton = ImageButton(this@CategoryDetailActivity).apply {
                setImageResource(R.drawable.ic_arrow_back)
                setColorFilter(ContextCompat.getColor(this@CategoryDetailActivity, R.color.brand_primary))
                background = null
                setPadding(dp(8), dp(8), dp(8), dp(8))
                setOnClickListener { finish() }
            }
            addView(backButton, FrameLayout.LayoutParams(dp(40), dp(40)).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
            })

            // TiÃªu Ä‘á»
            addView(TextView(this@CategoryDetailActivity).apply {
                text = "Chi tiết danh mục"
                setTextColor(ContextCompat.getColor(this@CategoryDetailActivity, R.color.brand_primary))
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
            }, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER })

            // Avatar
            val avatar = ImageView(this@CategoryDetailActivity).apply {
                setImageResource(android.R.drawable.ic_menu_myplaces)
                setColorFilter(Color.WHITE)
                setBackgroundResource(R.drawable.bg_circle_avatar)
                contentDescription = "Tài khoản quản trị"
                setPadding(dp(6), dp(6), dp(6), dp(6))
            }
            AdminAvatarController.bind(this@CategoryDetailActivity, avatar)
            addView(avatar, FrameLayout.LayoutParams(dp(34), dp(34)).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            })
        }.also {
            it.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.TOP }
        }
    }

    // â”€â”€ Category Header (Icon trÃ²n + TÃªn + Badge + MÃ´ táº£) â”€â”€
    // Background gradient tráº¯ng â†’ tÃ­m nháº¡t, bo gÃ³c dÆ°á»›i 40dp (giá»‘ng thiáº¿t káº¿ HTML gá»‘c)
    private fun buildCategoryHeader(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL

            // Gradient background: tráº¯ng phÃ­a trÃªn â†’ tÃ­m nháº¡t phÃ­a dÆ°á»›i, bo 2 gÃ³c dÆ°á»›i
            background = GradientDrawable().apply {
                colors = intArrayOf(Color.WHITE, Color.parseColor("#F5F7FF"))
                orientation = GradientDrawable.Orientation.TOP_BOTTOM
                cornerRadii = floatArrayOf(
                    0f, 0f, 0f, 0f,                       // top-left, top-right
                    dp(40).toFloat(), dp(40).toFloat(),    // bottom-right
                    dp(40).toFloat(), dp(40).toFloat()     // bottom-left
                )
            }
            setPadding(dp(20), dp(20), dp(20), dp(36))
            elevation = dp(2).toFloat()

            // Icon trÃ²n lá»›n cÃ³ hiá»‡u á»©ng phÃ¡t sÃ¡ng (shadow) nháº¹ hÆ¡n
            ivCategoryIcon = ImageView(this@CategoryDetailActivity).apply {
                setImageResource(android.R.drawable.ic_menu_sort_by_size)
                setColorFilter(Color.WHITE)
                background = rounded(ContextCompat.getColor(this@CategoryDetailActivity, R.color.brand_primary), dp(20))
                setPadding(dp(24), dp(24), dp(24), dp(24))
                elevation = dp(6).toFloat()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    outlineAmbientShadowColor = ContextCompat.getColor(this@CategoryDetailActivity, R.color.brand_primary)
                    outlineSpotShadowColor = ContextCompat.getColor(this@CategoryDetailActivity, R.color.brand_primary)
                }
            }
            addView(ivCategoryIcon, LinearLayout.LayoutParams(dp(88), dp(88)).apply {
                topMargin = dp(12)
            })

            // TÃªn danh má»¥c
            tvCategoryName = TextView(this@CategoryDetailActivity).apply {
                text = "Đang tải..."
                setTextColor(Color.parseColor("#0F172A"))
                textSize = 26f
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
            }
            addView(tvCategoryName, matchWrap(top = 20))

            // Badge sá»‘ lÆ°á»£ng sáº£n pháº©m
            val badgeContainer = LinearLayout(this@CategoryDetailActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(Color.parseColor("#EEF2FF"), dp(12))
                setPadding(dp(12), dp(6), dp(16), dp(6))

                addView(ImageView(this@CategoryDetailActivity).apply {
                    setImageResource(android.R.drawable.ic_menu_agenda)
                    setColorFilter(ContextCompat.getColor(this@CategoryDetailActivity, R.color.brand_primary))
                }, LinearLayout.LayoutParams(dp(16), dp(16)))

                tvBadgeCount = TextView(this@CategoryDetailActivity).apply {
                    text = "0 SẢN PHẨM"
                    setTextColor(ContextCompat.getColor(this@CategoryDetailActivity, R.color.brand_primary))
                    textSize = 12f
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(dp(6), 0, 0, 0)
                }
                addView(tvBadgeCount)
            }
            addView(badgeContainer, wrapWrap(top = 10))

            // MÃ´ táº£
            tvDescription = TextView(this@CategoryDetailActivity).apply {
                text = "Chưa có mô tả"
                setTextColor(Color.parseColor("#64748B"))
                textSize = 14f
                gravity = Gravity.CENTER
                setLineSpacing(0f, 1.4f)
            }
            addView(tvDescription, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(20)
                marginStart = dp(16)
                marginEnd = dp(16)
            })
        }
    }

    // â”€â”€ TiÃªu Ä‘á» "Danh sÃ¡ch sáº£n pháº©m" â”€â”€
    private fun buildSectionTitle(): View {
        return TextView(this).apply {
            text = "Danh sách sản phẩm"
            setTextColor(Color.parseColor("#1E293B"))
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(20), dp(28), dp(20), dp(16))
        }
    }

    // â”€â”€ Container chá»©a danh sÃ¡ch sáº£n pháº©m â”€â”€
    private fun buildProductList(): View {
        productListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
        }
        return productListContainer
    }

    // ======================================================================
    //  PHáº¦N 2: Gáº®N Dá»® LIá»†U VÃ€O GIAO DIá»†N
    // ======================================================================
    private fun bindCategoryHeader() {
        tvCategoryName.text = categoryName
        tvBadgeCount.text = "$categoryCount SẢN PHẨM"
        tvDescription.text = categoryDesc.ifBlank { "Chưa có mô tả" }

        if (categoryIcon.isNotBlank()) {
            ImageUtils.bindImage(ivCategoryIcon, categoryIcon, android.R.drawable.ic_menu_gallery)
            ivCategoryIcon.clearColorFilter()
            ivCategoryIcon.imageTintList = null
            ivCategoryIcon.setPadding(0, 0, 0, 0)
            ivCategoryIcon.scaleType = ImageView.ScaleType.CENTER_CROP
            ivCategoryIcon.outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, dp(20).toFloat())
                }
            }
            ivCategoryIcon.clipToOutline = true
        }
    }

    // ======================================================================
    //  PHáº¦N 3: Gá»ŒI API Láº¤Y DANH SÃCH Sáº¢N PHáº¨M THEO DANH Má»¤C
    // ======================================================================
    private fun loadProductsByCategory() {
        runApi(
            loadingMessage = "Đang tải sản phẩm...",
            request = { ApiClient.get("/products?category=${encodeQueryValue(categoryName)}") }
        ) { result ->
            val data = JSONObject(result.body).get("data")

            val products: JSONArray = when (data) {
                is JSONArray -> data
                is JSONObject -> data.optJSONArray("items") ?: JSONArray()
                else -> JSONArray()
            }

            // Cáº­p nháº­t láº¡i badge count theo sá»‘ lÆ°á»£ng thá»±c táº¿ tá»« DB
            tvBadgeCount.text = "${products.length()} SẢN PHẨM"

            renderProducts(products)
        }
    }

    // ======================================================================
    //  PHáº¦N 4: RENDER Tá»ªNG THáºº Sáº¢N PHáº¨M VÃ€O DANH SÃCH
    // ======================================================================
    private fun renderProducts(products: JSONArray) {
        productListContainer.removeAllViews()

        if (products.length() == 0) {
            productListContainer.addView(TextView(this).apply {
                text = "Chưa có sản phẩm nào trong danh mục này."
                setTextColor(ContextCompat.getColor(this@CategoryDetailActivity, R.color.text_secondary))
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, dp(32), 0, dp(32))
            })
            return
        }

        for (i in 0 until products.length()) {
            val product = products.getJSONObject(i)
            productListContainer.addView(buildProductCard(product))
        }
    }

    // â”€â”€ XÃ¢y dá»±ng 1 tháº» sáº£n pháº©m (khá»›p thiáº¿t káº¿ HTML gá»‘c) â”€â”€
    private fun buildProductCard(product: JSONObject): View {
        val name = product.optString("name", "Sản phẩm")
        val size = product.optString("size", "--")
        val productId = product.optString("id", "")
        val idShort = productId.take(8).uppercase()
        val rentalPrice = product.optDouble("rentalPrice", 0.0)
        val status = product.optString("status", "available")
        val imageUrl = product.optString("imageUrl", "")

        // Card container chÃ­nh
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = rounded(Color.WHITE, dp(16))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            elevation = dp(1).toFloat() // Giáº£m shadow cá»§a card
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) }

            // Báº¥m vÃ o card -> má»Ÿ chi tiáº¿t sáº£n pháº©m
            setOnClickListener {
                val intent = android.content.Intent(this@CategoryDetailActivity, ProductDetailActivity::class.java)
                intent.putExtra("product_id", productId)
                startActivity(intent)
            }
        }

        // â”€â”€ áº¢nh sáº£n pháº©m (bo gÃ³c 12dp) â”€â”€
        val imgProduct = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_gallery)
            setBackgroundColor(Color.parseColor("#F1F5F9"))
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, dp(12).toFloat())
                }
            }
            ImageUtils.bindImage(this, imageUrl, android.R.drawable.ic_menu_gallery)
        }
        card.addView(imgProduct, LinearLayout.LayoutParams(dp(96), dp(96)))

        // â”€â”€ Ná»™i dung bÃªn pháº£i â”€â”€
        val contentColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(2), 0, dp(2))
        }
        card.addView(contentColumn, LinearLayout.LayoutParams(0, dp(96), 1f))

        // Row 1: TÃªn sáº£n pháº©m + Badge tráº¡ng thÃ¡i
        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        contentColumn.addView(row1, matchWrap())

        row1.addView(TextView(this).apply {
            text = if (name.length > 16) "${name.take(16)}..." else name
            setTextColor(Color.parseColor("#1E293B"))
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(8)
        })
        // Badge tráº¡ng thÃ¡i
        val isReady = status == "available"
        val badgeBg = if (isReady) Color.parseColor("#EEF2FF") else Color.parseColor("#FDF2F8")
        val badgeColor = if (isReady) Color.parseColor("#6366F1") else Color.parseColor("#DB2777")
        val badgeText = if (isReady) "SẴN SÀNG" else "ĐANG CHO THUÊ"

        row1.addView(TextView(this).apply {
            text = badgeText
            setTextColor(badgeColor)
            textSize = 9f
            setTypeface(typeface, Typeface.BOLD)
            background = rounded(badgeBg, dp(4))
            setPadding(dp(8), dp(3), dp(8), dp(3))
        })

        // Row 2: Size + MÃ£
        contentColumn.addView(TextView(this).apply {
            text = "Size: $size | Mã: $idShort"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 12f
        }, matchWrap(top = 4))

        // Spacer â†’ Ä‘áº©y giÃ¡ xuá»‘ng dÆ°á»›i
        contentColumn.addView(View(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        // Row 3: GiÃ¡ thuÃª
        val priceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
        }
        contentColumn.addView(priceRow)

        priceRow.addView(TextView(this).apply {
            text = CurrencyUtils.formatVnd(rentalPrice)
            setTextColor(ContextCompat.getColor(this@CategoryDetailActivity, R.color.brand_primary))
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
        })

        priceRow.addView(TextView(this).apply {
            text = " / NGÀY"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 10f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(4), 0, 0, dp(2))
        })

        return card
    }

    // ======================================================================
    //  HELPERS
    // ======================================================================
    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply { setColor(color); cornerRadius = radius.toFloat() }
    }

    private fun matchWrap(top: Int = 0) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(top) }

    private fun wrapWrap(top: Int = 0) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(top) }

    private fun encodeQueryValue(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name())
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
