package com.example.project_android.view.activity

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.project_android.R
import com.example.project_android.model.dao.CategoryDao
import com.example.project_android.model.database.DatabaseHelper
import com.example.project_android.model.entity.Category
import com.example.project_android.navigation.SidebarController
import com.example.project_android.utils.ImageUtils
import java.util.Locale

class CategoryManagementActivity : AppCompatActivity() {

    private lateinit var btnAddCategory: LinearLayout
    private lateinit var categoryList: LinearLayout
    private lateinit var edtSearchCategory: EditText

    private var allCategories: List<Category> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_management)

        initViews()
        SidebarController.bindFromActivity(this)
        setupEvents()
    }

    override fun onResume() {
        super.onResume()
        loadCategories()
    }

    private fun initViews() {
        btnAddCategory = findViewById(R.id.btnAddCategory)
        categoryList = findViewById(R.id.categoryList)
        edtSearchCategory = findViewById(R.id.edtSearchCategory)
    }

    private fun setupEvents() {
        btnAddCategory.setOnClickListener {
            startActivity(Intent(this, AddCategoryActivity::class.java))
        }

        // E2E SEARCH: user gõ tên danh mục -> filter danh sách theo tên realtime.
        edtSearchCategory.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                filterAndRender(s?.toString().orEmpty())
            }
        })
    }

    private fun loadCategories() {
        val db = DatabaseHelper(this).readableDatabase
        allCategories = CategoryDao(db).getAll()
        filterAndRender(edtSearchCategory.text?.toString().orEmpty())
    }

    private fun filterAndRender(keyword: String) {
        val normalizedKeyword = keyword.trim().lowercase(Locale.getDefault())
        val filtered = if (normalizedKeyword.isEmpty()) {
            allCategories
        } else {
            allCategories.filter { category ->
                category.name.lowercase(Locale.getDefault()).contains(normalizedKeyword)
            }
        }

        categoryList.removeAllViews()
        filtered.forEach { category ->
            categoryList.addView(createCategoryCard(category))
        }
    }

    private fun createCategoryCard(category: Category): View {
        val card = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(184)
            ).apply {
                bottomMargin = dp(28)
            }
            setBackgroundResource(R.drawable.bg_category_card)
            elevation = dp(1).toFloat()
        }

        card.addView(View(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(4), dp(58), Gravity.START or Gravity.TOP).apply {
                topMargin = dp(34)
            }
            setBackgroundResource(R.drawable.bg_category_accent)
        })

        card.addView(ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(58), dp(58), Gravity.START or Gravity.TOP).apply {
                leftMargin = dp(30)
                topMargin = dp(34)
            }
            setBackgroundResource(R.drawable.bg_category_icon_primary)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            scaleType = ImageView.ScaleType.CENTER_CROP

            ImageUtils.bindImage(this, category.iconUri, android.R.drawable.ic_menu_camera)
            if (category.iconUri.isNotBlank()) {
                imageTintList = null
                clearColorFilter()
                setPadding(0, 0, 0, 0)
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: android.graphics.Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, dp(16).toFloat())
                    }
                }
                clipToOutline = true
            } else {
                setColorFilter(getColor(R.color.brand_primary))
            }
        })

        val moreIcon = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(28), dp(28), Gravity.END or Gravity.TOP).apply {
                topMargin = dp(42)
                rightMargin = dp(42)
            }
            setPadding(dp(3), dp(3), dp(3), dp(3))
            setImageResource(R.drawable.ic_more_vertical)
            
            setOnClickListener { view ->
                val popupMenu = android.widget.PopupMenu(this@CategoryManagementActivity, view)
                popupMenu.menu.add(0, 1, 0, "Sửa danh mục")
                popupMenu.menu.add(0, 2, 0, "Xóa danh mục")
                popupMenu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        1 -> {
                            val intent = Intent(this@CategoryManagementActivity, AddCategoryActivity::class.java).apply {
                                putExtra("EDIT_CATEGORY_ID", category.id)
                                putExtra("EDIT_CATEGORY_NAME", category.name)
                                putExtra("EDIT_CATEGORY_DESC", category.description)
                                putExtra("EDIT_CATEGORY_ICON", category.iconUri)
                            }
                            startActivity(intent)
                        }
                        2 -> deleteCategory(category.id, category.name)
                    }
                    true
                }
                popupMenu.show()
            }
        }
        card.addView(moreIcon)

        val content = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP
            ).apply {
                topMargin = dp(92)
            }
            orientation = LinearLayout.VERTICAL
            setPadding(dp(30), 0, dp(28), 0)
        }

        content.addView(TextView(this).apply {
            text = category.name
            setTextColor(getColor(R.color.text_primary))
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
        })

        content.addView(LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL

            addView(TextView(this@CategoryManagementActivity).apply {
                text = "${category.productCount} SẢN PHẨM"
                setBackgroundResource(R.drawable.bg_category_badge_primary)
                setPadding(dp(9), dp(3), dp(9), dp(3))
                setTextColor(getColor(R.color.brand_primary))
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
            })

            addView(TextView(this@CategoryManagementActivity).apply {
                text = " • ${category.description.ifBlank { "Chưa có mô tả" }}"
                setTextColor(getColor(R.color.text_secondary))
                textSize = 14f
                maxLines = 1
            })
        })

        card.addView(content)
        
        card.setOnClickListener {
            val intent = Intent(this, CategoryDetailActivity::class.java).apply {
                putExtra("CATEGORY_ID", category.id)
                putExtra("CATEGORY_NAME", category.name)
                putExtra("CATEGORY_DESC", category.description)
                putExtra("CATEGORY_ICON", category.iconUri)
                putExtra("CATEGORY_COUNT", category.productCount)
            }
            startActivity(intent)
        }
        
        return card
    }

    private fun deleteCategory(id: String, name: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Xóa danh mục")
            .setMessage("Bạn có chắc chắn muốn xóa danh mục '$name' không? Thao tác này không thể hoàn tác.")
            .setPositiveButton("Xóa") { _, _ ->
                val db = DatabaseHelper(this).writableDatabase
                val success = CategoryDao(db).delete(id)
                if (success) {
                    android.widget.Toast.makeText(this, "Đã xóa danh mục", android.widget.Toast.LENGTH_SHORT).show()
                    loadCategories() // Load lại danh sách sau khi xóa
                } else {
                    android.widget.Toast.makeText(this, "Xóa thất bại", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
