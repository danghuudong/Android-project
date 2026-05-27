package com.example.project_android.model.database

object DatabaseContract {
    const val DATABASE_NAME = "project_android.db"
    const val DATABASE_VERSION = 21

    object Categories {
        const val TABLE = "categories"
    }

    object Products {
        const val TABLE = "products"
    }

    object Customers {
        const val TABLE = "customers"
    }

    object Accounts {
        const val TABLE = "accounts"
    }

    object Orders {
        const val TABLE = "orders"
    }

    object OrderProducts {
        const val TABLE = "order_products"
    }

    object ReturnRecords {
        const val TABLE = "return_records"
    }

    object DiscountCodes {
        const val TABLE = "discount_codes"
    }

    object DiscountEmailLogs {
        const val TABLE = "discount_email_logs"
    }
}
