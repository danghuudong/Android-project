package com.example.project_android.view.activity

import com.example.project_android.R

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.example.project_android.navigation.AdminAvatarController
import com.example.project_android.network.ApiClient
import com.example.project_android.network.runApi
import org.json.JSONObject
import android.view.View
import android.app.AlertDialog
import java.io.File
import java.util.UUID

class AddProductActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var uploadMainImage: LinearLayout
    private lateinit var edtName: EditText
    private lateinit var spinnerCategory: LinearLayout
    private lateinit var spinnerSize: LinearLayout
    private lateinit var tvSizeLabel: TextView
    private lateinit var edtPrice: EditText
    private lateinit var edtDeposit: EditText
    private lateinit var edtQuantity: EditText
    private lateinit var btnCancel: Button
    private lateinit var btnSave: Button

    private var categoryNames: List<String> = emptyList()
    private var selectedCategory = ""
    private var selectedSize = ""
    private var mainImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mainImageUri = copyProductImageToAppStorage(uri)
            
            val imgView = uploadMainImage.getChildAt(0) as ImageView
            val tvUpload = uploadMainImage.getChildAt(1) as TextView
            
            imgView.setImageURI(mainImageUri)
            imgView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            imgView.scaleType = ImageView.ScaleType.CENTER_CROP
            imgView.imageTintList = null
            imgView.clearColorFilter()
            imgView.setPadding(0, 0, 0, 0)
            imgView.clipToOutline = true
            
            tvUpload.visibility = View.GONE
            uploadMainImage.setPadding(0, 0, 0, 0)
        }
    }

    private var editProductId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        initViews()
        setupEvents()
        loadCategories()
        
        editProductId = intent.getStringExtra("product_id")
        if (editProductId != null) {
            val topBar = btnBack.parent as LinearLayout
            val tvTitle = topBar.getChildAt(1) as TextView
            tvTitle.text = "Sửa sản phẩm"
            btnSave.text = "Lưu thay đổi"
            loadProductDetail()
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        uploadMainImage = findViewById(R.id.uploadMainImage)
        edtName = findViewById(R.id.edtName)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerSize = findViewById(R.id.spinnerSize)
        tvSizeLabel = findViewById(R.id.tvSizeLabel)
        edtPrice = findViewById(R.id.edtPrice)
        edtDeposit = findViewById(R.id.edtDeposit)
        edtQuantity = findViewById(R.id.edtQuantity)
        btnCancel = findViewById(R.id.btnCancel)
        btnSave = findViewById(R.id.btnSave)
    }

    private fun setupEvents() {
        AdminAvatarController.bind(this)
        btnBack.setOnClickListener { finish() }
        btnCancel.setOnClickListener { finish() }

        uploadMainImage.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        spinnerCategory.setOnClickListener {
            if (categoryNames.isEmpty()) {
                Toast.makeText(this, "Chua co danh muc. Hay tao danh muc truoc.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val categories = categoryNames.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Chọn danh mục")
                .setItems(categories) { _, which ->
                    selectedCategory = categories[which]
                    updateCategoryLabel()
                }
                .show()
        }

        spinnerSize.setOnClickListener {
            val sizes = arrayOf("S", "M", "L", "XL", "XXL")
            AlertDialog.Builder(this)
                .setTitle("Chọn size")
                .setItems(sizes) { _, which ->
                    selectedSize = sizes[which]
                    updateSizeLabel()
                }
                .show()
        }

        btnSave.setOnClickListener { validateAndSave() }
    }

    private fun loadCategories() {
        runApi(
            loadingMessage = null,
            request = { ApiClient.get("/categories") }
        ) { result ->
            val data = JSONObject(result.body).getJSONArray("data")
            val names = mutableListOf<String>()

            for (index in 0 until data.length()) {
                val name = data.getJSONObject(index).optString("name").trim()
                if (name.isNotEmpty()) names.add(name)
            }

            categoryNames = names

            // E2E SYNC: form san pham lay danh muc tu bang categories, khong hard-code rieng.
            if (selectedCategory.isBlank()) {
                selectedCategory = categoryNames.firstOrNull().orEmpty()
            } else if (selectedCategory !in categoryNames) {
                categoryNames = listOf(selectedCategory) + categoryNames
            }

            updateCategoryLabel()
        }
    }

    private fun updateCategoryLabel() {
        val tvCategory = spinnerCategory.getChildAt(0) as TextView
        tvCategory.text = selectedCategory.ifBlank { "Chon danh muc" }
    }

    private fun updateSizeLabel() {
        tvSizeLabel.text = selectedSize.ifBlank { "Chọn size" }
        if (selectedSize.isNotBlank()) {
            tvSizeLabel.setTextColor(getColor(R.color.text_primary))
        } else {
            tvSizeLabel.setTextColor(getColor(R.color.text_secondary))
        }
    }

    private fun validateAndSave() {
        val name = edtName.text.toString().trim()
        val size = selectedSize
        val price = edtPrice.text.toString().trim().toDoubleOrNull()
        val deposit = edtDeposit.text.toString().trim().toDoubleOrNull()
        val quantity = edtQuantity.text.toString().trim().toIntOrNull()

        if (selectedCategory.isBlank()) {
            Toast.makeText(this, "Hay chon danh muc cho san pham", Toast.LENGTH_SHORT).show()
            return
        }

        if (name.isEmpty()) return showFieldError(edtName, "Nhập tên sản phẩm")
        if (size.isEmpty()) {
            Toast.makeText(this, "Hãy chọn size cho sản phẩm", Toast.LENGTH_SHORT).show()
            return
        }
        if (price == null || price <= 0) return showFieldError(edtPrice, "Giá thuê không hợp lệ")
        if (deposit == null || deposit < 0) return showFieldError(edtDeposit, "Tiền cọc không hợp lệ")
        if (quantity == null || quantity < 0) return showFieldError(edtQuantity, "Số lượng không hợp lệ")

        val requestBody = JSONObject().apply {
            put("name", name)
            put("category", selectedCategory)
            put("size", size)
            put("rentalPrice", price)
            put("deposit", deposit)
            put("quantity", quantity)
            if (editProductId == null) {
                put("status", "available")
                put("description", "Created from Android app")
            }
            put("imageUrl", mainImageUri?.toString() ?: "")
        }

        if (editProductId != null) {
            runApi(
                loadingMessage = "Đang cập nhật...",
                request = { ApiClient.put("/products/$editProductId", requestBody) }
            ) {
                Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            runApi(
                loadingMessage = "Đang lưu sản phẩm...",
                request = { ApiClient.post("/products", requestBody) }
            ) { result ->
                val product = JSONObject(result.body).getJSONObject("data")
                val productId = product.optString("id")
                if (intent.getBooleanExtra("return_product_to_order", false)) {
                    getSharedPreferences("create_order_return", MODE_PRIVATE)
                        .edit()
                        .putString("added_product_id", productId)
                        .apply()
                }
                setResult(
                    Activity.RESULT_OK,
                    Intent().putExtra("product_id", productId)
                )
                Toast.makeText(this, "Lưu sản phẩm thành công", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun loadProductDetail() {
        runApi(
            loadingMessage = "Đang tải dữ liệu...",
            request = { ApiClient.get("/products/$editProductId/detail") }
        ) { result ->
            val data = JSONObject(result.body).getJSONObject("data").getJSONObject("product")
            edtName.setText(data.optString("name"))
            selectedCategory = data.optString("category", "").trim()
            if (selectedCategory.isNotBlank() && selectedCategory !in categoryNames) {
                categoryNames = listOf(selectedCategory) + categoryNames
            }
            updateCategoryLabel()
            selectedSize = data.optString("size", "")
            updateSizeLabel()
            edtPrice.setText(data.optDouble("rentalPrice", 0.0).toString())
            edtDeposit.setText(data.optDouble("deposit", 0.0).toString())
            edtQuantity.setText(data.optInt("quantity", 0).toString())
            
            val imageUrl = data.optString("imageUrl", "")
            if (imageUrl.isNotEmpty()) {
                mainImageUri = Uri.parse(imageUrl)
                val imgView = uploadMainImage.getChildAt(0) as ImageView
                val tvUpload = uploadMainImage.getChildAt(1) as TextView
                
                try {
                    imgView.setImageURI(mainImageUri)
                    imgView.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    imgView.scaleType = ImageView.ScaleType.CENTER_CROP
                    imgView.imageTintList = null
                    imgView.clearColorFilter()
                    imgView.setPadding(0, 0, 0, 0)
                    imgView.clipToOutline = true
                    
                    tvUpload.visibility = View.GONE
                    uploadMainImage.setPadding(0, 0, 0, 0)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun copyProductImageToAppStorage(sourceUri: Uri): Uri {
        // PRODUCT IMAGE STORAGE:
        // Anh tu thu vien co the la content:// URI phu thuoc quyen doc cua provider.
        // Copy vao filesDir de Product/Order screen reload tu SQLite luc nao cung doc duoc.
        return try {
            val imageDir = File(filesDir, "product_images").apply { mkdirs() }
            val imageFile = File(imageDir, "${UUID.randomUUID()}.jpg")

            contentResolver.openInputStream(sourceUri)?.use { input ->
                imageFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return sourceUri

            Uri.fromFile(imageFile)
        } catch (e: Exception) {
            e.printStackTrace()
            sourceUri
        }
    }

    private fun showFieldError(field: EditText, message: String) {
        field.error = message
        field.requestFocus()
    }
}

