package com.example.project_android.controller

import android.database.sqlite.SQLiteDatabase
import com.example.project_android.model.dao.AccountDao
import com.example.project_android.model.dao.OrderDao
import com.example.project_android.model.dao.ProductDao
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReportController(private val db: SQLiteDatabase) {
    private val productDao = ProductDao(db)
    private val orderDao = OrderDao(db)
    private val accountDao = AccountDao(db)

    fun getDashboard(): JSONObject {
        val currentQuarter = quarterBounds(0)
        val previousQuarter = quarterBounds(-1)
        val revenue = orderDao.revenue(currentQuarter.first, currentQuarter.second)
        val previousRevenue = orderDao.revenue(previousQuarter.first, previousQuarter.second)
        val growthPercent = when {
            previousRevenue > 0 -> ((revenue - previousRevenue) / previousRevenue) * 100
            revenue > 0 -> 100.0
            else -> 0.0
        }

        return JSONObject()
            .put("productCount", productDao.count())
            .put("orderCount", orderDao.count())
            .put("activeOrderCount", orderDao.activeCount())
            .put("rentedProductCount", orderDao.rentedProductCount())
            .put("accountCount", accountDao.count())
            .put("returnedCount", orderDao.returnedCount())
            .put("revenue", revenue)
            .put("growthPercent", growthPercent)
            .put("revenueSeries", decemberToJuneRevenueSeries())
            .put("topProducts", productDao.findTop())
    }

    fun getTopProducts() = productDao.findTop()

    private fun quarterBounds(offset: Int): Pair<String, String> {
        val quarterStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.MONTH, (get(Calendar.MONTH) / 3) * 3)
            add(Calendar.MONTH, offset * 3)
        }
        val quarterEnd = (quarterStart.clone() as Calendar).apply {
            add(Calendar.MONTH, 3)
        }
        return formatDate(quarterStart) to formatDate(quarterEnd)
    }

    private fun decemberToJuneRevenueSeries(): JSONArray {
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val calendar = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, currentYear - 1)
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        
        return JSONArray().apply {
            repeat(7) {
                val periodStart = (calendar.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val periodEnd = (calendar.clone() as Calendar).apply {
                    add(Calendar.MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                
                val amount = orderDao.revenue(formatDate(periodStart), formatDate(periodEnd))
                val label = "Th${calendar.get(Calendar.MONTH) + 1}"
                val isCurrentMonth =
                    calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                
                put(
                    JSONObject()
                        .put("amount", amount)
                        .put("label", label)
                        .put("isCurrentMonth", isCurrentMonth)
                )
                calendar.add(Calendar.MONTH, 1)
            }
        }
    }

    private fun formatDate(calendar: Calendar): String {
        return SimpleDateFormat(DATE_PATTERN, Locale.US).format(calendar.time)
    }

    companion object {
        private const val DATE_PATTERN = "yyyy-MM-dd"
    }
}
