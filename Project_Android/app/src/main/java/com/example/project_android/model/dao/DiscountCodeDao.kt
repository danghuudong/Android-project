package com.example.project_android.model.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.project_android.model.database.DatabaseContract
import com.example.project_android.model.database.DatabaseHelper
import com.example.project_android.model.entity.DiscountCode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class DiscountCodeDao(private val db: SQLiteDatabase) {

    // User lưu mã -> SQLite giữ dữ liệu -> danh sách đọc lại kể cả sau khi mở lại app.
    fun insert(
        programName: String,
        code: String,
        discountType: String,
        discountValue: Double,
        minimumOrder: Double,
        maximumDiscount: Double,
        usageLimit: Int,
        startDate: String,
        endDate: String
    ): DiscountCode {
        val now = DatabaseHelper.now()
        val id = "discount_${UUID.randomUUID()}"
        val normalizedCode = code.trim().uppercase()

        db.insertOrThrow(DatabaseContract.DiscountCodes.TABLE, null, ContentValues().apply {
            put("id", id)
            put("programName", programName.trim())
            put("code", normalizedCode)
            put("discountType", discountType)
            put("discountValue", discountValue)
            put("minimumOrder", minimumOrder)
            put("maximumDiscount", maximumDiscount)
            put("usageLimit", usageLimit)
            put("usedCount", 0)
            put("totalSaved", 0)
            put("generatedRevenue", 0)
            put("startDate", startDate)
            put("endDate", endDate)
            put("status", "active")
            put("createdAt", now)
            put("updatedAt", now)
        })

        return DiscountCode(
            id = id,
            programName = programName.trim(),
            code = normalizedCode,
            discountType = discountType,
            discountValue = discountValue,
            minimumOrder = minimumOrder,
            maximumDiscount = maximumDiscount,
            requiredTotalSpent = -1.0,
            usageLimit = usageLimit,
            usedCount = 0,
            totalSaved = 0.0,
            generatedRevenue = 0.0,
            startDate = startDate,
            endDate = endDate,
            status = "active",
            createdAt = now,
            updatedAt = now
        )
    }

    fun getAll(): List<DiscountCode> {
        return db.rawQuery(
            """
            SELECT * FROM ${DatabaseContract.DiscountCodes.TABLE}
            WHERE status != 'deleted'
            ORDER BY
                CASE WHEN requiredTotalSpent < 0 THEN 1 ELSE 0 END,
                requiredTotalSpent ASC,
                minimumOrder ASC
            """.trimIndent(),
            null
        ).use { cursor ->
            val result = mutableListOf<DiscountCode>()
            while (cursor.moveToNext()) {
                result.add(readDiscountCode(cursor))
            }
            result
        }
    }

    fun getById(id: String): DiscountCode? {
        return db.query(
            DatabaseContract.DiscountCodes.TABLE,
            null,
            "id = ? AND status != ?",
            arrayOf(id, "deleted"),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            if (cursor.moveToFirst()) readDiscountCode(cursor) else null
        }
    }

    // Thanh toán nhập mã -> kiểm tra điều kiện -> trả số tiền được giảm trên riêng phí thuê.
    // Tổng tiền đã thanh toán -> đọc mốc cấp email lưu trong SQLite -> chọn ưu đãi cao nhất còn hợp lệ.
    fun recommendForTotalSpent(totalSpent: Double): DiscountCode? {
        return db.rawQuery(
            """
            SELECT * FROM ${DatabaseContract.DiscountCodes.TABLE}
            WHERE status = 'active'
                AND requiredTotalSpent >= 0
                AND requiredTotalSpent <= ?
                AND usedCount < usageLimit
            ORDER BY requiredTotalSpent DESC
            """.trimIndent(),
            arrayOf(totalSpent.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val discount = readDiscountCode(cursor)
                if (isAvailableToday(discount.startDate, discount.endDate)) {
                    return@use discount
                }
            }
            null
        }
    }

    // Thanh toan cua khach -> chi chap nhan dung ma uu dai khach dang du dieu kien nhan.
    fun calculateForCustomer(code: String, rentalAmount: Double, totalSpent: Double): DiscountCalculation {
        val customerDiscount = recommendForTotalSpent(totalSpent)
            ?: return DiscountCalculation(
                errorMessage = "Khách hàng hiện chưa đủ điều kiện nhận mã giảm giá"
            )
        val normalizedCode = code.trim().uppercase()
        if (customerDiscount.code != normalizedCode) {
            return DiscountCalculation(
                errorMessage = "Khách hàng chỉ được áp dụng mã ${customerDiscount.code}"
            )
        }
        return calculateForRental(normalizedCode, rentalAmount)
    }

    fun calculateForRental(code: String, rentalAmount: Double): DiscountCalculation {
        val discount = getByCode(code.trim().uppercase())
            ?: return DiscountCalculation(errorMessage = "Mã giảm giá không tồn tại")

        if (discount.status != "active") {
            return DiscountCalculation(errorMessage = "Mã giảm giá không còn hoạt động")
        }
        if (!isAvailableToday(discount.startDate, discount.endDate)) {
            return DiscountCalculation(errorMessage = "Mã giảm giá chưa đến hạn hoặc đã hết hạn")
        }
        if (discount.usageLimit <= 0 || discount.usedCount >= discount.usageLimit) {
            return DiscountCalculation(errorMessage = "Mã giảm giá đã hết lượt sử dụng")
        }
        if (rentalAmount < discount.minimumOrder) {
            return DiscountCalculation(
                errorMessage = "Đơn thuê chưa đạt tối thiểu ${formatNumber(discount.minimumOrder)}đ"
            )
        }

        val originalDiscount = if (discount.discountType == "percent") {
            rentalAmount * discount.discountValue / 100
        } else {
            discount.discountValue
        }
        val limitedDiscount = if (discount.maximumDiscount > 0) {
            minOf(originalDiscount, discount.maximumDiscount)
        } else {
            originalDiscount
        }
        return DiscountCalculation(
            discount = discount,
            amount = minOf(limitedDiscount, rentalAmount).coerceAtLeast(0.0)
        )
    }

    private fun getByCode(code: String): DiscountCode? {
        return db.query(
            DatabaseContract.DiscountCodes.TABLE,
            null,
            "code = ? AND status != ?",
            arrayOf(code, "deleted"),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            if (cursor.moveToFirst()) readDiscountCode(cursor) else null
        }
    }

    fun update(
        id: String,
        programName: String,
        code: String,
        discountType: String,
        discountValue: Double,
        minimumOrder: Double,
        maximumDiscount: Double,
        usageLimit: Int,
        startDate: String,
        endDate: String
    ): Boolean {
        val affectedRows = db.update(
            DatabaseContract.DiscountCodes.TABLE,
            ContentValues().apply {
                put("programName", programName.trim())
                put("code", code.trim().uppercase())
                put("discountType", discountType)
                put("discountValue", discountValue)
                put("minimumOrder", minimumOrder)
                put("maximumDiscount", maximumDiscount)
                put("usageLimit", usageLimit)
                put("startDate", startDate)
                put("endDate", endDate)
                put("updatedAt", DatabaseHelper.now())
            },
            "id = ? AND status != ?",
            arrayOf(id, "deleted")
        )
        return affectedRows > 0
    }

    fun delete(id: String): Boolean {
        val affectedRows = db.update(
            DatabaseContract.DiscountCodes.TABLE,
            ContentValues().apply {
                put("status", "deleted")
                put("updatedAt", DatabaseHelper.now())
            },
            "id = ?",
            arrayOf(id)
        )
        return affectedRows > 0
    }

    private fun readDiscountCode(cursor: android.database.Cursor): DiscountCode {
        return DiscountCode(
            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
            programName = cursor.getString(cursor.getColumnIndexOrThrow("programName")),
            code = cursor.getString(cursor.getColumnIndexOrThrow("code")),
            discountType = cursor.getString(cursor.getColumnIndexOrThrow("discountType")),
            discountValue = cursor.getDouble(cursor.getColumnIndexOrThrow("discountValue")),
            minimumOrder = cursor.getDouble(cursor.getColumnIndexOrThrow("minimumOrder")),
            maximumDiscount = cursor.getDouble(cursor.getColumnIndexOrThrow("maximumDiscount")),
            requiredTotalSpent = cursor.getDouble(cursor.getColumnIndexOrThrow("requiredTotalSpent")),
            usageLimit = cursor.getInt(cursor.getColumnIndexOrThrow("usageLimit")),
            usedCount = cursor.getInt(cursor.getColumnIndexOrThrow("usedCount")),
            totalSaved = cursor.getDouble(cursor.getColumnIndexOrThrow("totalSaved")),
            generatedRevenue = cursor.getDouble(cursor.getColumnIndexOrThrow("generatedRevenue")),
            startDate = cursor.getString(cursor.getColumnIndexOrThrow("startDate")),
            endDate = cursor.getString(cursor.getColumnIndexOrThrow("endDate")),
            status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
            createdAt = cursor.getString(cursor.getColumnIndexOrThrow("createdAt")),
            updatedAt = cursor.getString(cursor.getColumnIndexOrThrow("updatedAt"))
        )
    }

    private fun isAvailableToday(startDate: String, endDate: String): Boolean {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")).apply {
            isLenient = false
        }
        val start = runCatching { formatter.parse(startDate) }.getOrNull() ?: return false
        val end = runCatching { formatter.parse(endDate) }.getOrNull() ?: return false
        val today = formatter.parse(formatter.format(Date())) ?: return false
        return !today.before(start) && !today.after(end)
    }

    private fun formatNumber(value: Double): String {
        return String.format(Locale.US, "%,.0f", value).replace(",", ".")
    }
}

data class DiscountCalculation(
    val discount: DiscountCode? = null,
    val amount: Double = 0.0,
    val errorMessage: String? = null
) {
    val isValid: Boolean get() = discount != null && errorMessage == null
}
