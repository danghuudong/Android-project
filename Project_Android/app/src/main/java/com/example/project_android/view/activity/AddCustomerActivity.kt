package com.example.project_android.view.activity

import com.example.project_android.R

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.project_android.network.ApiClient
import com.example.project_android.network.runApi
import com.example.project_android.utils.CustomerImageUtils
import org.json.JSONObject

class AddCustomerActivity : AppCompatActivity() {

    private var editCustomerId: String? = null

    private lateinit var edtFullName: EditText
    private lateinit var edtPhone: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtAddress: EditText
    private lateinit var edtCustomStyle: EditText
    private lateinit var spinnerDressSize: Spinner
    private lateinit var edtShoeSize: EditText
    private lateinit var imgAvatar: ImageView

    // Style chips
    private lateinit var chipToiGian: TextView
    private lateinit var chipSangTrong: TextView
    private lateinit var chipCoDien: TextView
    private lateinit var chipHienDai: TextView
    private lateinit var chipCaTinh: TextView
    private val allChips = mutableListOf<TextView>()
    private val selectedStyles = mutableSetOf("Sang trọng") // Mặc định "Sang trọng" được chọn

    private var avatarUri: Uri? = null

    // Launcher để chọn ảnh từ thư viện (dùng OpenDocument để có persistable permission)
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // Xin quyền truy cập lâu dài để URI không hết hạn sau khi tắt app
            try {
                contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            avatarUri = uri
            imgAvatar.setImageURI(uri)
            // Xóa tint và padding khi đã có ảnh thật
            imgAvatar.imageTintList = null
            imgAvatar.clearColorFilter()
            imgAvatar.setPadding(0, 0, 0, 0)
            imgAvatar.clipToOutline = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_customer)

        editCustomerId = intent.getStringExtra("customer_id")

        initViews()
        setupSpinners()
        setupEvents()

