package com.example.project_android.model.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.project_android.model.database.DatabaseContract
import com.example.project_android.model.database.DatabaseHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AccountDao(private val db: SQLiteDatabase) {

    fun findAll(): JSONArray {
        return db.rawQuery(
            """
            SELECT id, fullName, email, role, status, createdAt, updatedAt
            FROM ${DatabaseContract.Accounts.TABLE}
            ORDER BY createdAt DESC
            """.trimIndent(),
            emptyArray<String>()
        ).use { cursor -> rowsToArray(cursor) }
    }

    /**
     * Demo login: app đối chiếu email và mật khẩu với tài khoản admin cấp sẵn trong SQLite.
     * Bản production phải xác thực ở backend và không lưu mật khẩu dạng rõ.
     */
    fun authenticate(email: String, password: String): JSONObject? {
        return db.rawQuery(
            """
            SELECT id, fullName, email, role, status
            FROM ${DatabaseContract.Accounts.TABLE}
            WHERE LOWER(email) = LOWER(?) AND password = ? AND status = 'active'
            LIMIT 1
            """.trimIndent(),
            arrayOf(email.trim(), password)
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                return@use null
            }

            JSONObject().apply {
                put("id", cursor.getString(cursor.getColumnIndexOrThrow("id")))
                put("fullName", cursor.getString(cursor.getColumnIndexOrThrow("fullName")))
                put("email", cursor.getString(cursor.getColumnIndexOrThrow("email")))
                put("role", cursor.getString(cursor.getColumnIndexOrThrow("role")))
            }
        }
    }

    fun create(body: JSONObject): JSONObject {
        val id = UUID.randomUUID().toString()
        val now = DatabaseHelper.now()

        db.insertOrThrow(DatabaseContract.Accounts.TABLE, null, ContentValues().apply {
            put("id", id)
            put("fullName", body.getString("fullName"))
            put("email", body.getString("email").trim().lowercase())
            put("password", body.getString("password"))
            put("role", body.optString("role", "admin"))
            put("status", body.optString("status", "active"))
            put("createdAt", now)
            put("updatedAt", now)
        })

        return findById(db, DatabaseContract.Accounts.TABLE, id)!!.apply {
            remove("password")
        }
    }

    fun count(): Long = scalarLong(db, "SELECT COUNT(*) FROM ${DatabaseContract.Accounts.TABLE}")
}
