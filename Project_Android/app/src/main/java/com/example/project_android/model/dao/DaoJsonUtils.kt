package com.example.project_android.model.dao

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder

internal fun rowsToArray(cursor: Cursor): JSONArray {
    val rows = JSONArray()
    while (cursor.moveToNext()) {
        rows.put(rowToJson(cursor))
    }
    return rows
}

internal fun rowToJson(cursor: Cursor): JSONObject {
    val row = JSONObject()
    for (index in 0 until cursor.columnCount) {
        val key = cursor.getColumnName(index)
        when (cursor.getType(index)) {
            Cursor.FIELD_TYPE_INTEGER -> row.put(key, cursor.getLong(index))
            Cursor.FIELD_TYPE_FLOAT -> row.put(key, cursor.getDouble(index))
            Cursor.FIELD_TYPE_NULL -> row.put(key, JSONObject.NULL)
            else -> row.put(key, cursor.getString(index))
        }
    }
    row.put("_id", row.optString("id"))
    return row
}

internal fun findById(db: SQLiteDatabase, table: String, id: String): JSONObject? {
    return db.rawQuery("SELECT * FROM $table WHERE id = ?", arrayOf(id)).use { cursor ->
        if (cursor.moveToFirst()) rowToJson(cursor) else null
    }
}

internal fun scalarLong(db: SQLiteDatabase, sql: String): Long {
    return scalarLong(db, sql, emptyArray())
}

internal fun scalarLong(db: SQLiteDatabase, sql: String, args: Array<String>): Long {
    return db.rawQuery(sql, args).use { cursor ->
        cursor.moveToFirst()
        cursor.getLong(0)
    }
}

internal fun scalarDouble(db: SQLiteDatabase, sql: String): Double {
    return scalarDouble(db, sql, emptyArray())
}

internal fun scalarDouble(db: SQLiteDatabase, sql: String, args: Array<String>): Double {
    return db.rawQuery(sql, args).use { cursor ->
        cursor.moveToFirst()
        cursor.getDouble(0)
    }
}

internal fun whereSql(where: List<String>): String {
    return if (where.isEmpty()) "" else "WHERE ${where.joinToString(" AND ")}"
}

internal fun queryParam(path: String, key: String): String? {
    val query = path.substringAfter("?", missingDelimiterValue = "")
    if (query.isBlank()) return null

    return query.split("&")
        .mapNotNull {
            val parts = it.split("=", limit = 2)
            if (parts.size == 2) decodeQueryValue(parts[0]) to decodeQueryValue(parts[1]) else null
        }
        .firstOrNull { it.first == key }
        ?.second
}

private fun decodeQueryValue(value: String): String {
    return URLDecoder.decode(value, Charsets.UTF_8.name())
}