        if (editCustomerId != null) {
            setupEditMode()
        }
    }

    private fun setupEditMode() {
        findViewById<TextView>(R.id.tvHeaderTitle).text = "Sửa khách hàng"
        findViewById<TextView>(R.id.tvSubmitButton).text = "Lưu thay đổi"

        runApi(
            loadingMessage = "Đang tải dữ liệu...",
            request = { ApiClient.get("/customers/$editCustomerId/detail") }
        ) { result ->
            val customer = JSONObject(result.body).getJSONObject("data").getJSONObject("customer")
            edtFullName.setText(customer.optString("fullName"))
            edtPhone.setText(customer.optString("phone"))
            edtEmail.setText(customer.optString("email"))
            edtAddress.setText(customer.optString("address"))

            val dressSize = customer.optString("dressShirtSize", customer.optString("dressSize"))
            val dressSizes = arrayOf("XS", "S", "M", "L", "XL")
            val index = dressSizes.indexOf(dressSize)
            if (index >= 0) spinnerDressSize.setSelection(index)

            edtShoeSize.setText(customer.optString("shoeSize"))

            val notes = customer.optString("note").split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val customNotes = mutableListOf<String>()

            selectedStyles.clear()
            allChips.forEach { chip ->
                chip.setBackgroundResource(R.drawable.bg_chip_style_inactive)
                chip.setTextColor(getColor(R.color.add_cust_label))
            }

            notes.forEach { note ->
                val chip = allChips.find { it.text.toString().equals(note, ignoreCase = true) }
                if (chip != null) {
                    selectedStyles.add(chip.text.toString())
                    chip.setBackgroundResource(R.drawable.bg_chip_style_active)
                    chip.setTextColor(getColor(R.color.add_cust_on_secondary_fixed))
                } else {
                    customNotes.add(note)
                }
            }
            if (customNotes.isNotEmpty()) {
                edtCustomStyle.setText(customNotes.joinToString(", "))
            }

            val avatarStr = customer.optString("avatar", "")
            avatarUri = avatarStr.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
            if (CustomerImageUtils.bindAvatar(
                    imgAvatar,
                    avatarStr,
                    customer.optString("id", ""),
                    customer.optString("phone", "")
                )
            ) {
                imgAvatar.imageTintList = null
                imgAvatar.clearColorFilter()
                imgAvatar.setPadding(0, 0, 0, 0)
                imgAvatar.clipToOutline = true
            }
        }
    }

    private fun initViews() {
        edtFullName = findViewById(R.id.edtFullName)
        edtPhone = findViewById(R.id.edtPhone)
        edtEmail = findViewById(R.id.edtEmail)
        edtAddress = findViewById(R.id.edtAddress)
        edtCustomStyle = findViewById(R.id.edtCustomStyle)
        spinnerDressSize = findViewById(R.id.spinnerDressSize)
        edtShoeSize = findViewById(R.id.edtShoeSize)
        imgAvatar = findViewById(R.id.imgAvatar)

        chipToiGian = findViewById(R.id.chipToiGian)
        chipSangTrong = findViewById(R.id.chipSangTrong)
        chipCoDien = findViewById(R.id.chipCoDien)
        chipHienDai = findViewById(R.id.chipHienDai)
        chipCaTinh = findViewById(R.id.chipCaTinh)
        allChips.addAll(listOf(chipToiGian, chipSangTrong, chipCoDien, chipHienDai, chipCaTinh))
    }

    private fun setupSpinners() {
        val dressSizes = arrayOf("XS", "S", "M", "L", "XL")
        spinnerDressSize.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, dressSizes)
        spinnerDressSize.setSelection(1) // Mặc định S
    }

    private fun setupEvents() {
        // Nút quay lại
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<android.view.View>(R.id.btnSaveTop).setOnClickListener { validateAndSave() }

        // Upload avatar - mở thư viện ảnh
        findViewById<android.view.View>(R.id.btnUploadAvatar).setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        // Style chips toggle
        setupChipToggle(chipToiGian, "Tối giản")
        setupChipToggle(chipSangTrong, "Sang trọng")
        setupChipToggle(chipCoDien, "Cổ điển")
        setupChipToggle(chipHienDai, "Hiện đại")
        setupChipToggle(chipCaTinh, "Cá tính")

        // Nút tạo hồ sơ
        findViewById<LinearLayout>(R.id.btnCreateCustomer).setOnClickListener { validateAndSave() }
    }

    private fun setupChipToggle(chip: TextView, styleName: String) {
        chip.setOnClickListener {
            if (selectedStyles.contains(styleName)) {
                selectedStyles.remove(styleName)
                chip.setBackgroundResource(R.drawable.bg_chip_style_inactive)
                chip.setTextColor(getColor(R.color.add_cust_label))
            } else {
                selectedStyles.add(styleName)
                chip.setBackgroundResource(R.drawable.bg_chip_style_active)
                chip.setTextColor(getColor(R.color.add_cust_on_secondary_fixed))
            }
        }
    }

    private fun validateAndSave() {
        val fullName = edtFullName.text.toString().trim()
        val phone = edtPhone.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val address = edtAddress.text.toString().trim()

        if (fullName.isEmpty()) return showFieldError(edtFullName, "Vui lòng nhập họ và tên")
        if (phone.isEmpty()) return showFieldError(edtPhone, "Vui lòng nhập số điện thoại")

        val customStyle = edtCustomStyle.text.toString().trim()
        val allStyles = selectedStyles.toMutableList()
        if (customStyle.isNotEmpty()) {
            allStyles.add(customStyle)
        }
        val finalNote = allStyles.joinToString(", ")

        val dressSize = spinnerDressSize.selectedItem?.toString() ?: ""
        val shoeSize = edtShoeSize.text.toString().trim()

        val requestBody = JSONObject().apply {
            put("fullName", fullName)
            put("phone", phone)
            put("email", email)
            put("address", address)
            put("note", finalNote)
            put("dressShirtSize", dressSize)
            put("shoeSize", shoeSize)
            put("avatar", avatarUri?.toString() ?: "")
        }

        if (editCustomerId != null) {
            runApi(
                loadingMessage = "Đang cập nhật hồ sơ...",
                request = { ApiClient.put("/customers/$editCustomerId", requestBody) }
            ) {
                Toast.makeText(this, "Đã cập nhật hồ sơ khách hàng!", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            runApi(
                loadingMessage = "Đang tạo hồ sơ khách hàng...",
                request = { ApiClient.post("/customers", requestBody) }
            ) { result ->
                val customer = JSONObject(result.body).getJSONObject("data")
                val customerId = customer.optString("_id", customer.optString("id", ""))
                if (intent.getBooleanExtra("return_customer_to_order", false) && customerId.isNotBlank()) {
                    getSharedPreferences("create_order_return", MODE_PRIVATE)
                        .edit()
                        .putString("added_customer_id", customerId)
                        .apply()

                    setResult(Activity.RESULT_OK, Intent().putExtra("customer_id", customerId))
                }
                Toast.makeText(this, "Đã tạo hồ sơ khách hàng thành công!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun showFieldError(field: EditText, message: String) {
        field.error = message
        field.requestFocus()
    }

}
