package com.example.project_android.view.activity

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.view.animation.AlphaAnimation
import android.view.animation.ScaleAnimation
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.example.project_android.R
import com.example.project_android.email.DiscountEmailSender
import com.example.project_android.model.dao.DiscountCodeDao
import com.example.project_android.model.dao.DiscountEmailLogDao
import com.example.project_android.model.database.DatabaseHelper
import com.example.project_android.model.entity.DiscountCode
import com.example.project_android.navigation.AppNavigator
import com.example.project_android.navigation.AdminAvatarController
import com.example.project_android.network.ApiClient
import com.example.project_android.network.runApi
import com.example.project_android.utils.CurrencyUtils
import com.example.project_android.utils.CustomerImageUtils
import org.json.JSONArray
import org.json.JSONObject

class CustomerDetailActivity : AppCompatActivity() {

    private lateinit var tvName: TextView
    private lateinit var tvMemberSince: TextView
    private lateinit var tvTotalRentals: TextView
    private lateinit var tvTotalSpent: TextView
    private lateinit var tvActiveRentals: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvNote: TextView
    private lateinit var tvDressSize: TextView
    private lateinit var tvShoeSize: TextView
    private lateinit var tvActiveCount: TextView
    private lateinit var imgAvatar: ImageView
    private lateinit var activeRentalsContainer: LinearLayout
    private lateinit var historyContainer: LinearLayout
    private lateinit var tvRewardCode: TextView
    private lateinit var tvRewardDescription: TextView
    private lateinit var tvRewardStatus: TextView
    private lateinit var btnSendDiscountEmail: AppCompatButton

