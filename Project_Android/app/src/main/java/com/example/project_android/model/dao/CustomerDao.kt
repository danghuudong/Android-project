package com.example.project_android.model.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.project_android.model.database.DatabaseContract
import com.example.project_android.model.database.DatabaseHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class CustomerDao(private val db: SQLiteDatabase) {
    fun findAll(path: String): JSONArray {
        val search = queryParam(path, "search")?.trim()
        val where: String
        val args: Array<String>
        
        if (search.isNullOrBlank()) {
            where = ""
            args = emptyArray()
        } else {
            where = "WHERE (fullName LIKE ? OR email LIKE ? OR phone LIKE ?)"
            val pattern = "%$search%"
            args = arrayOf(pattern, pattern, pattern)
        }
        
        return db.rawQuery(
            "SELECT * FROM ${DatabaseContract.Customers.TABLE} $where ORDER BY createdAt DESC",
            args
        ).use { cursor -> rowsToArray(cursor) }
    }

    fun create(body: JSONObject): JSONObject {
        val id = UUID.randomUUID().toString()
        val now = DatabaseHelper.now()
        db.insertOrThrow(DatabaseContract.Customers.TABLE, null, ContentValues().apply {
            put("id", id)
            put("fullName", body.getString("fullName"))
            put("phone", body.getString("phone"))
            put("email", body.optString("email", ""))
            put("address", body.optString("address", ""))
            put("note", body.optString("note", ""))
            put("dressSize", body.optString("dressSize", ""))
            put("shoeSize", body.optString("shoeSize", ""))
            put("avatar", body.optString("avatar", ""))
            put("createdAt", now)
            put("updatedAt", now)
        })
        return findById(db, DatabaseContract.Customers.TABLE, id)!!
    }

    fun findById(id: String): JSONObject? {
        return findById(db, DatabaseContract.Customers.TABLE, id)
    }

    fun detail(id: String): JSONObject? {
        val customer = findById(id) ?: return null
        val phone = customer.optString("phone")
        val orderDao = OrderDao(db)

        return JSONObject()
            .put("customer", customer)
            .put("orders", orderDao.findByCustomerPhone(phone))
            .put("summary", orderDao.summaryByCustomerPhone(phone))
    }

    fun delete(id: String): Boolean {
        return db.delete(DatabaseContract.Customers.TABLE, "id = ?", arrayOf(id)) > 0
    }

    fun updateStatus(id: String, status: String): JSONObject? {
        val count = db.update(DatabaseContract.Customers.TABLE, ContentValues().apply {
            put("status", status)
            put("updatedAt", DatabaseHelper.now())
        }, "id = ?", arrayOf(id))
        return if (count > 0) findById(id) else null
    }

    fun update(id: String, body: JSONObject): JSONObject? {
        val count = db.update(DatabaseContract.Customers.TABLE, ContentValues().apply {
            put("fullName", body.getString("fullName"))
            put("phone", body.getString("phone"))
            put("email", body.optString("email", ""))
            put("address", body.optString("address", ""))
            put("note", body.optString("note", ""))
            put("dressSize", body.optString("dressSize", ""))
            put("shoeSize", body.optString("shoeSize", ""))
            put("avatar", body.optString("avatar", ""))
            put("updatedAt", DatabaseHelper.now())
        }, "id = ?", arrayOf(id))
        return if (count > 0) findById(id) else null
    }
}
