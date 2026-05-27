package com.example.project_android.model.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.project_android.model.database.DatabaseContract
import com.example.project_android.model.database.DatabaseHelper
import com.example.project_android.model.entity.DiscountCode
import java.util.UUID

class DiscountEmailLogDao(private val db: SQLiteDatabase) {

    // SMTP trả kết quả -> SQLite lưu lịch sử để admin biết lần gửi thành công hay thất bại.
    fun insert(
        customerId: String,
        recipientEmail: String,
        totalSpent: Double,
        discount: DiscountCode,
        status: String,
        errorMessage: String = ""
    ) {
        db.insertOrThrow(DatabaseContract.DiscountEmailLogs.TABLE, null, ContentValues().apply {
            put("id", UUID.randomUUID().toString())
            put("customerId", customerId)
            put("discountCodeId", discount.id)
            put("discountCode", discount.code)
            put("recipientEmail", recipientEmail)
            put("grantedForTotalSpent", totalSpent)
            put("sentAt", DatabaseHelper.now())
            put("status", status)
            put("errorMessage", errorMessage)
        })
    }
}
