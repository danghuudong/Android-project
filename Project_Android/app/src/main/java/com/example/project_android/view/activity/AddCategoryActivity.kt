package com.example.project_android.view.activity

import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.project_android.R
import com.example.project_android.model.dao.CategoryDao
import com.example.project_android.model.database.DatabaseHelper
import com.example.project_android.navigation.AdminAvatarController
import com.example.project_android.utils.ImageUtils
import java.io.File
import java.util.UUID

class AddCategoryActivity : AppCompatActivity() {

    private lateinit var edtCategoryName: EditText
    private lateinit var edtCategoryDescription: EditText
    private lateinit var imgCategoryIcon: ImageView

    private lateinit var tvHeaderTitle: android.widget.TextView

    private var selectedIconUri: Uri? = null
    private var editCategoryId: String? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult

        // E2E IMAGE: user chọn ảnh -> app giữ quyền đọc URI -> hiển thị preview trên form.
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        selectedIconUri = copyCategoryImageToAppStorage(uri)
        renderCategoryImage(selectedIconUri.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_category)

        initViews()
        setupEvents()
        
        // Kiểm tra xem có đang ở chế độ Sửa danh mục không
        editCategoryId = intent.getStringExtra("EDIT_CATEGORY_ID")
        if (editCategoryId != null) {
            setupEditMode()
        }
    }

    private fun initViews() {
        edtCategoryName = findViewById(R.id.edtCategoryName)
        edtCategoryDescription = findViewById(R.id.edtCategoryDescription)
        imgCategoryIcon = findViewById(R.id.imgCategoryIcon)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
    }

    private fun setupEditMode() {
        tvHeaderTitle.text = "Sửa danh mục"
        
        edtCategoryName.setText(intent.getStringExtra("EDIT_CATEGORY_NAME") ?: "")
        edtCategoryDescription.setText(intent.getStringExtra("EDIT_CATEGORY_DESC") ?: "")
        
        val iconUriStr = intent.getStringExtra("EDIT_CATEGORY_ICON") ?: ""
        if (iconUriStr.isNotBlank()) {
            try {
                selectedIconUri = Uri.parse(iconUriStr)
                renderCategoryImage(iconUriStr)
            } catch (e: Exception) {
                // Keep default icon
            }
        }
    }

    private fun renderCategoryImage(iconUri: String) {
        imgCategoryIcon.imageTintList = null
        imgCategoryIcon.clearColorFilter()
        imgCategoryIcon.setPadding(0, 0, 0, 0)
        imgCategoryIcon.scaleType = ImageView.ScaleType.CENTER_CROP
        ImageUtils.bindImage(imgCategoryIcon, iconUri, android.R.drawable.ic_menu_camera)
    }

    private fun copyCategoryImageToAppStorage(sourceUri: Uri): Uri {
        return try {
            val imageDir = File(filesDir, "category_images").apply { mkdirs() }
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

    private fun setupEvents() {
        AdminAvatarController.bind(this)
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<android.view.View>(R.id.btnPickCategoryImage).setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        findViewById<LinearLayout>(R.id.btnSaveCategory).setOnClickListener {
            validateAndSave()
        }
    }

    private fun validateAndSave() {
        val name = edtCategoryName.text.toString().trim()
        val description = edtCategoryDescription.text.toString().trim()

        if (name.isEmpty()) {
            edtCategoryName.error = "Vui lòng nhập tên danh mục"
            edtCategoryName.requestFocus()
            return
        }

        try {
            val db = DatabaseHelper(this).writableDatabase
            val dao = CategoryDao(db)
            
            if (editCategoryId != null) {
                // Chế độ Sửa
                val success = dao.update(
                    id = editCategoryId!!,
                    name = name,
                    description = description,
                    iconUri = selectedIconUri?.toString() ?: ""
                )
                if (success) {
                    Toast.makeText(this, "Đã cập nhật danh mục", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Chế độ Thêm mới
                dao.insert(
                    name = name,
                    description = description,
                    iconUri = selectedIconUri?.toString() ?: ""
                )
                Toast.makeText(this, "Đã lưu danh mục", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: SQLiteConstraintException) {
            edtCategoryName.error = "Danh mục này đã tồn tại"
            edtCategoryName.requestFocus()
        } catch (e: Exception) {
            Toast.makeText(this, "Đã xảy ra lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
