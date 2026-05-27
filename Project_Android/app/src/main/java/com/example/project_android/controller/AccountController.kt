package com.example.project_android.controller

import android.database.sqlite.SQLiteDatabase
import com.example.project_android.model.dao.AccountDao
import org.json.JSONObject

class AccountController(private val db: SQLiteDatabase) {

    private val accountDao = AccountDao(db)

    fun getAccounts() = accountDao.findAll()

    fun createAccount(body: JSONObject) = accountDao.create(body)

    fun login(body: JSONObject): JSONObject? {
        val email = body.optString("identifier").trim()
        val password = body.optString("password")

        if (email.isBlank() || password.isBlank()) {
            return null
        }

        return accountDao.authenticate(email, password)
    }
}
