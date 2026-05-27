package com.example.project_android.controller

import android.database.sqlite.SQLiteDatabase
import com.example.project_android.model.dao.ProductDao
import org.json.JSONObject

class ProductController(private val db: SQLiteDatabase) {
    private val productDao = ProductDao(db)

    fun getProducts(path: String) = productDao.findAll(path)

    fun createProduct(body: JSONObject) = productDao.create(body)

    fun getProductDetail(productId: String): JSONObject? {
        val product = productDao.findById(productId) ?: return null
        val history = productDao.findRentalHistory(productId)
        return JSONObject().apply {
            put("product", product)
            put("rentalHistory", history)
        }
    }

    fun deleteProduct(id: String) = productDao.delete(id)
    
    fun updateProduct(id: String, body: JSONObject) = productDao.update(id, body)
}
