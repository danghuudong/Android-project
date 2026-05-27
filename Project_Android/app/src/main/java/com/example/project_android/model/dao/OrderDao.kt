package com.example.project_android.model.dao

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.project_android.model.database.DatabaseContract
import com.example.project_android.model.database.DatabaseHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class OrderDao(private val db: SQLiteDatabase) {
    fun findAll(path: String): JSONArray {
        val status = queryParam(path, "status")
        val customerPhone = queryParam(path, "customerPhone")
        val search = queryParam(path, "search")
        val where = mutableListOf<String>()
        val args = mutableListOf<String>()

        where.add("status != ?")
        args.add("deleted_returned")

        if (!status.isNullOrBlank()) {
            where.add("status = ?")
            args.add(status)
        }

        if (!customerPhone.isNullOrBlank()) {
            where.add("customerPhone = ?")
            args.add(customerPhone)
        }

        if (!search.isNullOrBlank()) {
            where.add(
                """
                (
                    code LIKE ?
                    OR customerName LIKE ?
                    OR customerPhone LIKE ?
                    OR EXISTS (
                        SELECT 1
                        FROM ${DatabaseContract.OrderProducts.TABLE} op
                        INNER JOIN ${DatabaseContract.Products.TABLE} p ON p.id = op.productId
                        WHERE op.orderId = ${DatabaseContract.Orders.TABLE}.id
                        AND (p.id LIKE ? OR p.name LIKE ?)
                    )
                )
                """.trimIndent()
            )
            args.add("%$search%")
            args.add("%$search%")
            args.add("%$search%")
            args.add("%$search%")
            args.add("%$search%")
        }

        val orderBy = if (status.isNullOrBlank()) {
            """
            ORDER BY
                CASE
                    WHEN status IN ('returned', 'overdue_history', 'cancelled') THEN 1
                    ELSE 0
                END ASC,
                createdAt DESC
            """.trimIndent()
        } else {
            "ORDER BY createdAt DESC"
        }

        return db.rawQuery(
            "SELECT * FROM ${DatabaseContract.Orders.TABLE} ${whereSql(where)} $orderBy",
            args.toTypedArray()
        ).use { cursor ->
            val orders = JSONArray()
            while (cursor.moveToNext()) {
                orders.put(orderFromCursor(cursor))
            }
            orders
        }
    }

    fun findByCustomerPhone(phone: String): JSONArray {
        return findAll("/orders?customerPhone=$phone")
    }

    fun summaryByCustomerPhone(phone: String): JSONObject {
        val args = arrayOf(phone)
        val orderCount = scalarLong(
            db,
            "SELECT COUNT(*) FROM ${DatabaseContract.Orders.TABLE} WHERE customerPhone = ? AND status != 'deleted_returned'",
            args
        )
        val activeCount = scalarLong(
            db,
            "SELECT COUNT(*) FROM ${DatabaseContract.Orders.TABLE} WHERE customerPhone = ? AND status IN ('renting', 'overdue')",
            args
        )
        val returnedCount = scalarLong(
            db,
            "SELECT COUNT(*) FROM ${DatabaseContract.Orders.TABLE} WHERE customerPhone = ? AND status = 'returned'",
            args
        )
        val totalSpent = scalarDouble(
            db,
            """
            SELECT COALESCE(
                SUM(
                    o.qualifyingSpentAmount +
                    CASE
                        WHEN o.status IN ('returned', 'deleted_returned') THEN COALESCE(
                            (
                                SELECT rr.penaltyAmount
                                FROM ${DatabaseContract.ReturnRecords.TABLE} rr
                                WHERE rr.orderCode = o.code
                                ORDER BY rr.createdAt DESC
                                LIMIT 1
                            ),
                            0
                        )
                        ELSE 0
                    END
                ),
                0
            )
            FROM ${DatabaseContract.Orders.TABLE} o
            WHERE o.customerPhone = ? AND o.paidAt != ''
            """.trimIndent(),
            args
        )

        return JSONObject()
            .put("orderCount", orderCount)
            .put("activeCount", activeCount)
            .put("returnedCount", returnedCount)
            .put("totalSpent", totalSpent)
    }

    fun findByCode(code: String): JSONObject? {
        return db.rawQuery(
            "SELECT * FROM ${DatabaseContract.Orders.TABLE} WHERE code = ?",
            arrayOf(code)
        ).use { cursor ->
            if (cursor.moveToFirst()) orderFromCursor(cursor) else null
        }
    }

    fun create(body: JSONObject): JSONObject {
        val productIds = body.optJSONArray("productIds") ?: JSONArray()
        val productSizes = body.optJSONArray("productSizes")
        val productDao = ProductDao(db)
        val products = mutableListOf<JSONObject>()

        val requestedCounts = mutableMapOf<String, Int>()
        for (index in 0 until productIds.length()) {
            val pid = productIds.getString(index)
            requestedCounts[pid] = (requestedCounts[pid] ?: 0) + 1
        }

        for ((pid, count) in requestedCounts) {
            val product = productDao.findById(pid)
                ?: throw IllegalArgumentException("One or more products are invalid")
            if (product.optInt("quantity", 0) < count) {
                throw IllegalArgumentException("${product.optString("name", "Product")} không đủ số lượng trong kho")
            }
        }

        for (index in 0 until productIds.length()) {
            products.add(productDao.findById(productIds.getString(index))!!)
        }

        val id = UUID.randomUUID().toString()
        val code = "ORD-${System.currentTimeMillis().toString().takeLast(6)}"
        val now = DatabaseHelper.now()
        val totalAmount = body.optDouble(
            "totalAmount", 
            products.sumOf { it.optDouble("rentalPrice", 0.0) }
        )

        db.beginTransaction()
        try {
            db.insertOrThrow(DatabaseContract.Orders.TABLE, null, ContentValues().apply {
                put("id", id)
                put("code", code)
                put("customerName", body.getString("customerName"))
                put("customerPhone", body.getString("customerPhone"))
                put("pickupDate", body.getString("pickupDate"))
                put("returnDate", body.getString("returnDate"))
                put("totalAmount", totalAmount)
                put("status", body.optString("status", "pending"))
                put("createdAt", now)
                put("updatedAt", now)
            })

            for (index in 0 until productIds.length()) {
                db.insertOrThrow(DatabaseContract.OrderProducts.TABLE, null, ContentValues().apply {
                    put("orderId", id)
                    put("productId", productIds.getString(index))
                    put("size", productSizes?.optString(index, products[index].optString("size", "")) ?: products[index].optString("size", ""))
                })

                db.execSQL(
                    """
                    UPDATE ${DatabaseContract.Products.TABLE}
                    SET quantity = quantity - 1, updatedAt = ?
                    WHERE id = ?
                    """.trimIndent(),
                    arrayOf(now, productIds.getString(index))
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        return findByCode(code)!!
    }

    fun updateStatus(code: String, status: String): JSONObject? {
        val orderBeforeUpdate = findByCode(code)
        val now = DatabaseHelper.now()

        db.beginTransaction()
        try {
            if (status == "cancelled" && orderBeforeUpdate?.optString("status") != "cancelled") {
                restockProductsForOrder(code, now)
            }

            if (status == "pending" && orderBeforeUpdate?.optString("status") == "cancelled") {
                reserveProductsForOrder(code, now)
            }

            db.execSQL(
                "UPDATE ${DatabaseContract.Orders.TABLE} SET status = ?, updatedAt = ? WHERE code = ?",
                arrayOf(status, now, code)
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return findByCode(code)
    }

    // Xác nhận thanh toán -> áp mã một lần -> cập nhật đơn và thống kê mã trong cùng transaction.
    fun confirmPayment(code: String, body: JSONObject): JSONObject? {
        val currentOrder = findByCode(code) ?: return null
        if (currentOrder.optString("paidAt", "").isNotBlank()) {
            return currentOrder
        }

        val rentalAmount = body.optDouble("rentalAmount", 0.0).coerceAtLeast(0.0)
        val totalBeforeDiscount = body.optDouble(
            "totalBeforeDiscount",
            currentOrder.optDouble("totalAmount", 0.0)
        ).coerceAtLeast(0.0)
        val enteredDiscountCode = body.optString("discountCode", "").trim()
        val customerTotalSpent = summaryByCustomerPhone(
            currentOrder.optString("customerPhone", "")
        ).optDouble("totalSpent", 0.0)
        val now = DatabaseHelper.now()

        db.beginTransaction()
        try {
            val calculation = if (enteredDiscountCode.isBlank()) {
                null
            } else {
                DiscountCodeDao(db).calculateForCustomer(
                    enteredDiscountCode,
                    rentalAmount,
                    customerTotalSpent
                ).also {
                    if (!it.isValid) {
                        throw IllegalArgumentException(it.errorMessage ?: "Không thể áp dụng mã giảm giá")
                    }
                }
            }
            val discount = calculation?.discount
            val discountAmount = calculation?.amount ?: 0.0
            val paidAmount = (totalBeforeDiscount - discountAmount).coerceAtLeast(0.0)
            // Chi tieu xet uu dai chi gom phi thue thuc thu, khong gom tien coc hoan lai.
            val qualifyingSpentAmount = (rentalAmount - discountAmount).coerceAtLeast(0.0)

            val paidRows = db.update(
                DatabaseContract.Orders.TABLE,
                ContentValues().apply {
                    put("status", "renting")
                    put("discountCodeId", discount?.id.orEmpty())
                    put("discountCode", discount?.code.orEmpty())
                    put("discountAmount", discountAmount)
                    put("paidAmount", paidAmount)
                    put("qualifyingSpentAmount", qualifyingSpentAmount)
                    put("paidAt", now)
                    put("updatedAt", now)
                },
                "code = ? AND paidAt = ''",
                arrayOf(code)
            )
            if (paidRows == 0) {
                throw IllegalArgumentException("Đơn thuê này đã được thanh toán")
            }

            if (discount != null) {
                val rows = db.compileStatement(
                    """
                    UPDATE ${DatabaseContract.DiscountCodes.TABLE}
                    SET usedCount = usedCount + 1,
                        totalSaved = totalSaved + ?,
                        generatedRevenue = generatedRevenue + ?,
                        updatedAt = ?
                    WHERE id = ?
                      AND status = 'active'
                      AND usedCount < usageLimit
                    """.trimIndent()
                ).apply {
                    bindDouble(1, discountAmount)
                    bindDouble(2, (rentalAmount - discountAmount).coerceAtLeast(0.0))
                    bindString(3, now)
                    bindString(4, discount.id)
                }.executeUpdateDelete()
                if (rows == 0) {
                    throw IllegalArgumentException("Mã giảm giá đã hết lượt sử dụng")
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return findByCode(code)
    }

    private fun reserveProductsForOrder(orderCode: String, now: String) {
        val productIds = db.rawQuery(
            """
            SELECT op.productId
            FROM ${DatabaseContract.OrderProducts.TABLE} op
            INNER JOIN ${DatabaseContract.Orders.TABLE} o ON o.id = op.orderId
            WHERE o.code = ?
            """.trimIndent(),
            arrayOf(orderCode)
        ).use { cursor ->
            val ids = mutableListOf<String>()
            while (cursor.moveToNext()) {
                ids.add(cursor.getString(0))
            }
            ids
        }

        productIds.forEach { productId ->
            db.execSQL(
                """
                UPDATE ${DatabaseContract.Products.TABLE}
                SET quantity = quantity - 1,
                    updatedAt = ?
                WHERE id = ?
                """.trimIndent(),
                arrayOf(now, productId)
            )
        }
    }

    fun createReturnRecord(body: JSONObject): JSONObject {
        val id = UUID.randomUUID().toString()
        val now = DatabaseHelper.now()
        val orderCode = body.getString("orderCode")
        val orderBeforeReturn = findByCode(orderCode)
        val shouldRestock = orderBeforeReturn?.optString("status") != "returned"

        db.beginTransaction()
        try {
            db.insertOrThrow(DatabaseContract.ReturnRecords.TABLE, null, ContentValues().apply {
                put("id", id)
                put("orderCode", orderCode)
                put("condition", body.getString("condition"))
                put("penaltyAmount", body.optDouble("penaltyAmount", 0.0))
                put("notes", body.optString("notes", ""))
                put("createdAt", now)
                put("updatedAt", now)
            })

            if (shouldRestock) {
                restockProductsForOrder(orderCode, now)
            }

            db.execSQL(
                "UPDATE ${DatabaseContract.Orders.TABLE} SET status = 'returned', updatedAt = ? WHERE code = ?",
                arrayOf(now, orderCode)
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return findById(db, DatabaseContract.ReturnRecords.TABLE, id)!!
    }

    private fun restockProductsForOrder(orderCode: String, now: String) {
        val productIds = db.rawQuery(
            """
            SELECT op.productId
            FROM ${DatabaseContract.OrderProducts.TABLE} op
            INNER JOIN ${DatabaseContract.Orders.TABLE} o ON o.id = op.orderId
            WHERE o.code = ?
            """.trimIndent(),
            arrayOf(orderCode)
        ).use { cursor ->
            val ids = mutableListOf<String>()
            while (cursor.moveToNext()) {
                ids.add(cursor.getString(0))
            }
            ids
        }

        productIds.forEach { productId ->
            db.execSQL(
                """
                UPDATE ${DatabaseContract.Products.TABLE}
                SET quantity = quantity + 1,
                    status = 'available',
                    updatedAt = ?
                WHERE id = ?
                """.trimIndent(),
                arrayOf(now, productId)
            )
        }
    }

    // ===== E2E UPDATE ORDER =====
    // User sửa đơn thuê -> cập nhật thông tin khách, ngày thuê/trả, danh sách sản phẩm.
    // Logic: restock sản phẩm cũ -> gán sản phẩm mới -> tính lại totalAmount.
    fun update(code: String, body: JSONObject): JSONObject? {
        val order = findByCode(code) ?: return null
        val orderId = order.getString("id")
        val now = DatabaseHelper.now()

        // Lấy danh sách productId mới từ request body
        val newProductIds = body.optJSONArray("productIds") ?: JSONArray()
        val newProductSizes = body.optJSONArray("productSizes")
        val productDao = ProductDao(db)

        // Validate tất cả sản phẩm mới phải hợp lệ
        val newProducts = mutableListOf<JSONObject>()
        for (index in 0 until newProductIds.length()) {
            val product = productDao.findById(newProductIds.getString(index))
                ?: throw IllegalArgumentException("Sản phẩm không hợp lệ")
            newProducts.add(product)
        }

        db.beginTransaction()
        try {
            // --- Bước 1: Restock sản phẩm cũ (trả lại kho) ---
            val oldProductIds = db.rawQuery(
                "SELECT productId FROM ${DatabaseContract.OrderProducts.TABLE} WHERE orderId = ?",
                arrayOf(orderId)
            ).use { cursor ->
                val ids = mutableListOf<String>()
                while (cursor.moveToNext()) { ids.add(cursor.getString(0)) }
                ids
            }

            oldProductIds.forEach { productId ->
                db.execSQL(
                    """
                    UPDATE ${DatabaseContract.Products.TABLE}
                    SET quantity = quantity + 1, updatedAt = ?
                    WHERE id = ?
                    """.trimIndent(),
                    arrayOf(now, productId)
                )
            }

            // --- Bước 2: Xóa liên kết cũ trong bảng order_products ---
            db.delete(DatabaseContract.OrderProducts.TABLE, "orderId = ?", arrayOf(orderId))

            // --- Bước 3: Kiểm tra kho có đủ cho danh sách sản phẩm mới (sau khi đã restock đồ cũ) ---
            val requestedCounts = mutableMapOf<String, Int>()
            for (index in 0 until newProductIds.length()) {
                val pid = newProductIds.getString(index)
                requestedCounts[pid] = (requestedCounts[pid] ?: 0) + 1
            }

            for ((pid, count) in requestedCounts) {
                val pData = db.rawQuery("SELECT quantity, name FROM ${DatabaseContract.Products.TABLE} WHERE id = ?", arrayOf(pid)).use {
                    if (it.moveToFirst()) Pair(it.getInt(0), it.getString(1)) else null
                } ?: throw IllegalArgumentException("Sản phẩm không hợp lệ")

                if (pData.first < count) {
                    throw IllegalArgumentException("${pData.second} không đủ số lượng trong kho")
                }
            }

            // --- Bước 4: Thêm liên kết mới + trừ kho sản phẩm mới ---
            for (index in 0 until newProductIds.length()) {
                val pid = newProductIds.getString(index)
                db.insertOrThrow(DatabaseContract.OrderProducts.TABLE, null, ContentValues().apply {
                    put("orderId", orderId)
                    put("productId", pid)
                    put("size", newProductSizes?.optString(index, newProducts[index].optString("size", "")) ?: newProducts[index].optString("size", ""))
                })
                db.execSQL(
                    """
                    UPDATE ${DatabaseContract.Products.TABLE}
                    SET quantity = quantity - 1, updatedAt = ?
                    WHERE id = ?
                    """.trimIndent(),
                    arrayOf(now, pid)
                )
            }

            // --- Bước 4: Lấy totalAmount thực tế từ UI truyền xuống (đã tính ngày thuê + cọc) ---
            val totalAmount = body.optDouble(
                "totalAmount",
                newProducts.sumOf { it.optDouble("rentalPrice", 0.0) }
            )

            // --- Bước 5: Cập nhật thông tin đơn thuê (dùng ContentValues cho type-safe) ---
            db.update(
                DatabaseContract.Orders.TABLE,
                ContentValues().apply {
                    put("customerName", body.optString("customerName", order.optString("customerName")))
                    put("customerPhone", body.optString("customerPhone", order.optString("customerPhone")))
                    put("pickupDate", body.optString("pickupDate", order.optString("pickupDate")))
                    put("returnDate", body.optString("returnDate", order.optString("returnDate")))
                    put("totalAmount", totalAmount)
                    put("updatedAt", now)
                },
                "code = ?",
                arrayOf(code)
            )

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        return findByCode(code)
    }

    fun count(): Long = scalarLong(db, "SELECT COUNT(*) FROM ${DatabaseContract.Orders.TABLE} WHERE status != 'deleted_returned'")

    fun activeCount(): Long {
        return scalarLong(
            db,
            "SELECT COUNT(*) FROM ${DatabaseContract.Orders.TABLE} WHERE status IN ('renting', 'overdue')"
        )
    }

    fun rentedProductCount(): Long {
        return scalarLong(
            db,
            """
            SELECT COUNT(*)
            FROM ${DatabaseContract.OrderProducts.TABLE} op
            INNER JOIN ${DatabaseContract.Orders.TABLE} o ON o.id = op.orderId
            WHERE o.status IN ('renting', 'overdue')
            """.trimIndent()
        )
    }

    fun returnedCount(): Long {
        return scalarLong(db, "SELECT COUNT(*) FROM ${DatabaseContract.Orders.TABLE} WHERE status = 'returned'")
    }

    fun revenue(startDate: String? = null, endDate: String? = null): Double {
        if (startDate != null && endDate != null) {
            return scalarDouble(
                db,
                """
                SELECT
                    COALESCE(
                        (
                            SELECT SUM(o.qualifyingSpentAmount)
                            FROM ${DatabaseContract.Orders.TABLE} o
                            WHERE o.paidAt != ''
                              AND o.paidAt >= ?
                              AND o.paidAt < ?
                        ),
                        0
                    )
                    +
                    COALESCE(
                        (
                            SELECT SUM(rr.penaltyAmount)
                            FROM ${DatabaseContract.ReturnRecords.TABLE} rr
                            INNER JOIN ${DatabaseContract.Orders.TABLE} o ON o.code = rr.orderCode
                            WHERE rr.createdAt >= ?
                              AND rr.createdAt < ?
                              AND o.status IN ('returned', 'deleted_returned')
                        ),
                        0
                    )
                """.trimIndent(),
                arrayOf(startDate, endDate, startDate, endDate)
            )
        }

        return scalarDouble(
            db,
            """
            SELECT
                COALESCE(
                    (SELECT SUM(qualifyingSpentAmount) FROM ${DatabaseContract.Orders.TABLE} WHERE paidAt != ''),
                    0
                )
                +
                COALESCE(
                    (
                        SELECT SUM(rr.penaltyAmount)
                        FROM ${DatabaseContract.ReturnRecords.TABLE} rr
                        INNER JOIN ${DatabaseContract.Orders.TABLE} o ON o.code = rr.orderCode
                        WHERE o.status IN ('returned', 'deleted_returned')
                    ),
                    0
                )
            """.trimIndent()
        )
    }

    private fun orderFromCursor(cursor: Cursor): JSONObject {
        val order = rowToJson(cursor)
        val latestReturn = latestReturnRecord(order.optString("code", ""))
        val customer = customerForOrder(order.optString("customerPhone", ""))
        val products = db.rawQuery(
            """
            SELECT
                p.id,
                p.name,
                p.category,
                COALESCE(NULLIF(op.size, ''), p.size) AS size,
                p.rentalPrice,
                p.deposit,
                p.quantity,
                p.status,
                p.imageUrl,
                p.description,
                p.createdAt,
                p.updatedAt
            FROM ${DatabaseContract.Products.TABLE} p
            INNER JOIN ${DatabaseContract.OrderProducts.TABLE} op ON op.productId = p.id
            WHERE op.orderId = ?
            ORDER BY op.id ASC
            """.trimIndent(),
            arrayOf(order.getString("id"))
        ).use { productCursor -> rowsToArray(productCursor) }

        // Backward compatible: productIds dang duoc UI cu dung nhu danh sach product object.
        // products la ten ro nghia hon cho man Don thue khi can lay imageUrl/name/id.
        order.put("productIds", products)
        order.put("products", products)
        if (customer != null) {
            order.put("customerId", customer.optString("id", ""))
            order.put("customerAvatar", customer.optString("avatar", ""))
            order.put("customerEmail", customer.optString("email", ""))
        }
        if (latestReturn != null) {
            val returnedAt = latestReturn.optString("createdAt", "")
            order.put("actualReturnDate", returnedAt.take(10))
            order.put("returnedAt", returnedAt)
            order.put("returnCondition", latestReturn.optString("condition", ""))
            order.put("returnPenaltyAmount", latestReturn.optDouble("penaltyAmount", 0.0))
            order.put("returnNotes", latestReturn.optString("notes", ""))
        }
        return order
    }

    private fun customerForOrder(phone: String): JSONObject? {
        if (phone.isBlank()) return null

        return db.rawQuery(
            """
            SELECT id, avatar, email
            FROM ${DatabaseContract.Customers.TABLE}
            WHERE phone = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(phone)
        ).use { cursor ->
            if (cursor.moveToFirst()) rowToJson(cursor) else null
        }
    }

    private fun latestReturnRecord(orderCode: String): JSONObject? {
        if (orderCode.isBlank()) return null

        return db.rawQuery(
            """
            SELECT *
            FROM ${DatabaseContract.ReturnRecords.TABLE}
            WHERE orderCode = ?
            ORDER BY createdAt DESC
            LIMIT 1
            """.trimIndent(),
            arrayOf(orderCode)
        ).use { cursor ->
            if (cursor.moveToFirst()) rowToJson(cursor) else null
        }
    }
}
