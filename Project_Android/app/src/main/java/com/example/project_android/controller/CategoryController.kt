package com.example.project_android.controller

import android.database.sqlite.SQLiteDatabase
import com.example.project_android.model.dao.CategoryDao
import org.json.JSONArray
import org.json.JSONObject

class CategoryController(private val db: SQLiteDatabase) {
    private val categoryDao = CategoryDao(db)

    fun getCategories(): JSONArray {
        val categories = JSONArray()

        categoryDao.getAll().forEach { category ->
            categories.put(JSONObject().apply {
                put("id", category.id)
                put("name", category.name)
                put("description", category.description)
                put("iconUri", category.iconUri)
                put("productCount", category.productCount)
                put("createdAt", category.createdAt)
                put("updatedAt", category.updatedAt)
            })
        }

        return categories
    }
}