    private var customerPhone = ""
    private var customerId = ""
    private var customerName = ""
    private var customerEmail = ""
    private var customerTotalSpent = 0.0
    private var recommendedDiscount: DiscountCode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createContentView())
        loadCustomerDetail()
    }

    private fun createContentView(): View {
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@CustomerDetailActivity, R.color.screen_background))
            isFillViewport = true
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(28))
        }
        scrollView.addView(root)

        root.addView(createTopBar())
        root.addView(createProfileHeader(), matchWrap(top = 24))
        root.addView(createStatsSection(), matchWrap(top = 24))
        root.addView(createRewardEmailSection(), matchWrap(top = 18))
        root.addView(createContactSection(), matchWrap(top = 24))
        root.addView(createSizeSection(), matchWrap(top = 18))
        root.addView(createNoteSection(), matchWrap(top = 18))
        root.addView(createActiveRentalsSection(), matchWrap(top = 24))
        root.addView(createHistorySection(), matchWrap(top = 24))

        return scrollView
    }

    private fun createTopBar(): View {
        return android.widget.FrameLayout(this).apply {
            addView(TextView(this@CustomerDetailActivity).apply {
                text = "Hồ sơ khách hàng"
                setTextColor(ContextCompat.getColor(this@CustomerDetailActivity, R.color.brand_primary))
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
            }, android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ))

            addView(ImageButton(this@CustomerDetailActivity).apply {
                setImageResource(R.drawable.ic_arrow_back)
                setColorFilter(ContextCompat.getColor(this@CustomerDetailActivity, R.color.brand_primary))
                background = null
                setPadding(dp(8), dp(8), dp(8), dp(8))
                setOnClickListener { finish() }
            }, android.widget.FrameLayout.LayoutParams(dp(40), dp(40), Gravity.START or Gravity.CENTER_VERTICAL))

            val adminAvatar = ImageView(this@CustomerDetailActivity).apply {
                setImageResource(android.R.drawable.ic_menu_myplaces)
                setColorFilter(Color.WHITE)
                setBackgroundResource(R.drawable.bg_circle_avatar)
                contentDescription = "Tài khoản quản trị"
                setPadding(dp(6), dp(6), dp(6), dp(6))
            }
            AdminAvatarController.bind(this@CustomerDetailActivity, adminAvatar)
            addView(adminAvatar, android.widget.FrameLayout.LayoutParams(dp(34), dp(34), Gravity.END or Gravity.CENTER_VERTICAL))
        }
    }

    private fun createProfileHeader(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL

            imgAvatar = ImageView(this@CustomerDetailActivity).apply {
                setImageResource(android.R.drawable.ic_menu_myplaces)
                setColorFilter(ContextCompat.getColor(this@CustomerDetailActivity, R.color.text_secondary))
                background = rounded(Color.WHITE, dp(34))
                setPadding(dp(28), dp(28), dp(28), dp(28))
            }
            addView(imgAvatar, LinearLayout.LayoutParams(dp(132), dp(132)))

            tvName = TextView(this@CustomerDetailActivity).apply {
                text = "Đang tải..."
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(this@CustomerDetailActivity, R.color.text_primary))
                textSize = 26f
                setTypeface(typeface, Typeface.BOLD)
            }
            addView(tvName, matchWrap(top = 16))

            tvMemberSince = TextView(this@CustomerDetailActivity).apply {
                text = "Khách hàng từ --"
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(this@CustomerDetailActivity, R.color.text_secondary))
                textSize = 14f
            }
            addView(tvMemberSince, matchWrap(top = 12))
        }
    }

    private fun createStatsSection(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            tvTotalRentals = addStatCard(this, "TỔNG LƯỢT THUÊ", "0", R.color.brand_primary)
            tvTotalSpent = addStatCard(this, "TỔNG CHI TIÊU", "0đ", R.color.checkbox_checked)
            tvActiveRentals = addStatCard(this, "ĐƠN ĐANG THUÊ", "0", R.color.accent_magenta)
        }
    }

    private fun createRewardEmailSection(): View {
        return card(Color.WHITE).apply {
            addView(sectionTitle("Ưu đãi theo tổng chi tiêu", android.R.drawable.ic_dialog_email))

            tvRewardCode = TextView(this@CustomerDetailActivity).apply {
                text = "Đang kiểm tra điều kiện..."
                setTextColor(ContextCompat.getColor(this@CustomerDetailActivity, R.color.brand_primary))
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
            }
            addView(tvRewardCode, matchWrap(top = 14))

            tvRewardDescription = TextView(this@CustomerDetailActivity).apply {
                setTextColor(ContextCompat.getColor(this@CustomerDetailActivity, R.color.text_secondary))
                textSize = 13f
                setLineSpacing(0f, 1.15f)
            }
            addView(tvRewardDescription, matchWrap(top = 8))

            btnSendDiscountEmail = AppCompatButton(this@CustomerDetailActivity).apply {
                text = "Gửi mã giảm giá qua email"
                setTextColor(Color.WHITE)
                textSize = 14f
                isAllCaps = false
                isEnabled = false
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(this@CustomerDetailActivity, R.color.brand_primary)
                )
                setOnClickListener { sendDiscountEmail() }
            }
            addView(btnSendDiscountEmail, matchWrap(top = 16))

            tvRewardStatus = TextView(this@CustomerDetailActivity).apply {
                visibility = View.GONE
                textSize = 13f
            }
            addView(tvRewardStatus, matchWrap(top = 10))
        }
    }

    private fun createContactSection(): View {
        return card(Color.WHITE).apply {
            addView(sectionTitle("Thông tin liên hệ", android.R.drawable.ic_menu_info_details))
            tvEmail = addContactLine(this, android.R.drawable.ic_dialog_email, "EMAIL", "")
            tvPhone = addContactLine(this, android.R.drawable.ic_menu_call, "SỐ ĐIỆN THOẠI", "")
            tvAddress = addContactLine(this, R.drawable.ic_location_pin, "ĐỊA CHỈ", "")
        }
    }

    private fun createSizeSection(): View {
        return card(Color.parseColor("#F0ECF9")).apply {
            setPadding(dp(30), dp(28), dp(30), dp(28))
            addView(heading("Thông số kích cỡ"))

            val row = LinearLayout(this@CustomerDetailActivity).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            addView(row, matchWrap(top = 24))
            val viewDressSize = sizeBox("SIZE VÁY", "--") as LinearLayout
            tvDressSize = viewDressSize.getChildAt(1) as TextView
            row.addView(viewDressSize, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(10)
            })

            val viewShoeSize = sizeBox("SIZE GIÀY", "--") as LinearLayout
            tvShoeSize = viewShoeSize.getChildAt(1) as TextView
            row.addView(viewShoeSize, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(10)
            })
        }
    }

    private fun createNoteSection(): View {
        return card(Color.parseColor("#FFD9E4")).apply {
            addView(sectionTitle("Ghi chú", R.drawable.ic_note_document))
            tvNote = TextView(this@CustomerDetailActivity).apply {
                text = "Không có ghi chú"
                setTextColor(Color.parseColor("#8C0053"))
                textSize = 14f
                setLineSpacing(0f, 1.15f)
            }
            addView(tvNote, matchWrap(top = 10))
        }
    }

    private fun createActiveRentalsSection(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            val header = LinearLayout(this@CustomerDetailActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            addView(header)
            header.addView(heading("Đang thuê"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            tvActiveCount = TextView(this@CustomerDetailActivity).apply {
                text = "0 sản phẩm"
                setTextColor(ContextCompat.getColor(this@CustomerDetailActivity, R.color.brand_primary))
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
            }
            header.addView(tvActiveCount)

            activeRentalsContainer = LinearLayout(this@CustomerDetailActivity).apply {
                orientation = LinearLayout.VERTICAL
            }
            addView(activeRentalsContainer, matchWrap(top = 12))
        }
    }

    private fun createHistorySection(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            val header = LinearLayout(this@CustomerDetailActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            addView(header)
            header.addView(heading("Lịch sử thuê"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            historyContainer = card(Color.WHITE)
            addView(historyContainer, matchWrap(top = 12))
        }
    }

    private fun loadCustomerDetail() {
        val customerId = intent.getStringExtra("customer_id")
        if (customerId.isNullOrBlank()) {
            Toast.makeText(this, "Thiếu mã khách hàng", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        runApi(
            loadingMessage = "Đang tải chi tiết khách hàng...",
            request = { ApiClient.get("/customers/$customerId/detail") }
        ) { result ->
            bindCustomerDetail(JSONObject(result.body).getJSONObject("data"))
        }
    }

    private fun bindCustomerDetail(detail: JSONObject) {
        val customer = detail.getJSONObject("customer")
        val summary = detail.getJSONObject("summary")
        val orders = detail.getJSONArray("orders")

        customerId = customer.optString("id")
        customerName = customer.optString("fullName", "Khách hàng")
        customerEmail = customer.optString("email")
        customerPhone = customer.optString("phone")
        customerTotalSpent = summary.optDouble("totalSpent")
        tvName.text = customerName
        tvMemberSince.text = "Khách hàng từ ${formatCreatedAt(customer.optString("createdAt"))}"
        tvEmail.text = customerEmail.ifBlank { "Chưa có" }
        tvPhone.text = customerPhone.ifBlank { "Chưa có" }
        tvAddress.text = customer.optString("address").ifBlank { "Chưa có" }
        tvNote.text = customer.optString("note").ifBlank { "Không có ghi chú" }
        tvDressSize.text = customer.optString("dressSize").ifBlank { "--" }
        tvShoeSize.text = customer.optString("shoeSize").ifBlank { "--" }

        if (CustomerImageUtils.bindAvatar(
                imgAvatar,
                customer.optString("avatar", ""),
                customerId,
                customerPhone
            )
        ) {
            imgAvatar.imageTintList = null
            imgAvatar.clearColorFilter()
            imgAvatar.setPadding(0, 0, 0, 0)
            imgAvatar.clipToOutline = true
        }

        tvTotalRentals.text = summary.optLong("orderCount").toString()
        tvTotalSpent.text = CurrencyUtils.formatVnd(customerTotalSpent)
        tvActiveRentals.text = summary.optLong("activeCount").toString()

        bindRewardOffer()
        bindActiveRentals(orders)
        bindHistory(orders)
    }

    private fun bindRewardOffer() {
        val database = DatabaseHelper(this).readableDatabase
        recommendedDiscount = DiscountCodeDao(database)
            .recommendForTotalSpent(customerTotalSpent)

        val discount = recommendedDiscount
        if (discount == null) {
            tvRewardCode.text = "Hiện chưa có ưu đãi dành cho khách hàng"
            tvRewardDescription.text =
                "Tổng chi tiêu hiện tại: ${CurrencyUtils.formatVnd(customerTotalSpent)}\n" +
                    "Khách hàng chưa đủ điều kiện nhận mã giảm giá mới."
            btnSendDiscountEmail.text = "Chưa có ưu đãi để gửi"
            btnSendDiscountEmail.isEnabled = false
            return
        }

        val benefit = if (discount.discountType == "percent") {
            "Giảm ${discount.discountValue.toInt()}%, tối đa ${CurrencyUtils.formatVnd(discount.maximumDiscount)}"
        } else {
            "Giảm tối đa ${CurrencyUtils.formatVnd(discount.discountValue)}"
        }
        tvRewardCode.text = "Mã ${discount.code}"
        tvRewardDescription.text =
            "$benefit - Đơn thuê từ ${CurrencyUtils.formatVnd(discount.minimumOrder)}\n" +
                "Hạn dùng: ${discount.endDate}"
        btnSendDiscountEmail.text = "Gửi mã ưu đãi qua email"
        btnSendDiscountEmail.isEnabled = customerEmail.isNotBlank()
        if (customerEmail.isBlank()) {
            showRewardStatus("Khách hàng chưa có email nhận mã.", true)
        }
    }

    private fun sendDiscountEmail() {
        val discount = recommendedDiscount ?: return
        if (customerEmail.isBlank()) {
            showRewardStatus("Khách hàng chưa có email nhận mã.", true)
            return
        }

        btnSendDiscountEmail.isEnabled = false
        showRewardStatus("Đang gửi mã ${discount.code} tới $customerEmail...", false)

        Thread {
            val result = DiscountEmailSender.send(
                recipientEmail = customerEmail,
                customerName = customerName,
                totalSpent = customerTotalSpent,
                discount = discount
            )
            val status = if (result.isSuccess) "sent" else "failed"
            val error = result.exceptionOrNull()?.message.orEmpty()
            runCatching {
                DiscountEmailLogDao(DatabaseHelper(applicationContext).writableDatabase).insert(
                    customerId = customerId,
                    recipientEmail = customerEmail,
                    totalSpent = customerTotalSpent,
                    discount = discount,
                    status = status,
                    errorMessage = error
                )
            }

            runOnUiThread {
                if (result.isSuccess) {
                    showRewardStatus("Đã gửi mã ${discount.code} tới $customerEmail.", false)
                    bindRewardOffer()
                    Toast.makeText(this, "Gửi email thành công", Toast.LENGTH_SHORT).show()
                } else {
                    showRewardStatus("Gửi email thất bại: $error", true)
                    btnSendDiscountEmail.isEnabled = true
                }
            }
        }.start()
    }

    private fun showRewardStatus(message: String, isError: Boolean) {
        tvRewardStatus.visibility = View.VISIBLE
        tvRewardStatus.text = message
        tvRewardStatus.setTextColor(
            if (isError) Color.parseColor("#BA1A1A") else ContextCompat.getColor(this, R.color.brand_primary)
        )
    }

    private fun bindActiveRentals(orders: JSONArray) {
        activeRentalsContainer.removeAllViews()
        val activeOrders = (0 until orders.length())
            .map { orders.getJSONObject(it) }
            .filter { it.optString("status") == "renting" || it.optString("status") == "overdue" }

        tvActiveCount.text = "${activeOrders.size} sản phẩm"

        if (activeOrders.isEmpty()) {
            activeRentalsContainer.addView(emptyText("Khách hàng chưa có sản phẩm đang thuê."))
            return
        }

        activeOrders.take(2).forEachIndexed { index, order ->
            activeRentalsContainer.addView(activeRentalCard(order, index), matchWrap(top = if (index == 0) 0 else 10))
        }
    }

    private fun bindHistory(orders: JSONArray) {
        historyContainer.removeAllViews()
        historyContainer.addView(historyHeader())
        val historyOrders = (0 until orders.length())
            .map { orders.getJSONObject(it) }
            .filter { it.optString("status") != "renting" && it.optString("status") != "overdue" }

        if (historyOrders.isEmpty()) {
            historyContainer.addView(emptyHistoryText("Chưa có lịch sử thuê."), matchWrap(top = 12))
            return
        }

        historyOrders.take(5).forEachIndexed { index, order ->
            historyContainer.addView(historyRow(order, index), matchWrap(top = 10))
        }
    }

    private fun activeRentalCard(order: JSONObject, index: Int): View {
        val product = order.optJSONArray("productIds")?.optJSONObject(0)
        val status = order.optString("status")
        val isOverdue = status == "overdue"

        return card(Color.WHITE).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(10), dp(10), dp(10), dp(10))

            addView(ImageView(this@CustomerDetailActivity).apply {
                setImageResource(if (index % 2 == 0) R.drawable.img_create_order_product_1 else R.drawable.img_create_order_product_2)
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = rounded(Color.parseColor("#F0ECF9"), dp(8))
            }, LinearLayout.LayoutParams(dp(84), dp(104)))

            addView(LinearLayout(this@CustomerDetailActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, 0, 0)

                addView(TextView(this@CustomerDetailActivity).apply {
                    text = product?.optString("name") ?: order.optString("code")
                    setTextColor(ContextCompat.getColor(this@CustomerDetailActivity, R.color.text_primary))
                    textSize = 15f
                    setTypeface(typeface, Typeface.BOLD)
                })

                addView(TextView(this@CustomerDetailActivity).apply {
                    text = "HẠN TRẢ: ${formatDate(order.optString("returnDate"))}"
                    setTextColor(if (isOverdue) Color.parseColor("#BA1A1A") else ContextCompat.getColor(this@CustomerDetailActivity, R.color.text_secondary))
                    textSize = 10f
                    setTypeface(typeface, Typeface.BOLD)
                }, matchWrap(top = 4))

                addView(statusChip(if (isOverdue) "QUÁ HẠN" else "TRONG HẠN", isOverdue), wrapStart(top = 22))
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    private fun historyHeader(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = rounded(Color.parseColor("#F5F2FF"), dp(8))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            addHistoryCell(this, "SẢN PHẨM", 3f, true, 16)
            addHistoryCell(this, "NGÀY THUÊ", 2f, true, 8)
            addHistoryCell(this, "TRẠNG THÁI", 2f, true)
        }
    }

    private fun historyRow(order: JSONObject, index: Int): View {
        val product = order.optJSONArray("productIds")?.optJSONObject(0)
        val status = order.optString("status")

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))

            addView(LinearLayout(this@CustomerDetailActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                addView(ImageView(this@CustomerDetailActivity).apply {
                    setImageResource(if (index % 2 == 0) R.drawable.img_create_order_product_1 else R.drawable.img_create_order_product_2)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    background = rounded(Color.parseColor("#F0ECF9"), dp(4))
                }, LinearLayout.LayoutParams(dp(28), dp(36)))

                addView(TextView(this@CustomerDetailActivity).apply {
                    text = product?.optString("name") ?: order.optString("code")
                    setTextColor(ContextCompat.getColor(this@CustomerDetailActivity, R.color.text_primary))
                    textSize = 13f
                    maxLines = 3
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(8)
                })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f).apply {
                marginEnd = dp(16)
            })
            addHistoryCell(this, formatDate(order.optString("pickupDate")), 2f, false, 8)
            
            // Bọc chip trong một LinearLayout để chip không bị kéo giãn full cột
            val chipContainer = LinearLayout(this@CustomerDetailActivity).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                addView(statusChip(statusLabel(status), status == "overdue" || status == "overdue_history"))
            }
            addView(chipContainer, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f))
        }
    }

    private fun addStatCard(parent: LinearLayout, label: String, value: String, colorRes: Int): TextView {
        val borderColor = ContextCompat.getColor(this, colorRes)
        val valueView = TextView(this).apply {
            text = value
            setTextColor(ContextCompat.getColor(this@CustomerDetailActivity, R.color.text_primary))
            textSize = 28f
            setTypeface(typeface, Typeface.BOLD)
        }

        parent.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = rounded(Color.parseColor("#F5F2FF"), dp(12))

            addView(View(this@CustomerDetailActivity).apply {
                setBackgroundColor(borderColor)
            }, LinearLayout.LayoutParams(dp(4), LinearLayout.LayoutParams.MATCH_PARENT))

            addView(LinearLayout(this@CustomerDetailActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(14), dp(18), dp(14))
                addView(sectionSmall(label))
                addView(valueView, matchWrap(top = 4))
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }, matchWrap(top = 10))

        return valueView
    }

    private fun addInfoLine(parent: LinearLayout, label: String, value: String): TextView {
        val valueView = TextView(this).apply {
            text = value
            setTextColor(ContextCompat.getColor(this@CustomerDetailActivity, R.color.text_primary))
            textSize = 14f
        }
        parent.addView(sectionSmall(label), matchWrap(top = 16))
        parent.addView(valueView, matchWrap(top = 3))
        return valueView
    }

    private fun addContactLine(parent: LinearLayout, iconRes: Int, label: String, value: String): TextView {
        val valueView = TextView(this).apply {
            text = value
            setTextColor(ContextCompat.getColor(this@CustomerDetailActivity, R.color.text_primary))
            textSize = 14f
            setLineSpacing(0f, 1.12f)
        }

        parent.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP

            addView(ImageView(this@CustomerDetailActivity).apply {
                setImageResource(iconRes)
                setColorFilter(ContextCompat.getColor(this@CustomerDetailActivity, R.color.text_primary))
            }, LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                topMargin = dp(2)
            })

            addView(LinearLayout(this@CustomerDetailActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), 0, 0, 0)

                addView(sectionSmall(label))
                addView(valueView, matchWrap(top = 4))
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }, matchWrap(top = 18))

        return valueView
    }

    private fun addHistoryCell(parent: LinearLayout, textValue: String, weight: Float, isHeader: Boolean, marginEndDp: Int = 0) {
        parent.addView(TextView(this).apply {
            text = textValue
            setTextColor(ContextCompat.getColor(this@CustomerDetailActivity, if (isHeader) R.color.text_secondary else R.color.text_primary))
            textSize = if (isHeader) 10f else 13f
            if (isHeader) setTypeface(typeface, Typeface.BOLD)
            maxLines = 2
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight).apply {
            if (marginEndDp > 0) marginEnd = dp(marginEndDp)
        })
    }

    private fun sizeBox(label: String, value: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(Color.WHITE, dp(12))
            setPadding(dp(14), dp(18), dp(14), dp(18))
            addView(TextView(this@CustomerDetailActivity).apply {
                text = label
                setTextColor(ContextCompat.getColor(this@CustomerDetailActivity, R.color.text_secondary))
                textSize = 10f
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
            }, matchWrap())
            addView(TextView(this@CustomerDetailActivity).apply {
                text = value
                setTextColor(ContextCompat.getColor(this@CustomerDetailActivity, R.color.brand_primary))
                textSize = 24f
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
            }, matchWrap(top = 6))
        }
    }

    private fun statusChip(textValue: String, isError: Boolean): TextView {
        return TextView(this).apply {
            text = textValue
            setTextColor(if (isError) Color.parseColor("#BA1A1A") else ContextCompat.getColor(this@CustomerDetailActivity, R.color.brand_primary))
            textSize = 9f
            setTypeface(typeface, Typeface.BOLD)
            background = rounded(if (isError) Color.parseColor("#FFDAD6") else Color.parseColor("#E1E0FF"), dp(4))
            setPadding(dp(8), dp(5), dp(8), dp(5))
            gravity = Gravity.CENTER
        }
    }

    private fun sectionTitle(textValue: String, iconRes: Int): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            addView(ImageView(this@CustomerDetailActivity).apply {
                setImageResource(iconRes)
                setColorFilter(ContextCompat.getColor(this@CustomerDetailActivity, R.color.brand_primary))
            }, LinearLayout.LayoutParams(dp(22), dp(22)))

            addView(heading(textValue), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(8)
            })
        }
    }

    private fun heading(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            setTextColor(ContextCompat.getColor(this@CustomerDetailActivity, R.color.text_primary))
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
        }
    }

    private fun sectionSmall(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            setTextColor(ContextCompat.getColor(this@CustomerDetailActivity, R.color.text_secondary))
            textSize = 10f
            setTypeface(typeface, Typeface.BOLD)
        }
    }

    private fun emptyText(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            setTextColor(ContextCompat.getColor(this@CustomerDetailActivity, R.color.text_secondary))
            textSize = 14f
        }
    }

    private fun emptyHistoryText(textValue: String): TextView {
        return emptyText(textValue).apply {
            setPadding(dp(12), dp(4), dp(12), dp(4))
        }
    }

    private fun statusLabel(status: String): String {
        return when (status) {
            "returned" -> "ĐÃ TRẢ"
            "overdue", "overdue_history" -> "QUÁ HẠN"
            "renting" -> "ĐANG THUÊ"
            else -> "TRONG HẠN"
        }
    }

    private fun formatDate(value: String): String {
        val parts = value.split("-")
        return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else value
    }

    private fun formatCreatedAt(value: String): String {
        return value.take(10).ifBlank { "--" }
    }

    private fun card(color: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(color, dp(12))
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }
    }

    private fun matchWrap(top: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(top)
        }
    }

    private fun wrapCenter(top: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(top)
            gravity = Gravity.CENTER_HORIZONTAL
        }
    }

    private fun wrapStart(top: Int = 0): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(top)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

}
