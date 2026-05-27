package com.example.project_android.model.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DatabaseContract.DATABASE_NAME,
    null,
    DatabaseContract.DATABASE_VERSION
) {
    init {
        setWriteAheadLoggingEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys = ON")
        createTables(db)
        seedData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("DROP TABLE IF EXISTS order_items")
        db.execSQL("DROP TABLE IF EXISTS payments")
        db.execSQL("DROP TABLE IF EXISTS return_records")
        db.execSQL("DROP TABLE IF EXISTS order_products")
        db.execSQL("DROP TABLE IF EXISTS orders")
        db.execSQL("DROP TABLE IF EXISTS customers")
        db.execSQL("DROP TABLE IF EXISTS products")
        db.execSQL("DROP TABLE IF EXISTS categories")
        db.execSQL("DROP TABLE IF EXISTS staffs")
        db.execSQL("DROP TABLE IF EXISTS accounts")
        createTables(db)
        seedData(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        ensureDemoCategoryImageUrls(db)
        ensureAdditionalDemoCustomer(db)
        ensureDemoCustomerAvatarUrls(db)
        ensureDemoProductImageUrls(db)
        ensureDemoVietnameseText(db)
        seedCustomerOrders(db, now())
        ensureDemoVietnameseText(db)
        removeDuplicateDemoOrderProducts(db)
        ensureOrderProductSizeColumn(db)
        ensureOrderDiscountColumns(db)
        ensureDashboardDemoData(db)
        ensureDiscountCodesTable(db)
        ensureDiscountEmailLogsTable(db)
        ensurePaymentTestOrders(db)
        ensureDiscountEmailDemoData(db)
        ensureDemoVietnameseText(db)
    }

    private fun createTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS categories (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL UNIQUE,
                description TEXT DEFAULT '',
                iconUri TEXT DEFAULT '',
                status TEXT NOT NULL DEFAULT 'active',
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS products (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                size TEXT NOT NULL,
                rentalPrice REAL NOT NULL,
                deposit REAL NOT NULL,
                quantity INTEGER NOT NULL,
                status TEXT NOT NULL DEFAULT 'available',
                imageUrl TEXT DEFAULT '',
                description TEXT DEFAULT '',
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS customers (
                id TEXT PRIMARY KEY,
                fullName TEXT NOT NULL,
                phone TEXT NOT NULL,
                email TEXT DEFAULT '',
                address TEXT DEFAULT '',
                note TEXT DEFAULT '',
                dressSize TEXT DEFAULT '',
                shoeSize TEXT DEFAULT '',
                avatar TEXT DEFAULT '',
                status TEXT NOT NULL DEFAULT 'active',
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS accounts (
                id TEXT PRIMARY KEY,
                fullName TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                role TEXT NOT NULL DEFAULT 'admin',
                status TEXT NOT NULL DEFAULT 'active',
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS orders (
                id TEXT PRIMARY KEY,
                code TEXT NOT NULL UNIQUE,
                customerName TEXT NOT NULL,
                customerPhone TEXT NOT NULL,
                pickupDate TEXT NOT NULL,
                returnDate TEXT NOT NULL,
                totalAmount REAL NOT NULL,
                discountCodeId TEXT DEFAULT '',
                discountCode TEXT DEFAULT '',
                discountAmount REAL NOT NULL DEFAULT 0,
                paidAmount REAL NOT NULL DEFAULT 0,
                qualifyingSpentAmount REAL NOT NULL DEFAULT 0,
                paidAt TEXT DEFAULT '',
                status TEXT NOT NULL DEFAULT 'renting',
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS return_records (
                id TEXT PRIMARY KEY,
                orderCode TEXT NOT NULL,
                condition TEXT NOT NULL,
                penaltyAmount REAL NOT NULL DEFAULT 0,
                notes TEXT DEFAULT '',
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS order_products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                orderId TEXT NOT NULL,
                productId TEXT NOT NULL,
                size TEXT DEFAULT '',
                FOREIGN KEY (orderId) REFERENCES orders(id) ON DELETE CASCADE,
                FOREIGN KEY (productId) REFERENCES products(id)
            )
            """.trimIndent()
        )

        ensureDiscountCodesTable(db)
        ensureDiscountEmailLogsTable(db)

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_products_status ON products(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_products_category ON products(category)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_categories_name ON categories(name)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_orders_code ON orders(code)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_orders_customer_phone ON orders(customerPhone)")
    }

    private fun ensureDiscountCodesTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS discount_codes (
                id TEXT PRIMARY KEY,
                programName TEXT NOT NULL,
                code TEXT NOT NULL UNIQUE,
                discountType TEXT NOT NULL,
                discountValue REAL NOT NULL,
                minimumOrder REAL NOT NULL DEFAULT 0,
                maximumDiscount REAL NOT NULL DEFAULT 0,
                requiredTotalSpent REAL NOT NULL DEFAULT -1,
                usageLimit INTEGER NOT NULL DEFAULT 0,
                usedCount INTEGER NOT NULL DEFAULT 0,
                totalSaved REAL NOT NULL DEFAULT 0,
                generatedRevenue REAL NOT NULL DEFAULT 0,
                startDate TEXT NOT NULL,
                endDate TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'active',
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL
            )
            """.trimIndent()
        )
        ensureDiscountExtraColumns(db)
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_discount_codes_code ON discount_codes(code)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_discount_codes_status ON discount_codes(status)")
        seedDiscountCodes(db)
        removeLegacyDiscountCodes(db)
    }

    private fun ensureDiscountEmailLogsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS discount_email_logs (
                id TEXT PRIMARY KEY,
                customerId TEXT NOT NULL,
                discountCodeId TEXT NOT NULL,
                discountCode TEXT NOT NULL,
                recipientEmail TEXT NOT NULL,
                grantedForTotalSpent REAL NOT NULL,
                sentAt TEXT NOT NULL,
                status TEXT NOT NULL,
                errorMessage TEXT DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_discount_email_customer ON discount_email_logs(customerId)")
    }

    private fun seedDiscountCodes(db: SQLiteDatabase) {
        val defaults = listOf(
            arrayOf("reward_new30", "Chào mừng khách thuê mới", "NEW30", "fixed", 30000.0, 200000.0, 150, 0, "01/01/2026", "31/12/2026", "active", "2026-05-27T00:00:00.000Z"),
            arrayOf("reward_rent5", "Ưu đãi đơn thuê thường", "RENT5", "percent", 5.0, 500000.0, 250, 0, "01/01/2026", "31/12/2026", "active", "2026-05-27T00:00:00.000Z"),
            arrayOf("reward_thankyou50", "Tri ân khách hàng", "THANKYOU50", "fixed", 50000.0, 300000.0, 100, 0, "01/01/2026", "31/12/2026", "active", "2026-05-27T00:00:00.000Z"),
            arrayOf("reward_loyal100", "Khách hàng thân thiết", "LOYAL100", "fixed", 100000.0, 800000.0, 100, 0, "01/01/2026", "31/12/2026", "active", "2026-05-27T00:00:00.000Z"),
            arrayOf("reward_vip10", "Ưu đãi khách VIP", "VIP10", "percent", 10.0, 1000000.0, 100, 0, "01/01/2026", "31/12/2026", "active", "2026-05-27T00:00:00.000Z"),
            arrayOf("reward_event15", "Ưu đãi khách VVIP sự kiện", "EVENT15", "percent", 15.0, 2000000.0, 80, 0, "01/01/2026", "31/12/2026", "active", "2026-05-27T00:00:00.000Z"),
            arrayOf("seed_spring23", "Mã hết hạn để kiểm tra", "SPRING23", "percent", 15.0, 500000.0, 200, 200, "01/01/2026", "31/03/2026", "active", "2026-01-01T00:00:00.000Z")
        )

        defaults.forEach { discount ->
            db.execSQL(
                """
                INSERT OR IGNORE INTO discount_codes
                    (id, programName, code, discountType, discountValue, minimumOrder, usageLimit, usedCount, startDate, endDate, status, createdAt, updatedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(
                    discount[0], discount[1], discount[2], discount[3], discount[4], discount[5],
                    discount[6], discount[7], discount[8], discount[9], discount[10], discount[11], discount[11]
                )
            )
        }

        // Äá»“ng bá»™ mÃ£ háº¿t háº¡n máº«u trÃªn database Ä‘Ã£ táº¡o tá»« phiÃªn báº£n cÅ©.
        db.execSQL(
            """
            UPDATE discount_codes
            SET programName = 'Mã hết hạn để kiểm tra',
                minimumOrder = 500000,
                maximumDiscount = 150000,
                updatedAt = ?
            WHERE id = 'seed_spring23'
            """.trimIndent(),
            arrayOf(now())
        )
        db.execSQL(
            """
            UPDATE discount_codes
            SET programName = 'Ưu đãi khách VVIP sự kiện',
                updatedAt = ?
            WHERE id = 'reward_event15'
            """.trimIndent(),
            arrayOf(now())
        )

        // CÃ¹ng má»™t má»‘c chi tiÃªu nÃ y Ä‘Æ°á»£c dÃ¹ng Ä‘á»ƒ hiá»ƒn thá»‹ trong danh sÃ¡ch vÃ  cáº¥p mÃ£ táº¡i chi tiáº¿t khÃ¡ch.
        seedRewardThreshold(db, "reward_new30", "Ưu đãi khởi đầu", 0.0)
        seedRewardThreshold(db, "reward_rent5", "Ưu đãi đơn thuê thường", 500_000.0)
        seedRewardThreshold(db, "reward_thankyou50", "Tri ân khách hàng", 1_000_000.0)
        seedRewardThreshold(db, "reward_loyal100", "Khách hàng thân thiết", 3_000_000.0)
        seedRewardThreshold(db, "reward_vip10", "Ưu đãi khách VIP", 5_000_000.0)
        seedRewardThreshold(db, "reward_event15", "Ưu đãi khách VVIP sự kiện", 10_000_000.0)
        seedRewardThreshold(db, "seed_spring23", "Mã hết hạn để kiểm tra", -1.0)

        // Dá»¯ liá»‡u demo cÃ³ analytics riÃªng Ä‘á»ƒ trang chi tiáº¿t thay Ä‘á»•i theo mÃ£ Ä‘Æ°á»£c chá»n.
        seedDiscountAnalytics(db, "reward_event15", 12500000.0, 84200000.0)
        seedDiscountAnalytics(db, "seed_spring23", 23800000.0, 136400000.0)
        seedMaximumDiscount(db, "reward_new30", 30000.0)
        seedMaximumDiscount(db, "reward_rent5", 50000.0)
        seedMaximumDiscount(db, "seed_spring23", 150000.0)
        seedMaximumDiscount(db, "reward_thankyou50", 50000.0)
        seedMaximumDiscount(db, "reward_loyal100", 100000.0)
        seedMaximumDiscount(db, "reward_vip10", 300000.0)
        seedMaximumDiscount(db, "reward_event15", 400000.0)
    }

    private fun removeLegacyDiscountCodes(db: SQLiteDatabase) {
        // XÃ³a má»m cÃ¡c mÃ£ máº«u cÅ© khÃ´ng cÃ²n thuá»™c bá»™ quy táº¯c Æ°u Ä‘Ã£i cá»§a flow Ä‘Æ¡n thuÃª.
        db.execSQL(
            """
            UPDATE discount_codes
            SET status = 'deleted', updatedAt = ?
            WHERE code IN ('ATELIER20', 'SUMMER24')
            """.trimIndent(),
            arrayOf(now())
        )
    }

    private fun ensureDiscountExtraColumns(db: SQLiteDatabase) {
        val columns = db.rawQuery("PRAGMA table_info(discount_codes)", null).use { cursor ->
            val names = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                names.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            }
            names
        }

        if ("totalSaved" !in columns) {
            db.execSQL("ALTER TABLE discount_codes ADD COLUMN totalSaved REAL NOT NULL DEFAULT 0")
        }
        if ("generatedRevenue" !in columns) {
            db.execSQL("ALTER TABLE discount_codes ADD COLUMN generatedRevenue REAL NOT NULL DEFAULT 0")
        }
        if ("maximumDiscount" !in columns) {
            db.execSQL("ALTER TABLE discount_codes ADD COLUMN maximumDiscount REAL NOT NULL DEFAULT 0")
            db.execSQL(
                """
                UPDATE discount_codes
                SET maximumDiscount = CASE
                    WHEN discountType = 'fixed' THEN discountValue
                    ELSE ((CAST((((CASE WHEN minimumOrder > 1000000 THEN minimumOrder ELSE 1000000 END) * discountValue / 100) + 9999) / 10000 AS INTEGER)) * 10000)
                END
                """.trimIndent()
            )
        }
        if ("requiredTotalSpent" !in columns) {
            db.execSQL("ALTER TABLE discount_codes ADD COLUMN requiredTotalSpent REAL NOT NULL DEFAULT -1")
        }
    }

    private fun seedDiscountAnalytics(
        db: SQLiteDatabase,
        discountId: String,
        totalSaved: Double,
        generatedRevenue: Double
    ) {
        db.execSQL(
            """
            UPDATE discount_codes
            SET totalSaved = ?, generatedRevenue = ?
            WHERE id = ? AND totalSaved = 0 AND generatedRevenue = 0
            """.trimIndent(),
            arrayOf(totalSaved, generatedRevenue, discountId)
        )
    }

    private fun seedMaximumDiscount(db: SQLiteDatabase, discountId: String, maximumDiscount: Double) {
        db.execSQL(
            """
            UPDATE discount_codes
            SET maximumDiscount = ?
            WHERE id = ? AND maximumDiscount = 0
            """.trimIndent(),
            arrayOf(maximumDiscount, discountId)
        )
    }

    private fun seedRewardThreshold(
        db: SQLiteDatabase,
        discountId: String,
        programName: String,
        requiredTotalSpent: Double
    ) {
        db.execSQL(
            """
            UPDATE discount_codes
            SET programName = ?, requiredTotalSpent = ?, updatedAt = ?
            WHERE id = ? AND status != 'deleted'
            """.trimIndent(),
            arrayOf(programName, requiredTotalSpent, now(), discountId)
        )
    }

    private fun ensureOrderProductSizeColumn(db: SQLiteDatabase) {
        val hasSizeColumn = db.rawQuery("PRAGMA table_info(order_products)", null).use { cursor ->
            var found = false
            while (cursor.moveToNext()) {
                if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == "size") {
                    found = true
                    break
                }
            }
            found
        }

        if (!hasSizeColumn) {
            db.execSQL("ALTER TABLE order_products ADD COLUMN size TEXT DEFAULT ''")
        }
    }

    private fun ensureOrderDiscountColumns(db: SQLiteDatabase) {
        val columns = db.rawQuery("PRAGMA table_info(orders)", null).use { cursor ->
            val names = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                names.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            }
            names
        }

        if ("discountCodeId" !in columns) {
            db.execSQL("ALTER TABLE orders ADD COLUMN discountCodeId TEXT DEFAULT ''")
        }
        if ("discountCode" !in columns) {
            db.execSQL("ALTER TABLE orders ADD COLUMN discountCode TEXT DEFAULT ''")
        }
        if ("discountAmount" !in columns) {
            db.execSQL("ALTER TABLE orders ADD COLUMN discountAmount REAL NOT NULL DEFAULT 0")
        }
        if ("paidAmount" !in columns) {
            db.execSQL("ALTER TABLE orders ADD COLUMN paidAmount REAL NOT NULL DEFAULT 0")
        }
        if ("paidAt" !in columns) {
            db.execSQL("ALTER TABLE orders ADD COLUMN paidAt TEXT DEFAULT ''")
        }
        if ("qualifyingSpentAmount" !in columns) {
            db.execSQL("ALTER TABLE orders ADD COLUMN qualifyingSpentAmount REAL NOT NULL DEFAULT 0")
            // Don cu da thanh toan: loai phan coc hoan lai de xet tong chi tieu uu dai.
            db.execSQL(
                """
                UPDATE orders
                SET qualifyingSpentAmount = MAX(
                    paidAmount - COALESCE(
                        (
                            SELECT SUM(p.deposit * 0.5)
                            FROM order_products op
                            INNER JOIN products p ON p.id = op.productId
                            WHERE op.orderId = orders.id
                        ),
                        0
                    ),
                    0
                )
                WHERE paidAt != ''
                """.trimIndent()
            )
        }
    }

    private fun ensurePaymentTestOrders(db: SQLiteDatabase) {
        val today = storageDate(0)
        val threeDaysAgo = storageDate(-3)
        val twoDaysAgo = storageDate(-2)
        val twoDaysLater = storageDate(2)

        val testOrders = listOf(
            arrayOf("test_pay_on_time", "ORD-PAY-01", "LÃª VÄƒn Nam", "0903334444", threeDaysAgo, today, 350000.0, "renting", "p5_M"),
            arrayOf("test_pay_early", "ORD-PAY-02", "Pháº¡m Tuyáº¿t Mai", "0905556666", twoDaysAgo, twoDaysLater, 380000.0, "renting", "p8_M")
        )

        testOrders.forEach { order ->
            db.execSQL(
                """
                INSERT OR IGNORE INTO orders (id, code, customerName, customerPhone, pickupDate, returnDate, totalAmount, status, createdAt, updatedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(order[0], order[1], order[2], order[3], order[4], order[5], order[6], order[7], "${order[4]}T00:00:00.000Z", now())
            )
            db.execSQL(
                """
                DELETE FROM order_products
                WHERE orderId = ?
                  AND productId = ?
                  AND id NOT IN (
                      SELECT MIN(id)
                      FROM order_products
                      WHERE orderId = ? AND productId = ?
                  )
                """.trimIndent(),
                arrayOf(order[0], order[8], order[0], order[8])
            )
            db.execSQL(
                """
                INSERT INTO order_products (orderId, productId, size)
                SELECT ?, ?, ?
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM order_products
                    WHERE orderId = ? AND productId = ?
                )
                """.trimIndent(),
                arrayOf(order[0], order[8], "M", order[0], order[8])
            )
        }
    }

    private fun ensureDiscountEmailDemoData(db: SQLiteDatabase) {
        db.execSQL(
            "UPDATE customers SET fullName = ?, email = ?, updatedAt = ? WHERE id = 'c1'",
            arrayOf("Nguyá»…n Thá»‹ Huyá»n", "nguyenthihuyen8x8@gmail.com", now())
        )

        val paidOrders = listOf(
            arrayOf("demo_vip_paid_1", "ORD-VIP-01", 2400000.0, "p1_M"),
            arrayOf("demo_vip_paid_2", "ORD-VIP-02", 3000000.0, "p2_M")
        )
        paidOrders.forEach { order ->
            db.execSQL(
                """
                INSERT OR IGNORE INTO orders
                    (id, code, customerName, customerPhone, pickupDate, returnDate, totalAmount, paidAmount, qualifyingSpentAmount, paidAt, status, createdAt, updatedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(
                    order[0], order[1], "Nguyá»…n Thá»‹ Huyá»n", "0901112222",
                    "2026-04-01", "2026-04-05", order[2], order[2], order[2],
                    "2026-04-05T10:00:00.000Z", "returned",
                    "2026-04-01T10:00:00.000Z", now()
                )
            )
            db.execSQL(
                """
                UPDATE orders
                SET qualifyingSpentAmount = ?
                WHERE id = ?
                """.trimIndent(),
                arrayOf(order[2], order[0])
            )
            db.execSQL(
                """
                INSERT INTO order_products (orderId, productId, size)
                SELECT ?, ?, 'M'
                WHERE NOT EXISTS (
                    SELECT 1 FROM order_products WHERE orderId = ? AND productId = ?
                )
                """.trimIndent(),
                arrayOf(order[0], order[3], order[0], order[3])
            )
        }
    }

    private fun ensureDashboardDemoData(db: SQLiteDatabase) {
        val now = now()
        val demoOrders = listOf(
            arrayOf("dash_2025_12_01", "ORD-DASH-1201", "Nguyễn Thị Thu Hà", "0901112222", "2025-12-03", "2025-12-06", 620000.0, "2025-12-06T10:00:00.000Z", "p1_M", "M"),
            arrayOf("dash_2025_12_02", "ORD-DASH-1202", "Lê Văn Nam", "0903334444", "2025-12-15", "2025-12-18", 580000.0, "2025-12-18T11:00:00.000Z", "p6_M", "M"),

            arrayOf("dash_2026_01_01", "ORD-DASH-0101", "Phạm Tuyết Mai", "0905556666", "2026-01-04", "2026-01-07", 720000.0, "2026-01-07T10:30:00.000Z", "p1_M", "M"),
            arrayOf("dash_2026_01_02", "ORD-DASH-0102", "Đỗ Minh Hoàng", "0907778888", "2026-01-12", "2026-01-16", 650000.0, "2026-01-16T09:20:00.000Z", "p15_M", "M"),
            arrayOf("dash_2026_01_03", "ORD-DASH-0103", "Trần Lan Anh", "0909990000", "2026-01-20", "2026-01-23", 780000.0, "2026-01-23T15:10:00.000Z", "p6_M", "M"),

            arrayOf("dash_2026_02_01", "ORD-DASH-0201", "Nguyễn Thị Thu Hà", "0901112222", "2026-02-03", "2026-02-06", 540000.0, "2026-02-06T10:00:00.000Z", "p1_M", "M"),
            arrayOf("dash_2026_02_02", "ORD-DASH-0202", "Lê Văn Nam", "0903334444", "2026-02-18", "2026-02-21", 960000.0, "2026-02-21T13:40:00.000Z", "p15_M", "M"),

            arrayOf("dash_2026_03_01", "ORD-DASH-0301", "Phạm Tuyết Mai", "0905556666", "2026-03-02", "2026-03-05", 850000.0, "2026-03-05T10:00:00.000Z", "p1_M", "M"),
            arrayOf("dash_2026_03_02", "ORD-DASH-0302", "Đỗ Minh Hoàng", "0907778888", "2026-03-10", "2026-03-14", 760000.0, "2026-03-14T11:00:00.000Z", "p6_M", "M"),
            arrayOf("dash_2026_03_03", "ORD-DASH-0303", "Trần Lan Anh", "0909990000", "2026-03-22", "2026-03-26", 910000.0, "2026-03-26T16:00:00.000Z", "p15_M", "M"),
            arrayOf("dash_2026_03_04", "ORD-DASH-0304", "Nguyễn Thị Thu Hà", "0901112222", "2026-03-28", "2026-03-30", 690000.0, "2026-03-30T09:00:00.000Z", "p8_M", "M"),

            arrayOf("dash_2026_04_01", "ORD-DASH-0401", "Lê Văn Nam", "0903334444", "2026-04-04", "2026-04-07", 720000.0, "2026-04-07T10:00:00.000Z", "p1_M", "M"),
            arrayOf("dash_2026_04_02", "ORD-DASH-0402", "Phạm Tuyết Mai", "0905556666", "2026-04-15", "2026-04-18", 640000.0, "2026-04-18T10:00:00.000Z", "p6_M", "M"),
            arrayOf("dash_2026_04_03", "ORD-DASH-0403", "Đỗ Minh Hoàng", "0907778888", "2026-04-24", "2026-04-27", 1040000.0, "2026-04-27T10:00:00.000Z", "p1_M", "M"),

            arrayOf("dash_2026_05_01", "ORD-DASH-0501", "Trần Lan Anh", "0909990000", "2026-05-02", "2026-05-06", 980000.0, "2026-05-06T10:00:00.000Z", "p1_M", "M"),
            arrayOf("dash_2026_05_02", "ORD-DASH-0502", "Nguyễn Thị Thu Hà", "0901112222", "2026-05-08", "2026-05-12", 860000.0, "2026-05-12T12:00:00.000Z", "p6_M", "M"),
            arrayOf("dash_2026_05_03", "ORD-DASH-0503", "Lê Văn Nam", "0903334444", "2026-05-16", "2026-05-20", 1250000.0, "2026-05-20T14:00:00.000Z", "p15_M", "M"),
            arrayOf("dash_2026_05_04", "ORD-DASH-0504", "Phạm Tuyết Mai", "0905556666", "2026-05-22", "2026-05-25", 1010000.0, "2026-05-25T09:30:00.000Z", "p1_M", "M"),

            arrayOf("dash_2026_06_01", "ORD-DASH-0601", "Đỗ Minh Hoàng", "0907778888", "2026-06-02", "2026-06-05", 790000.0, "2026-06-05T10:00:00.000Z", "p6_M", "M"),
            arrayOf("dash_2026_06_02", "ORD-DASH-0602", "Trần Lan Anh", "0909990000", "2026-06-12", "2026-06-15", 920000.0, "2026-06-15T10:00:00.000Z", "p15_M", "M"),
            arrayOf("dash_2026_06_03", "ORD-DASH-0603", "Nguyễn Thị Thu Hà", "0901112222", "2026-06-20", "2026-06-23", 1090000.0, "2026-06-23T10:00:00.000Z", "p8_M", "M")
        )

        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM order_products WHERE orderId LIKE 'dash_%'")
            db.execSQL("DELETE FROM orders WHERE id LIKE 'dash_%'")

            demoOrders.forEach { order ->
                db.execSQL(
                    """
                    INSERT INTO orders
                        (id, code, customerName, customerPhone, pickupDate, returnDate, totalAmount, paidAmount, qualifyingSpentAmount, paidAt, status, createdAt, updatedAt)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'returned', ?, ?)
                    """.trimIndent(),
                    arrayOf(
                        order[0], order[1], order[2], order[3], order[4], order[5],
                        order[6], order[6], order[6], order[7], "${order[4]}T08:00:00.000Z", now
                    )
                )
                db.execSQL(
                    """
                    INSERT INTO order_products (orderId, productId, size)
                    VALUES (?, ?, ?)
                    """.trimIndent(),
                    arrayOf(order[0], order[8], order[9])
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun storageDate(dayOffset: Int): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, dayOffset)
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(calendar.time)
    }

    private fun seedData(db: SQLiteDatabase) {
        val now = now()
        val categories = listOf(
            arrayOf("cat_dress", "Dress", "Đầm dạ hội, váy cocktail và váy dự tiệc cao cấp"),
            arrayOf("cat_suit", "Suit", "Suit, tuxedo và set vest cho sự kiện trang trọng"),
            arrayOf("cat_skirt", "Skirt", "Chân váy midi, pencil, tulle và các kiểu skirt"),
            arrayOf("cat_coat", "Coat", "Áo khoác trench coat, cape, faux fur và outerwear"),
            arrayOf("cat_blazer", "Blazer", "Blazer công sở, linen, wool và oversize")
        )

        categories.forEach { category ->
            db.execSQL(
                """
                INSERT OR IGNORE INTO categories (id, name, description, iconUri, status, createdAt, updatedAt)
                VALUES (?, ?, ?, '', 'active', ?, ?)
                """.trimIndent(),
                arrayOf(category[0], category[1], category[2], now, now)
            )
        }

        ensureDemoCategoryImageUrls(db)
        val productsData = listOf(
            arrayOf("p1", "VÃ¡y dáº¡ há»™i Silk Midnight", "Dress", "S,M", 450000.0, 1500000.0, 4, "available"),
            arrayOf("p2", "GiÃ y cao gÃ³t Crystal Ivory", "Suit", "M,L", 520000.0, 1800000.0, 3, "available"),
            arrayOf("p3", "Chan vay Midi xep ly Metallic", "Skirt", "S,M,L", 260000.0, 800000.0, 6, "available"),
            arrayOf("p4", "Ão khoÃ¡c Tweed Vintage", "Coat", "M,L", 390000.0, 1200000.0, 2, "cleaning"),
            arrayOf("p5", "Structured Wool Blazer", "Blazer", "M,L,XL", 350000.0, 1000000.0, 7, "renting"),
            arrayOf("p6", "Äáº§m lá»¥a Emerald", "Dress", "S,M", 420000.0, 1400000.0, 5, "available"),
            arrayOf("p7", "Suit xanh Navy Slim Fit", "Suit", "M,L,XL", 480000.0, 1600000.0, 4, "available"),
            arrayOf("p8", "Ao dai cach tan Ivory", "Dress", "S,M,L", 380000.0, 1200000.0, 6, "available"),
            arrayOf("p9", "Chan vay Tulle hong pastel", "Skirt", "S,M", 240000.0, 700000.0, 8, "available"),
            arrayOf("p10", "Blazer Linen trang kem", "Blazer", "M,L", 330000.0, 950000.0, 5, "available"),
            arrayOf("p11", "Dam maxi Floral Garden", "Dress", "M,L", 410000.0, 1300000.0, 3, "available"),
            arrayOf("p12", "Tuxedo den Classic", "Suit", "M,L,XL", 550000.0, 1900000.0, 4, "available"),
            arrayOf("p13", "Ao khoac Cape Wool", "Coat", "S,M,L", 360000.0, 1100000.0, 2, "cleaning"),
            arrayOf("p14", "Jumpsuit Sequin Silver", "Dress", "S,M", 390000.0, 1250000.0, 4, "available"),
            arrayOf("p15", "Set vest nu Charcoal", "Suit", "S,M,L", 460000.0, 1500000.0, 5, "available"),
            arrayOf("p16", "Dam bodycon Velvet Black", "Dress", "S,M", 370000.0, 1150000.0, 4, "renting"),
            arrayOf("p17", "Chan vay Pencil Office", "Skirt", "M,L", 220000.0, 650000.0, 7, "available"),
            arrayOf("p18", "Ao khoac Faux Fur Cream", "Coat", "M,L", 430000.0, 1450000.0, 2, "available"),
            arrayOf("p19", "Blazer Oversize Grey", "Blazer", "S,M,L", 340000.0, 980000.0, 6, "available"),
            arrayOf("p20", "Dam da tiec Champagne", "Dress", "S,M,L", 470000.0, 1550000.0, 3, "available")
        )

        productsData.forEach { product ->
            val idBase = product[0] as String
            val name = product[1] as String
            val category = product[2] as String
            val sizes = (product[3] as String).split(",")
            val rentalPrice = product[4] as Double
            val deposit = product[5] as Double
            val quantity = product[6] as Int
            val status = product[7] as String

            sizes.forEach { size ->
                val id = "${idBase}_$size"
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO products (id, name, category, size, rentalPrice, deposit, quantity, status, imageUrl, description, createdAt, updatedAt)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, '', '', ?, ?)
                    """.trimIndent(),
                    arrayOf(id, name, category, size.trim(), rentalPrice, deposit, quantity, status, now, now)
                )
            }
        }

        ensureDemoProductImageUrls(db)

        db.execSQL(
            """
            INSERT OR IGNORE INTO customers (id, fullName, phone, email, address, note, avatar, createdAt, updatedAt)
            VALUES ('c1', 'Nguyá»…n Thá»‹ Thu HÃ ', '0901112222', 'thuha.n@vidu.vn', 'Ho Chi Minh City', '', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&h=400&fit=crop&crop=face', ?, ?)
            """.trimIndent(),
            arrayOf(now, now)
        )
        db.execSQL(
            """
            INSERT OR IGNORE INTO customers (id, fullName, phone, email, address, note, avatar, createdAt, updatedAt)
            VALUES ('c2', 'LÃª VÄƒn Nam', '0903334444', 'nam.le@vidu.vn', 'Ho Chi Minh City', '', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&h=400&fit=crop&crop=face', ?, ?)
            """.trimIndent(),
            arrayOf(now, now)
        )
        db.execSQL(
            """
            INSERT OR IGNORE INTO customers (id, fullName, phone, email, address, note, avatar, createdAt, updatedAt)
            VALUES ('c3', 'Pháº¡m Tuyáº¿t Mai', '0905556666', 'tuyetmai.p@vidu.vn', 'Ho Chi Minh City', '', 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=400&h=400&fit=crop&crop=face', ?, ?)
            """.trimIndent(),
            arrayOf(now, now)
        )
        db.execSQL(
            """
            INSERT OR IGNORE INTO customers (id, fullName, phone, email, address, note, avatar, createdAt, updatedAt)
            VALUES ('c4', 'Äá»— Minh HoÃ ng', '0907778888', 'minhhoang.d@vidu.vn', 'Ho Chi Minh City', '', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&h=400&fit=crop&crop=face', ?, ?)
            """.trimIndent(),
            arrayOf(now, now)
        )
        ensureAdditionalDemoCustomer(db)
        db.execSQL(
            """
            INSERT OR IGNORE INTO accounts (id, fullName, email, password, role, status, createdAt, updatedAt)
            VALUES ('admin_001', 'Admin', 'admin123@gmail.com', '123456', 'admin', 'active', ?, ?)
            """.trimIndent(),
            arrayOf(now, now)
        )
        // Cáº­p nháº­t láº¡i dá»¯ liá»‡u cho nhá»¯ng Database Ä‘Ã£ Ä‘Æ°á»£c táº¡o trÆ°á»›c Ä‘Ã³
        db.execSQL("UPDATE customers SET fullName = 'Nguyá»…n Thá»‹ Thu HÃ ', email = 'thuha.n@vidu.vn', phone = '0901112222' WHERE id = 'c1'")
        db.execSQL("UPDATE customers SET fullName = 'LÃª VÄƒn Nam', email = 'nam.le@vidu.vn', phone = '0903334444' WHERE id = 'c2'")
        db.execSQL("UPDATE customers SET fullName = 'Pháº¡m Tuyáº¿t Mai', email = 'tuyetmai.p@vidu.vn', phone = '0905556666' WHERE id = 'c3'")
        db.execSQL("UPDATE customers SET fullName = 'Äá»— Minh HoÃ ng', email = 'minhhoang.d@vidu.vn', phone = '0907778888' WHERE id = 'c4'")
        
        db.execSQL("UPDATE orders SET customerName = 'Nguyá»…n Thá»‹ Thu HÃ ', customerPhone = '0901112222' WHERE customerPhone = '0901234567' OR customerName = 'Eleanor Vance'")
        db.execSQL("UPDATE orders SET customerName = 'LÃª VÄƒn Nam', customerPhone = '0903334444' WHERE customerPhone = '0912345678' OR customerName = 'Marcus Dupont'")
        db.execSQL("UPDATE orders SET customerName = 'Pháº¡m Tuyáº¿t Mai', customerPhone = '0905556666' WHERE customerPhone = '0987654321' OR customerName = 'Sophia Chen'")
        db.execSQL("UPDATE orders SET customerName = 'Äá»— Minh HoÃ ng', customerPhone = '0907778888' WHERE customerPhone = '0934567890' OR customerName = 'Julian Hayes'")

        seedCustomerOrders(db, now)
    }

    /**
     * ThÃªm khÃ¡ch demo má»›i theo cÃ¡ch khÃ´ng lÃ m máº¥t há»“ sÆ¡ hoáº·c Ä‘Æ¡n thuÃª mÃ  admin Ä‘Ã£ nháº­p.
     * HÃ m Ä‘Æ°á»£c gá»i cáº£ khi táº¡o database má»›i vÃ  khi má»Ÿ database cÅ© cá»§a báº£n demo.
     */
    private fun ensureAdditionalDemoCustomer(db: SQLiteDatabase) {
        val now = now()
        db.execSQL(
            """
            INSERT OR IGNORE INTO customers
                (id, fullName, phone, email, address, note, dressSize, shoeSize, avatar, status, createdAt, updatedAt)
            VALUES
                ('c5', 'Tráº§n Lan Anh', '0909990000', 'lananh.t@vidu.vn', 'Ho Chi Minh City', 'Sang trá»ng', 'M', '37', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400&h=400&fit=crop&crop=face', 'active', ?, ?)
            """.trimIndent(),
            arrayOf(now, now)
        )
    }

    private fun ensureDemoCustomerAvatarUrls(db: SQLiteDatabase) {
        val now = now()
        val demoAvatars = listOf(
            "c1" to "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&h=400&fit=crop&crop=face",
            "c2" to "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&h=400&fit=crop&crop=face",
            "c3" to "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=400&h=400&fit=crop&crop=face",
            "c4" to "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&h=400&fit=crop&crop=face",
            "c5" to "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400&h=400&fit=crop&crop=face"
        )

        demoAvatars.forEach { (customerId, avatarUrl) ->
            db.execSQL(
                """
                UPDATE customers
                SET avatar = ?, updatedAt = ?
                WHERE id = ?
                  AND (avatar = '' OR avatar LIKE 'content://%')
                """.trimIndent(),
                arrayOf(avatarUrl, now, customerId)
            )
        }
    }

    private fun ensureDemoCategoryImageUrls(db: SQLiteDatabase) {
        val now = now()
        val demoCategoryImages = listOf(
            "cat_dress" to "https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=900&h=900&fit=crop",
            "cat_suit" to "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=900&h=900&fit=crop",
            "cat_skirt" to "https://images.unsplash.com/photo-1485968579580-b6d095142e6e?w=900&h=900&fit=crop",
            "cat_coat" to "https://images.unsplash.com/photo-1548624313-0396c75e4b1a?w=900&h=900&fit=crop",
            "cat_blazer" to "https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=900&h=900&fit=crop"
        )

        demoCategoryImages.forEach { (categoryId, iconUrl) ->
            db.execSQL(
                """
                UPDATE categories
                SET iconUri = ?, updatedAt = ?
                WHERE id = ?
                  AND (
                      iconUri = ''
                      OR iconUri LIKE 'content://%'
                      OR iconUri LIKE 'https://images.unsplash.com/%'
                  )
                """.trimIndent(),
                arrayOf(iconUrl, now, categoryId)
            )
        }
    }

    private fun ensureDemoProductImageUrls(db: SQLiteDatabase) {
        val now = now()
        val demoProductImages = listOf(
            "p1" to "https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=700&h=900&fit=crop",
            "p2" to "https://images.unsplash.com/photo-1543163521-1bf539c55dd2?w=700&h=900&fit=crop",
            "p3" to "https://images.unsplash.com/photo-1485968579580-b6d095142e6e?w=700&h=900&fit=crop",
            "p4" to "https://images.unsplash.com/photo-1548624313-0396c75e4b1a?w=700&h=900&fit=crop",
            "p5" to "https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=700&h=900&fit=crop",
            "p6" to "https://images.unsplash.com/photo-1566174053879-31528523f8ae?w=700&h=900&fit=crop",
            "p7" to "https://images.unsplash.com/photo-1594938298603-c8148c4dae35?w=700&h=900&fit=crop",
            "p8" to "https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=700&h=900&fit=crop",
            "p9" to "https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?w=700&h=900&fit=crop",
            "p10" to "https://images.unsplash.com/photo-1591369822096-ffd140ec948f?w=700&h=900&fit=crop",
            "p11" to "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=700&h=900&fit=crop",
            "p12" to "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=700&h=900&fit=crop",
            "p13" to "https://images.unsplash.com/photo-1520975954732-35dd22299614?w=700&h=900&fit=crop",
            "p14" to "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=700&h=900&fit=crop",
            "p15" to "https://images.unsplash.com/photo-1548454782-15b189d129ab?w=700&h=900&fit=crop",
            "p16" to "https://images.unsplash.com/photo-1539008835657-9e8e9680c956?w=700&h=900&fit=crop",
            "p17" to "https://images.unsplash.com/photo-1551488831-00ddcb6c6bd3?w=700&h=900&fit=crop",
            "p18" to "https://images.unsplash.com/photo-1544022613-e87ca75a784a?w=700&h=900&fit=crop",
            "p19" to "https://images.unsplash.com/photo-1556905055-8f358a7a47b2?w=700&h=900&fit=crop",
            "p20" to "https://images.unsplash.com/photo-1568252542512-9fe8fe9c87bb?w=700&h=900&fit=crop"
        )

        demoProductImages.forEach { (productIdBase, imageUrl) ->
            db.execSQL(
                """
                UPDATE products
                SET imageUrl = ?, updatedAt = ?
                WHERE id LIKE ?
                  AND (
                      imageUrl = ''
                      OR imageUrl LIKE 'content://%'
                      OR imageUrl LIKE 'https://images.unsplash.com/%'
                  )
                """.trimIndent(),
                arrayOf(imageUrl, now, "${productIdBase}_%")
            )
        }
    }

    private fun ensureDemoVietnameseText(db: SQLiteDatabase) {
        val now = now()

        val customers = listOf(
            Triple("c1", "Nguyễn Thị Thu Hà", "thuha.n@vidu.vn"),
            Triple("c2", "Lê Văn Nam", "nam.le@vidu.vn"),
            Triple("c3", "Phạm Tuyết Mai", "tuyetmai.p@vidu.vn"),
            Triple("c4", "Đỗ Minh Hoàng", "minhhoang.d@vidu.vn"),
            Triple("c5", "Trần Lan Anh", "lananh.t@vidu.vn")
        )

        customers.forEach { (id, fullName, email) ->
            db.execSQL(
                """
                UPDATE customers
                SET fullName = ?, email = ?, updatedAt = ?
                WHERE id = ?
                """.trimIndent(),
                arrayOf(fullName, email, now, id)
            )
        }

        val orderCustomers = listOf(
            "0901112222" to "Nguyễn Thị Thu Hà",
            "0903334444" to "Lê Văn Nam",
            "0905556666" to "Phạm Tuyết Mai",
            "0907778888" to "Đỗ Minh Hoàng",
            "0909990000" to "Trần Lan Anh"
        )

        orderCustomers.forEach { (phone, customerName) ->
            db.execSQL(
                """
                UPDATE orders
                SET customerName = ?, updatedAt = ?
                WHERE customerPhone = ?
                """.trimIndent(),
                arrayOf(customerName, now, phone)
            )
        }

        val products = listOf(
            "p1" to "Váy dạ hội Silk Midnight",
            "p2" to "Giày cao gót Crystal Ivory",
            "p3" to "Chân váy Midi xếp ly Metallic",
            "p4" to "Áo khoác Tweed Vintage",
            "p5" to "Structured Wool Blazer",
            "p6" to "Đầm lụa Emerald",
            "p7" to "Suit xanh Navy Slim Fit",
            "p8" to "Áo dài cách tân Ivory",
            "p9" to "Chân váy Tulle hồng pastel",
            "p10" to "Blazer Linen trắng kem",
            "p11" to "Đầm maxi Floral Garden",
            "p12" to "Tuxedo đen Classic",
            "p13" to "Áo khoác Cape Wool",
            "p14" to "Jumpsuit Sequin Silver",
            "p15" to "Set vest nữ Charcoal",
            "p16" to "Đầm bodycon Velvet Black",
            "p17" to "Chân váy Pencil Office",
            "p18" to "Áo khoác Faux Fur Cream",
            "p19" to "Blazer Oversize Grey",
            "p20" to "Đầm dạ tiệc Champagne"
        )

        products.forEach { (idPrefix, productName) ->
            db.execSQL(
                """
                UPDATE products
                SET name = ?, updatedAt = ?
                WHERE id LIKE ?
                """.trimIndent(),
                arrayOf(productName, now, "${idPrefix}_%")
            )
        }

        val categories = listOf(
            "cat_dress" to "Đầm dạ hội, váy cocktail và váy dự tiệc cao cấp",
            "cat_suit" to "Suit, tuxedo và set vest cho sự kiện trang trọng",
            "cat_skirt" to "Chân váy midi, pencil, tulle và các kiểu skirt",
            "cat_coat" to "Áo khoác trench coat, cape, faux fur và outerwear",
            "cat_blazer" to "Blazer công sở, linen, wool và oversize"
        )

        categories.forEach { (id, description) ->
            db.execSQL(
                """
                UPDATE categories
                SET description = ?, updatedAt = ?
                WHERE id = ?
                """.trimIndent(),
                arrayOf(description, now, id)
            )
        }

        if (tableExists(db, "discount_codes")) {
            val discounts = listOf(
                "reward_new30" to "Chào mừng khách thuê mới",
                "reward_rent5" to "Ưu đãi đơn thuê thường",
                "reward_thankyou50" to "Tri ân khách hàng",
                "reward_loyal100" to "Khách hàng thân thiết",
                "reward_vip10" to "Ưu đãi khách VIP",
                "reward_event15" to "Ưu đãi khách VVIP sự kiện",
                "seed_spring23" to "Mã hết hạn để kiểm tra"
            )

            discounts.forEach { (id, programName) ->
                db.execSQL(
                    """
                    UPDATE discount_codes
                    SET programName = ?, updatedAt = ?
                    WHERE id = ?
                    """.trimIndent(),
                    arrayOf(programName, now, id)
                )
            }
        }
    }

    private fun tableExists(db: SQLiteDatabase, tableName: String): Boolean {
        return db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1",
            arrayOf(tableName)
        ).use { cursor -> cursor.moveToFirst() }
    }

    private fun seedCustomerOrders(db: SQLiteDatabase, now: String) {
        val orders = listOf(
            // Nguyá»…n Thá»‹ Thu HÃ  (c1)
            arrayOf("o1", "ORD-1001", "Nguyá»…n Thá»‹ Thu HÃ ", "0901112222", "2024-05-10", "2024-05-15", 450000.0, "renting", "p1_M"),
            arrayOf("o2", "ORD-1002", "Nguyá»…n Thá»‹ Thu HÃ ", "0901112222", "2024-05-05", "2024-05-10", 520000.0, "overdue", "p2_M"),
            arrayOf("o3", "ORD-1003", "Nguyá»…n Thá»‹ Thu HÃ ", "0901112222", "2024-04-02", "2024-04-05", 390000.0, "returned", "p4_M"),
            arrayOf("o4", "ORD-1004", "Nguyá»…n Thá»‹ Thu HÃ ", "0901112222", "2024-03-15", "2024-03-18", 420000.0, "returned", "p6_M"),
            arrayOf("o5", "ORD-1005", "Nguyá»…n Thá»‹ Thu HÃ ", "0901112222", "2024-02-20", "2024-02-24", 340000.0, "overdue_history", "p19_M"),
            
            // LÃª VÄƒn Nam (c2)
            arrayOf("o6", "ORD-1006", "LÃª VÄƒn Nam", "0903334444", "2024-05-12", "2024-05-16", 350000.0, "renting", "p5_M"),
            arrayOf("o7", "ORD-1007", "LÃª VÄƒn Nam", "0903334444", "2024-04-10", "2024-04-14", 480000.0, "returned", "p7_M"),
            arrayOf("o8", "ORD-1008", "LÃª VÄƒn Nam", "0903334444", "2024-03-01", "2024-03-05", 550000.0, "returned", "p12_M"),

            // Pháº¡m Tuyáº¿t Mai (c3)
            arrayOf("o9", "ORD-1009", "Pháº¡m Tuyáº¿t Mai", "0905556666", "2024-05-09", "2024-05-13", 380000.0, "renting", "p8_M"),
            arrayOf("o10", "ORD-1010", "Pháº¡m Tuyáº¿t Mai", "0905556666", "2024-05-01", "2024-05-06", 240000.0, "overdue", "p9_M"),
            arrayOf("o11", "ORD-1011", "Pháº¡m Tuyáº¿t Mai", "0905556666", "2024-04-20", "2024-04-25", 410000.0, "returned", "p11_M"),

            // Äá»— Minh HoÃ ng (c4)
            arrayOf("o12", "ORD-1012", "Äá»— Minh HoÃ ng", "0907778888", "2024-05-11", "2024-05-14", 330000.0, "renting", "p10_M"),            arrayOf("o13", "ORD-1013", "Đỗ Minh Hoàng", "0907778888", "2024-04-15", "2024-04-18", 460000.0, "returned", "p15_M"),
            arrayOf("o14", "ORD-1014", "Trần Lan Anh", "0909990000", "2026-05-20", "2026-05-24", 470000.0, "pending", "p20_M"),
            arrayOf("o15", "ORD-1015", "Lê Văn Nam", "0903334444", "2026-05-21", "2026-05-25", 340000.0, "pending", "p19_M"),
            arrayOf("o16", "ORD-1016", "Phạm Tuyết Mai", "0905556666", "2026-05-18", "2026-05-22", 370000.0, "cancelled", "p16_M"),
            arrayOf("o17", "ORD-1017", "Nguyễn Thị Thu Hà", "0901112222", "2026-05-19", "2026-05-23", 260000.0, "cancelled", "p3_M")
        )

        orders.forEach { order ->
            db.execSQL(
                """
                INSERT OR IGNORE INTO orders (id, code, customerName, customerPhone, pickupDate, returnDate, totalAmount, status, createdAt, updatedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(order[0], order[1], order[2], order[3], order[4], order[5], order[6], order[7], "${order[4]}T00:00:00.000Z", now)
            )
            db.execSQL(
                """
                INSERT INTO order_products (orderId, productId)
                SELECT ?, ?
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM order_products
                    WHERE orderId = ? AND productId = ?
                )
                """.trimIndent(),
                arrayOf(order[0], order[8], order[0], order[8])
            )
        }

        db.execSQL("UPDATE orders SET customerName = 'Trần Lan Anh' WHERE code = 'ORD-1014'")
        db.execSQL("UPDATE orders SET customerName = 'Lê Văn Nam' WHERE code = 'ORD-1015'")
        db.execSQL("UPDATE orders SET customerName = 'Phạm Tuyết Mai' WHERE code = 'ORD-1016'")
        db.execSQL("UPDATE orders SET customerName = 'Nguyễn Thị Thu Hà' WHERE code = 'ORD-1017'")
    }

    private fun removeDuplicateDemoOrderProducts(db: SQLiteDatabase) {
        db.execSQL(
            """
            DELETE FROM order_products
            WHERE orderId IN (
                'o1', 'o2', 'o3', 'o4', 'o5', 'o6', 'o7', 'o8', 'o9',
                'o10', 'o11', 'o12', 'o13', 'o14', 'o15', 'o16', 'o17'
            )
              AND id NOT IN (
                  SELECT MIN(id)
                  FROM order_products
                  WHERE orderId IN (
                      'o1', 'o2', 'o3', 'o4', 'o5', 'o6', 'o7', 'o8', 'o9',
                      'o10', 'o11', 'o12', 'o13', 'o14', 'o15', 'o16', 'o17'
                  )
                  GROUP BY orderId, productId
              )
            """.trimIndent()
        )
    }

    companion object {
        fun now(): String = java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            java.util.Locale.US
        ).format(java.util.Date())
    }
}
