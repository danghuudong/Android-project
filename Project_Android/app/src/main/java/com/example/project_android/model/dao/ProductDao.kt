package com.example.project_android.model.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.project_android.model.database.DatabaseContract
import com.example.project_android.model.database.DatabaseHelper
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.UUID

class ProductDao(private val db: SQLiteDatabase) {
    fun findAll(path: String): JSONArray {
        val status = queryParam(path, "status")
        val category = queryParam(path, "category")
        val search = queryParam(path, "search")
        val sort = queryParam(path, "sort")
        val where = mutableListOf<String>()
        val args = mutableListOf<String>()

        if (!status.isNullOrBlank()) {
            where.add("status = ?")
            args.add(status)
        }
        if (!category.isNullOrBlank()) {
            where.add("category = ?")
            args.add(category)
        }
        val orderBy = when (sort) {
            "price_asc" -> "rentalPrice ASC"
            "price_desc" -> "rentalPrice DESC"
            "newest" -> "updatedAt DESC, createdAt DESC, rowid DESC"
            "oldest" -> "updatedAt ASC, createdAt ASC, rowid ASC"
            else -> "createdAt DESC"
        }

        val sql = "SELECT * FROM ${DatabaseContract.Products.TABLE} ${whereSql(where)} ORDER BY $orderBy"
        android.util.Log.d("ProductSearch", "SQL: $sql | args: $args")
        val products = db.rawQuery(sql, args.toTypedArray()).use { cursor -> rowsToArray(cursor) }

        // SQLite LOWER + LIKE khong tu bo dau tieng Viet.
        // Vi du DB luu "Ao Dai" co dau, user go "ao dai" thi can normalize o app.
        return if (search.isNullOrBlank()) products else filterProductsBySearch(products, search)
    }

    fun create(body: JSONObject): JSONObject {
        val now = DatabaseHelper.now()
        val id = UUID.randomUUID().toString()
        db.insertOrThrow(DatabaseContract.Products.TABLE, null, ContentValues().apply {
            put("id", id)
            put("name", body.getString("name"))
            put("category", body.getString("category"))
            put("size", body.getString("size"))
            put("rentalPrice", body.getDouble("rentalPrice"))
            put("deposit", body.getDouble("deposit"))
            put("quantity", body.getInt("quantity"))
            put("status", body.optString("status", "available"))
            put("imageUrl", body.optString("imageUrl", ""))
            put("description", body.optString("description", ""))
            put("createdAt", now)
            put("updatedAt", now)
        })
        return findById(db, DatabaseContract.Products.TABLE, id)!!
    }

    fun update(id: String, body: JSONObject): JSONObject? {
        val now = DatabaseHelper.now()
        val affected = db.update(DatabaseContract.Products.TABLE, ContentValues().apply {
            if (body.has("name")) put("name", body.getString("name"))
            if (body.has("category")) put("category", body.getString("category"))
            if (body.has("size")) put("size", body.getString("size"))
            if (body.has("rentalPrice")) put("rentalPrice", body.getDouble("rentalPrice"))
            if (body.has("deposit")) put("deposit", body.getDouble("deposit"))
            if (body.has("quantity")) put("quantity", body.getInt("quantity"))
            if (body.has("status")) put("status", body.getString("status"))
            if (body.has("imageUrl")) put("imageUrl", body.getString("imageUrl"))
            if (body.has("description")) put("description", body.getString("description"))
            put("updatedAt", now)
        }, "id = ?", arrayOf(id))
        
        return if (affected > 0) findById(id) else null
    }

    fun findById(id: String): JSONObject? = findById(db, DatabaseContract.Products.TABLE, id)

    fun findTop(limit: Int = 3): JSONArray {
        return db.rawQuery(
            """
            SELECT
                p.*,
                COUNT(op.id) AS rentalCount,
                SUM(
                    CASE
                        WHEN (
                            SELECT SUM(orderProduct.rentalPrice)
                            FROM ${DatabaseContract.OrderProducts.TABLE} op2
                            INNER JOIN ${DatabaseContract.Products.TABLE} orderProduct ON orderProduct.id = op2.productId
                            WHERE op2.orderId = o.id
                        ) > 0
                        THEN o.qualifyingSpentAmount * p.rentalPrice / (
                            SELECT SUM(orderProduct.rentalPrice)
                            FROM ${DatabaseContract.OrderProducts.TABLE} op2
                            INNER JOIN ${DatabaseContract.Products.TABLE} orderProduct ON orderProduct.id = op2.productId
                            WHERE op2.orderId = o.id
                        )
                        ELSE 0
                    END
                ) AS generatedRevenue
            FROM ${DatabaseContract.Products.TABLE} p
            INNER JOIN ${DatabaseContract.OrderProducts.TABLE} op ON op.productId = p.id
            INNER JOIN ${DatabaseContract.Orders.TABLE} o ON o.id = op.orderId
            WHERE o.paidAt != ''
            GROUP BY p.id
            ORDER BY rentalCount DESC, generatedRevenue DESC, p.name ASC
            LIMIT ?
            """.trimIndent(),
            arrayOf(limit.toString())
        ).use { cursor -> rowsToArray(cursor) }
    }

    fun count(): Long = scalarLong(db, "SELECT COUNT(*) FROM ${DatabaseContract.Products.TABLE}")

    fun findRentalHistory(productId: String): JSONArray {
        val sql = """
            SELECT o.* FROM ${DatabaseContract.Orders.TABLE} o
            INNER JOIN ${DatabaseContract.OrderProducts.TABLE} op ON o.id = op.orderId
            WHERE op.productId = ?
            ORDER BY o.pickupDate DESC
            LIMIT 10
        """.trimIndent()
        return db.rawQuery(sql, arrayOf(productId)).use { cursor -> rowsToArray(cursor) }
    }

    fun delete(id: String): Boolean {
        db.beginTransaction()
        return try {
            // Xóa các bản ghi liên kết trong order_products trước
            db.delete(DatabaseContract.OrderProducts.TABLE, "productId = ?", arrayOf(id))
            val deleted = db.delete(DatabaseContract.Products.TABLE, "id = ?", arrayOf(id)) > 0
            db.setTransactionSuccessful()
            deleted
        } finally {
            db.endTransaction()
        }
    }

    private fun filterProductsBySearch(products: JSONArray, keyword: String): JSONArray {
        val normalizedKeyword = normalizeSearchText(keyword)
        val filtered = JSONArray()

        for (index in 0 until products.length()) {
            val product = products.getJSONObject(index)
            val normalizedName = normalizeSearchText(product.optString("name"))

            // Keyword ngan nhu "1" rat de match nham id UUID/category/size.
            // Man hinh dang ghi "Tim theo ten san pham", nen keyword ngan chi nen so voi name.
            if (normalizedKeyword.length <= 2) {
                if (normalizedName.contains(normalizedKeyword)) {
                    filtered.put(product)
                }
                continue
            }

            val searchableText = listOf(
                product.optString("name"),
                product.optString("id"),
                product.optString("category"),
                product.optString("size"),
                product.optString("description")
            ).joinToString(" ")

            if (normalizeSearchText(searchableText).contains(normalizedKeyword)) {
                filtered.put(product)
            }
        }

        return filtered
    }

    private fun normalizeSearchText(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("đ", "d")
            .replace("Đ", "D")
            .replace('\u0111', 'd')
            .replace('\u0110', 'D')
            .lowercase()
            .trim()
    }

}
