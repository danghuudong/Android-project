package com.example.project_android.controller

import android.database.sqlite.SQLiteDatabase
import com.example.project_android.model.dao.OrderDao
import org.json.JSONObject

class OrderController(private val db: SQLiteDatabase) {
    private val orderDao = OrderDao(db)

    fun getOrders(path: String) = orderDao.findAll(path)

    fun createOrder(body: JSONObject) = orderDao.create(body)

    fun updateOrderStatus(code: String, status: String) = orderDao.updateStatus(code, status)

    fun confirmPayment(code: String, body: JSONObject) = orderDao.confirmPayment(code, body)

    fun createReturn(body: JSONObject) = orderDao.createReturnRecord(body)

    fun updateOrder(code: String, body: JSONObject) = orderDao.update(code, body)
}
