package com.example.project_android.view.activity

import com.example.project_android.R

import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.project_android.navigation.AppNavigator
import com.example.project_android.navigation.SidebarController
import com.example.project_android.network.ApiClient
import com.example.project_android.network.runApi
import com.example.project_android.utils.CustomerImageUtils
import org.json.JSONObject

class CustomerActivity : AppCompatActivity() {

    private lateinit var edtSearch: EditText
    private lateinit var customerListContainer: LinearLayout
    private lateinit var fabAddCustomer: LinearLayout
    private lateinit var navHome: LinearLayout
    private lateinit var navOrders: LinearLayout
    private lateinit var navProducts: LinearLayout
    private lateinit var navCustomers: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer)

        initViews()
        SidebarController.bindFromActivity(this)
        setupEvents()
        loadCustomers()
    }

    override fun onResume() {
        super.onResume()
        loadCustomers(showToast = false)
    }

    private fun initViews() {
        edtSearch = findViewById(R.id.edtSearch)
        customerListContainer = findViewById(R.id.customerListContainer)
        fabAddCustomer = findViewById(R.id.fabAddCustomer)
        navHome = findViewById(R.id.navHome)
        navOrders = findViewById(R.id.navOrders)
        navProducts = findViewById(R.id.navProducts)
        navCustomers = findViewById(R.id.navCustomers)
    }

    private fun setupEvents() {
        navHome.setOnClickListener { AppNavigator.openDashboard(this) }
        navOrders.setOnClickListener { AppNavigator.openOrders(this) }
        navProducts.setOnClickListener { AppNavigator.openProducts(this) }
        navCustomers.setOnClickListener { loadCustomers() }

        // Bắt sự kiện bàn phím ảo (Soft Keyboard)
        edtSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH || 
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                loadCustomers(showToast = true)
                hideKeyboard()
                true
            } else {
                false
            }
        }

        // Bắt sự kiện bàn phím vật lý (Hardware Keyboard - Thường gặp trên Emulator)
        edtSearch.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN && keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                loadCustomers(showToast = true)
                hideKeyboard()
                true
            } else {
                false
            }
        }

        // Live Search: Tìm kiếm ngay khi gõ (Không hiện Toast để tránh spam)
        edtSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                loadCustomers(showToast = false)
            }
        })

        fabAddCustomer.setOnClickListener {
            AppNavigator.openAddCustomer(this)
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(edtSearch.windowToken, 0)
    }

    private fun loadCustomers(showToast: Boolean = true) {
        val search = edtSearch.text.toString().trim()
        val query = if (search.isEmpty()) "" else "?search=$search"

        runApi(
            loadingMessage = if (showToast) "Đang tìm kiếm..." else null,
            request = { ApiClient.get("/customers$query") }
        ) { result ->
            val customers = try {
                JSONObject(result.body).getJSONArray("data")
            } catch (e: Exception) {
                org.json.JSONArray()
            }
            
            customerListContainer.removeAllViews()
            val inflater = android.view.LayoutInflater.from(this)

            for (i in 0 until customers.length()) {
                val customer = customers.getJSONObject(i)
                val id = customer.getString("id")
                val name = customer.getString("fullName")
                val email = customer.optString("email", "")
                val phone = customer.optString("phone", "")
                val status = customer.optString("status", "active")
                val dressSize = customer.optString("dressSize", "").ifEmpty { "--" }
                val shoeSize = customer.optString("shoeSize", "").ifEmpty { "--" }
                val avatarStr = customer.optString("avatar", "")
                
                val itemView = inflater.inflate(R.layout.item_customer, customerListContainer, false)
                
                itemView.findViewById<android.widget.TextView>(R.id.tvName).text = name
                itemView.findViewById<android.widget.TextView>(R.id.tvEmail).text = email
                itemView.findViewById<android.widget.TextView>(R.id.tvPhone).text = phone
                itemView.findViewById<android.widget.TextView>(R.id.tvDressSize).text = "KÍCH CỠ: $dressSize"
                itemView.findViewById<android.widget.TextView>(R.id.tvShoeSize).text = "GIÀY: $shoeSize"

                val imgCustomerAvatar = itemView.findViewById<android.widget.ImageView>(R.id.imgCustomerAvatar)
                if (CustomerImageUtils.bindAvatar(imgCustomerAvatar, avatarStr, id, phone)) {
                    imgCustomerAvatar.imageTintList = null
                    imgCustomerAvatar.clearColorFilter()
                    imgCustomerAvatar.setPadding(0, 0, 0, 0)
                    imgCustomerAvatar.clipToOutline = true
                }

                val tvStatus = itemView.findViewById<android.widget.TextView>(R.id.tvStatus)
                if (status == "active") {
                    tvStatus.text = "ĐANG HOẠT ĐỘNG"
                    tvStatus.setBackgroundResource(R.drawable.bg_status_ready)
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#4432E6"))
                } else {
                    tvStatus.text = "NGỪNG HOẠT ĐỘNG"
                    tvStatus.setBackgroundResource(R.drawable.bg_status_neutral)
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#747474"))
                }

                tvStatus.setOnClickListener {
                    val newStatus = if (status == "active") "inactive" else "active"
                    updateCustomerStatus(id, newStatus)
                }

                itemView.setOnClickListener { AppNavigator.openCustomerDetail(this, id) }
                val btnMore = itemView.findViewById<android.widget.ImageView>(R.id.btnDelete)
                btnMore.setOnClickListener { view ->
                    val popupMenu = android.widget.PopupMenu(this, view)
                    popupMenu.menu.add(0, 1, 0, "Sửa thông tin")
                    popupMenu.menu.add(0, 2, 0, "Xóa khách hàng")
                    popupMenu.setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            1 -> AppNavigator.openEditCustomer(this, id)
                            2 -> deleteCustomer(id)
                        }
                        true
                    }
                    popupMenu.show()
                }

                customerListContainer.addView(itemView)
            }

            if (showToast && customers.length() == 0) {
                Toast.makeText(this, "Không tìm thấy khách hàng nào!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteCustomer(id: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn xóa khách hàng này không? Hành động này không thể hoàn tác.")
            .setPositiveButton("Xóa") { _, _ ->
                runApi(
                    loadingMessage = "Đang xóa khách hàng...",
                    request = { ApiClient.delete("/customers/$id") }
                ) {
                    Toast.makeText(this, "Đã xóa khách hàng thành công", Toast.LENGTH_SHORT).show()
                    loadCustomers(showToast = false)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun updateCustomerStatus(id: String, newStatus: String) {
        val requestBody = org.json.JSONObject().apply {
            put("status", newStatus)
        }
        runApi(
            loadingMessage = "Đang cập nhật...",
            request = { ApiClient.patch("/customers/$id/status", requestBody) }
        ) {
            Toast.makeText(this, "Đã cập nhật trạng thái!", Toast.LENGTH_SHORT).show()
            loadCustomers(showToast = false)
        }
    }
}

