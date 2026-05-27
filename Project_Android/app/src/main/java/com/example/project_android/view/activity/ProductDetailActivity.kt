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
import com.example.project_android.navigation.AppNavigator
import com.example.project_android.navigation.AdminAvatarController
import com.example.project_android.network.ApiClient
import com.example.project_android.network.runApi
import com.example.project_android.utils.CurrencyUtils
import com.example.project_android.utils.ImageUtils
import org.json.JSONArray
import org.json.JSONObject

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var imgHero: ImageView
    private lateinit var tvStatusBadge: TextView
    private lateinit var tvName: TextView
    private lateinit var tvSku: TextView
    private lateinit var tvSize: TextView
    private lateinit var tvQuantity: TextView
    private lateinit var tvQuantitySub: TextView
    private lateinit var tvRentalPrice: TextView
    private lateinit var tvDeposit: TextView
    private lateinit var tvTopBarTitle: TextView
    private lateinit var historyContainer: LinearLayout

    private var productId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUI())
        loadProductDetail()
    }

    private fun buildUI(): View {
        val root = FrameLayout(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@ProductDetailActivity, R.color.screen_background))
        }

        val scrollView = ScrollView(this).apply { isFillViewport = true }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(60), 0, dp(100))
        }
        scrollView.addView(content)
        root.addView(scrollView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        // Hero image
        content.addView(buildHeroSection())
        // Product identity
        content.addView(buildIdentitySection())
        // Bento specs grid
        content.addView(buildSpecsGrid())
        // Pricing
        content.addView(buildPricingSection())
        // History
        content.addView(buildHistorySection())

        // Top bar overlay
        root.addView(buildTopBar())

        return root
    }

    // ── Hero Section ──
    private fun buildHeroSection(): View {
        return FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(400))

            imgHero = ImageView(this@ProductDetailActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageResource(android.R.drawable.ic_menu_gallery)
                setBackgroundColor(Color.parseColor("#F5F2FF"))
            }
            addView(imgHero, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

            // Status badge
            tvStatusBadge = TextView(this@ProductDetailActivity).apply {
                text = "SẴN SÀNG"
                setTextColor(Color.WHITE)
                textSize = 10f
                setTypeface(typeface, Typeface.BOLD)
                background = rounded(ContextCompat.getColor(this@ProductDetailActivity, R.color.brand_primary), dp(6))
                setPadding(dp(14), dp(8), dp(14), dp(8))
            }
            addView(tvStatusBadge, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                setMargins(dp(16), dp(16), 0, 0)
            })
        }
    }

    // ── Identity Section ──
    private fun buildIdentitySection(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(8))

            tvName = TextView(this@ProductDetailActivity).apply {
                text = "Đang tải..."
                setTextColor(ContextCompat.getColor(this@ProductDetailActivity, R.color.text_primary))
                textSize = 26f
                setTypeface(typeface, Typeface.BOLD)
            }
            addView(tvName)
        }
    }

    // ── Bento Specs Grid ──
    private fun buildSpecsGrid(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(20))

            // Row 1: Size + Material
            val row1 = LinearLayout(this@ProductDetailActivity).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            addView(row1)

            // Size card (with left border)
            row1.addView(buildSpecCardWithBorder("KÍCH CỠ").also { tvSize = it.second }.first,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) })

            // SKU card
            row1.addView(buildSpecCard("MÃ SẢN PHẨM").also { tvSku = it.second }.first,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(6) })

            // Row 2: Stock status (full width)
            val stockCard = LinearLayout(this@ProductDetailActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                background = rounded(Color.parseColor("#F0ECF9"), dp(14))
                setPadding(dp(18), dp(16), dp(18), dp(16))
                gravity = Gravity.CENTER_VERTICAL
            }
            addView(stockCard, matchWrap(top = 12))

            val stockLeft = LinearLayout(this@ProductDetailActivity).apply {
                orientation = LinearLayout.VERTICAL
            }
            stockCard.addView(stockLeft, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            stockLeft.addView(labelText("TÌNH TRẠNG KHO"))

            val qtyRow = LinearLayout(this@ProductDetailActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.BOTTOM
            }
            stockLeft.addView(qtyRow, matchWrap(top = 4))

            tvQuantity = TextView(this@ProductDetailActivity).apply {
                text = "0"
                setTextColor(ContextCompat.getColor(this@ProductDetailActivity, R.color.brand_primary))
                textSize = 28f
                setTypeface(typeface, Typeface.BOLD)
            }
            qtyRow.addView(tvQuantity)

            tvQuantitySub = TextView(this@ProductDetailActivity).apply {
                text = " có sẵn"
                setTextColor(ContextCompat.getColor(this@ProductDetailActivity, R.color.text_secondary))
                textSize = 20f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(6), 0, 0, dp(2))
            }
            qtyRow.addView(tvQuantitySub)
        }
    }

    private fun buildSpecCardWithBorder(label: String): Pair<View, TextView> {
        val valueView = TextView(this).apply {
            text = "--"
            setTextColor(ContextCompat.getColor(this@ProductDetailActivity, R.color.brand_primary))
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = rounded(Color.parseColor("#F5F2FF"), dp(14))
            clipChildren = true

            addView(View(this@ProductDetailActivity).apply {
                setBackgroundColor(ContextCompat.getColor(this@ProductDetailActivity, R.color.brand_primary))
            }, LinearLayout.LayoutParams(dp(4), LinearLayout.LayoutParams.MATCH_PARENT))

            addView(LinearLayout(this@ProductDetailActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                addView(labelText(label))
                addView(valueView, matchWrap(top = 6))
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        return card to valueView
    }

    private fun buildSpecCard(label: String): Pair<View, TextView> {
        val valueView = TextView(this).apply {
            text = "--"
            setTextColor(ContextCompat.getColor(this@ProductDetailActivity, R.color.text_primary))
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.parseColor("#F5F2FF"), dp(14))
            setPadding(dp(16), dp(14), dp(16), dp(14))
            addView(labelText(label))
            addView(valueView, matchWrap(top = 6))
        }
        return card to valueView
    }

    // ── Pricing Section ──
    private fun buildPricingSection(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(0), dp(20), dp(20))

            addView(sectionLabel("CHI PHÍ THUÊ"))

            val card = LinearLayout(this@ProductDetailActivity).apply {
                orientation = LinearLayout.VERTICAL
                background = rounded(Color.WHITE, dp(16))
                setPadding(dp(18), dp(18), dp(18), dp(18))
                elevation = dp(2).toFloat()
            }
            addView(card, matchWrap(top = 12))

            // Rental price row
            tvRentalPrice = TextView(this@ProductDetailActivity).apply {
                text = "0đ"
                setTextColor(ContextCompat.getColor(this@ProductDetailActivity, R.color.text_primary))
                textSize = 17f
                setTypeface(typeface, Typeface.BOLD)
            }
            card.addView(buildPriceRow("payments", "Giá mỗi ngày", tvRentalPrice, true))

            // Divider
            card.addView(View(this@ProductDetailActivity).apply {
                setBackgroundColor(Color.parseColor("#E4E1EE"))
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                topMargin = dp(14); bottomMargin = dp(14)
            })

            // Deposit row
            tvDeposit = TextView(this@ProductDetailActivity).apply {
                text = "0đ"
                setTextColor(ContextCompat.getColor(this@ProductDetailActivity, R.color.text_primary))
                textSize = 17f
                setTypeface(typeface, Typeface.BOLD)
            }
            card.addView(buildPriceRow("security", "Tiền đặt cọc", tvDeposit, false))
        }
    }

    private fun buildPriceRow(iconName: String, label: String, valueView: TextView, showBadge: Boolean): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            // Icon circle
            addView(LinearLayout(this@ProductDetailActivity).apply {
                gravity = Gravity.CENTER
                background = rounded(Color.parseColor("#EDE9FF"), dp(20))
                addView(ImageView(this@ProductDetailActivity).apply {
                    setImageResource(if (iconName == "payments") R.drawable.ic_payments else R.drawable.ic_security)
                    setColorFilter(ContextCompat.getColor(this@ProductDetailActivity, R.color.brand_primary))
                }, LinearLayout.LayoutParams(dp(20), dp(20)))
            }, LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(12) })

            // Label + value
            addView(LinearLayout(this@ProductDetailActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(this@ProductDetailActivity).apply {
                    text = label
                    setTextColor(ContextCompat.getColor(this@ProductDetailActivity, R.color.text_secondary))
                    textSize = 12f
                })
                addView(valueView)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            if (showBadge) {
                addView(TextView(this@ProductDetailActivity).apply {
                    text = "Standard"
                    setTextColor(Color.parseColor("#2F2EBE"))
                    textSize = 10f
                    setTypeface(typeface, Typeface.BOLD)
                    background = rounded(Color.parseColor("#E1E0FF"), dp(6))
                    setPadding(dp(10), dp(5), dp(10), dp(5))
                })
            }
        }
    }

    // ── History Section ──
    private fun buildHistorySection(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(0), dp(20), dp(24))

            val header = LinearLayout(this@ProductDetailActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            addView(header)

            header.addView(sectionLabel("LỊCH SỬ HOẠT ĐỘNG"),
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            historyContainer = LinearLayout(this@ProductDetailActivity).apply {
                orientation = LinearLayout.VERTICAL
            }
            addView(historyContainer, matchWrap(top = 12))
        }
    }

    // ── Top Bar Overlay ──
    private fun buildTopBar(): View {
        return FrameLayout(this).apply {
            setPadding(dp(12), dp(10), dp(16), dp(10))
            setBackgroundColor(ContextCompat.getColor(this@ProductDetailActivity, R.color.screen_background))

            val backButton = ImageButton(this@ProductDetailActivity).apply {
                setImageResource(R.drawable.ic_arrow_back)
                setColorFilter(ContextCompat.getColor(this@ProductDetailActivity, R.color.brand_primary))
                background = null
                setPadding(dp(8), dp(8), dp(8), dp(8))
                setOnClickListener { finish() }
            }
            addView(backButton, FrameLayout.LayoutParams(dp(40), dp(40)).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
            })

            tvTopBarTitle = TextView(this@ProductDetailActivity).apply {
                text = "Chi tiết sản phẩm"
                setTextColor(ContextCompat.getColor(this@ProductDetailActivity, R.color.brand_primary))
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
            }
            addView(tvTopBarTitle, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            })

            val avatar = ImageView(this@ProductDetailActivity).apply {
                setImageResource(android.R.drawable.ic_menu_myplaces)
                setColorFilter(Color.WHITE)
                setBackgroundResource(R.drawable.bg_circle_avatar)
                contentDescription = "Tài khoản quản trị"
                setPadding(dp(6), dp(6), dp(6), dp(6))
            }
            AdminAvatarController.bind(this@ProductDetailActivity, avatar)
            addView(avatar, FrameLayout.LayoutParams(dp(34), dp(34)).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            })
        }.also {
            it.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.TOP }
        }
    }

    // ── Bottom Action Bar ──
    private fun buildBottomBar(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(12), dp(16), dp(28))
            setBackgroundColor(Color.parseColor("#E8FCF8FF"))
            gravity = Gravity.CENTER_VERTICAL

            // Edit button
            addView(LinearLayout(this@ProductDetailActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                background = rounded(Color.parseColor("#E4E1EE"), dp(24))
                setPadding(dp(16), dp(14), dp(16), dp(14))
                setOnClickListener {
                    Toast.makeText(this@ProductDetailActivity, "Chỉnh sửa sản phẩm", Toast.LENGTH_SHORT).show()
                }
                addView(ImageView(this@ProductDetailActivity).apply {
                    setImageResource(android.R.drawable.ic_menu_edit)
                    setColorFilter(ContextCompat.getColor(this@ProductDetailActivity, R.color.text_primary))
                }, LinearLayout.LayoutParams(dp(18), dp(18)).apply { marginEnd = dp(6) })
                addView(TextView(this@ProductDetailActivity).apply {
                    text = "Chỉnh sửa"
                    setTextColor(ContextCompat.getColor(this@ProductDetailActivity, R.color.text_primary))
                    textSize = 13f
                    setTypeface(typeface, Typeface.BOLD)
                })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(10) })

            // Create order button
            addView(LinearLayout(this@ProductDetailActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                background = rounded(ContextCompat.getColor(this@ProductDetailActivity, R.color.brand_primary), dp(24))
                setPadding(dp(20), dp(14), dp(20), dp(14))
                setOnClickListener { AppNavigator.openCreateOrder(this@ProductDetailActivity) }
                addView(ImageView(this@ProductDetailActivity).apply {
                    setImageResource(android.R.drawable.ic_menu_add)
                    setColorFilter(Color.WHITE)
                }, LinearLayout.LayoutParams(dp(18), dp(18)).apply { marginEnd = dp(6) })
                addView(TextView(this@ProductDetailActivity).apply {
                    text = "Tạo đơn thuê"
                    setTextColor(Color.WHITE)
                    textSize = 13f
                    setTypeface(typeface, Typeface.BOLD)
                })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f))
        }.also {
            it.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.BOTTOM }
        }
    }

    // ── Data Loading ──
    private fun loadProductDetail() {
        productId = intent.getStringExtra("product_id") ?: ""
        if (productId.isBlank()) {
            Toast.makeText(this, "Thiếu mã sản phẩm", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        runApi(
            loadingMessage = "Đang tải chi tiết sản phẩm...",
            request = { ApiClient.get("/products/$productId/detail") }
        ) { result ->
            val data = JSONObject(result.body).getJSONObject("data")
            bindProduct(data.getJSONObject("product"))
            bindHistory(data.getJSONArray("rentalHistory"))
        }
    }

    private fun bindProduct(p: JSONObject) {
        tvName.text = p.optString("name", "Sản phẩm")
        tvSize.text = p.optString("size", "--")
        tvSku.text = p.optString("id", "--").take(8).uppercase()
        tvRentalPrice.text = CurrencyUtils.formatVnd(p.optDouble("rentalPrice", 0.0))
        tvDeposit.text = CurrencyUtils.formatVnd(p.optDouble("deposit", 0.0))

        val qty = p.optInt("quantity", 0)
        tvQuantity.text = qty.toString()
        tvQuantitySub.text = " sản phẩm"

        val status = p.optString("status", "available")
        tvStatusBadge.text = statusText(status)
        tvStatusBadge.background = rounded(statusColor(status), dp(6))

        ImageUtils.bindImage(
            imgHero,
            p.optString("imageUrl", ""),
            android.R.drawable.ic_menu_gallery
        )
    }

    private fun bindHistory(history: JSONArray) {
        historyContainer.removeAllViews()
        if (history.length() == 0) {
            historyContainer.addView(TextView(this).apply {
                text = "Chưa có lịch sử hoạt động."
                setTextColor(ContextCompat.getColor(this@ProductDetailActivity, R.color.text_secondary))
                textSize = 14f
                setPadding(0, dp(8), 0, dp(8))
            })
            return
        }

        for (i in 0 until history.length()) {
            val order = history.getJSONObject(i)
            historyContainer.addView(buildHistoryItem(order, i < history.length() - 1))
        }
    }

    private fun buildHistoryItem(order: JSONObject, showLine: Boolean): View {
        val status = order.optString("status", "")
        val isOverdue = status == "overdue" || status == "overdue_history"
        val customerName = order.optString("customerName", "Khách hàng")
        val pickupDate = formatDate(order.optString("pickupDate", ""))
        val returnDate = formatDate(order.optString("returnDate", ""))

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), dp(10), dp(4), dp(10))

            // Timeline icon + line
            addView(LinearLayout(this@ProductDetailActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL

                addView(LinearLayout(this@ProductDetailActivity).apply {
                    gravity = Gravity.CENTER
                    background = rounded(
                        if (isOverdue) Color.parseColor("#1ABA1A1A") else Color.parseColor("#1A4430E8"),
                        dp(10)
                    )
                    addView(ImageView(this@ProductDetailActivity).apply {
                        setImageResource(
                            if (status == "renting") android.R.drawable.ic_menu_my_calendar
                            else if (status == "returned") android.R.drawable.ic_menu_send
                            else android.R.drawable.ic_menu_recent_history
                        )
                        setColorFilter(
                            if (isOverdue) Color.parseColor("#BA1A1A")
                            else ContextCompat.getColor(this@ProductDetailActivity, R.color.brand_primary)
                        )
                    }, LinearLayout.LayoutParams(dp(20), dp(20)))
                }, LinearLayout.LayoutParams(dp(38), dp(38)))

                if (showLine) {
                    addView(View(this@ProductDetailActivity).apply {
                        setBackgroundColor(Color.parseColor("#E4E1EE"))
                    }, LinearLayout.LayoutParams(dp(2), 0, 1f).apply { topMargin = dp(4) })
                }
            }, LinearLayout.LayoutParams(dp(42), LinearLayout.LayoutParams.MATCH_PARENT))

            // Content
            addView(LinearLayout(this@ProductDetailActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), 0, 0, dp(8))

                addView(LinearLayout(this@ProductDetailActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(TextView(this@ProductDetailActivity).apply {
                        text = statusLabel(status)
                        setTextColor(ContextCompat.getColor(this@ProductDetailActivity, R.color.text_primary))
                        textSize = 13f
                        setTypeface(typeface, Typeface.BOLD)
                    }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    addView(TextView(this@ProductDetailActivity).apply {
                        text = pickupDate
                        setTextColor(ContextCompat.getColor(this@ProductDetailActivity, R.color.text_secondary))
                        textSize = 10f
                    })
                })

                addView(TextView(this@ProductDetailActivity).apply {
                    text = "Khách hàng: $customerName. Hạn trả: $returnDate"
                    setTextColor(ContextCompat.getColor(this@ProductDetailActivity, R.color.text_secondary))
                    textSize = 12f
                    setLineSpacing(0f, 1.3f)
                }, matchWrap(top = 4))
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    // ── Helpers ──
    private fun statusText(s: String) = when (s) {
        "available" -> "SẴN SÀNG"
        "cleaning" -> "ĐANG GIẶT ỦI"
        "renting" -> "ĐANG THUÊ"
        else -> s.uppercase()
    }

    private fun statusColor(s: String) = when (s) {
        "available" -> ContextCompat.getColor(this, R.color.brand_primary)
        "renting" -> Color.parseColor("#B5478C")
        "cleaning" -> Color.parseColor("#777587")
        else -> Color.parseColor("#777587")
    }

    private fun statusLabel(s: String) = when (s) {
        "returned" -> "Đã trả hàng - Hoàn tất"
        "renting" -> "Đang cho thuê"
        "overdue", "overdue_history" -> "Quá hạn trả"
        else -> "Đơn hàng"
    }

    private fun formatDate(v: String): String {
        val parts = v.split("-")
        return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else v
    }

    private fun labelText(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(ContextCompat.getColor(this@ProductDetailActivity, R.color.text_secondary))
            textSize = 10f
            setTypeface(typeface, Typeface.BOLD)
        }
    }

    private fun sectionLabel(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(ContextCompat.getColor(this@ProductDetailActivity, R.color.text_secondary))
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
        }
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply { setColor(color); cornerRadius = radius.toFloat() }
    }

    private fun matchWrap(top: Int = 0) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(top) }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
