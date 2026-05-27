package com.example.project_android.model.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.project_android.model.database.DatabaseContract
import com.example.project_android.model.database.DatabaseHelper
import com.example.project_android.model.entity.Category
import java.util.UUID

class CategoryDao(private val db: SQLiteDatabase) {

    // E2E READ: CategoryManagementActivity gọi hàm này -> SQLite trả danh mục mới nhất -> UI render card.
    fun getAll(): List<Category> {
        val sql = """
            SELECT c.id, c.name, c.description, c.iconUri, c.createdAt, c.updatedAt,
                   COUNT(p.id) AS productCount
            FROM ${DatabaseContract.Categories.TABLE} c
            LEFT JOIN ${DatabaseContract.Products.TABLE} p ON p.category = c.name
            WHERE c.status != 'deleted'
            GROUP BY c.id, c.name, c.description, c.iconUri, c.createdAt, c.updatedAt
            ORDER BY c.createdAt ASC
        """.trimIndent()

        return db.rawQuery(sql, null).use { cursor ->
            val categories = mutableListOf<Category>()
            while (cursor.moveToNext()) {
                categories.add(
                    Category(
                        id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                        iconUri = cursor.getString(cursor.getColumnIndexOrThrow("iconUri")),
                        productCount = cursor.getInt(cursor.getColumnIndexOrThrow("productCount")),
                        createdAt = cursor.getString(cursor.getColumnIndexOrThrow("createdAt")),
                        updatedAt = cursor.getString(cursor.getColumnIndexOrThrow("updatedAt"))
                    )
                )
            }
            categories
        }
    }

    // E2E CREATE: user nhập form -> validate -> insert SQLite -> màn danh sách đọc lại và hiển thị.
    fun insert(name: String, description: String, iconUri: String): Category {
        val now = DatabaseHelper.now()
        val id = "cat_${UUID.randomUUID()}"

        db.insertOrThrow(DatabaseContract.Categories.TABLE, null, ContentValues().apply {
            put("id", id)
            put("name", name)
            put("description", description)
            put("iconUri", iconUri)
            put("status", "active")
            put("createdAt", now)
            put("updatedAt", now)
        })

        return Category(
            id = id,
            name = name,
            description = description,
            iconUri = iconUri,
            productCount = 0,
            createdAt = now,
            updatedAt = now
        )
    }

    fun update(id: String, name: String, description: String, iconUri: String): Boolean {
        val now = DatabaseHelper.now()
        val oldName = findNameById(id) ?: return false

        db.beginTransaction()
        return try {
            val affected = db.update(DatabaseContract.Categories.TABLE, ContentValues().apply {
                put("name", name)
                put("description", description)
                put("iconUri", iconUri)
                put("updatedAt", now)
            }, "id = ?", arrayOf(id))

            // E2E SYNC: user doi ten danh muc -> products.category doi theo -> product list/filter/detail khong bi lech.
            if (affected > 0 && oldName != name) {
                db.update(DatabaseContract.Products.TABLE, ContentValues().apply {
                    put("category", name)
                    put("updatedAt", now)
                }, "category = ?", arrayOf(oldName))
            }

            db.setTransactionSuccessful()
            affected > 0
        } finally {
            db.endTransaction()
        }
    }

    fun delete(id: String): Boolean {
        val categoryName = findNameById(id) ?: return false
        val productCount = scalarLong(
            db,
            "SELECT COUNT(*) FROM ${DatabaseContract.Products.TABLE} WHERE category = ?",
            arrayOf(categoryName)
        )
        if (productCount > 0) return false

        // Thực hiện xóa mềm (Soft delete)
        val now = DatabaseHelper.now()
        val affected = db.update(DatabaseContract.Categories.TABLE, ContentValues().apply {
            put("status", "deleted")
            put("updatedAt", now)
        }, "id = ?", arrayOf(id))
        return affected > 0
    }

    private fun findNameById(id: String): String? {
        return db.rawQuery(
            "SELECT name FROM ${DatabaseContract.Categories.TABLE} WHERE id = ? AND status != 'deleted'",
            arrayOf(id)
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }
}
