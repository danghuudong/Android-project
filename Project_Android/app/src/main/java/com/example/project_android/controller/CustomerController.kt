package com.example.project_android.controller

import android.database.sqlite.SQLiteDatabase
import com.example.project_android.model.dao.CustomerDao
import org.json.JSONObject

class CustomerController(private val db: SQLiteDatabase) {
    private val customerDao = CustomerDao(db)

    fun getCustomers(path: String) = customerDao.findAll(path)

    fun getCustomerDetail(id: String) = customerDao.detail(id)

    fun createCustomer(body: JSONObject) = customerDao.create(body)

    fun updateCustomerStatus(id: String, status: String) = customerDao.updateStatus(id, status)

    fun updateCustomer(id: String, body: JSONObject) = customerDao.update(id, body)

    fun deleteCustomer(id: String) = customerDao.delete(id)
}
