package com.example.project_android.network

import android.content.Context
import com.example.project_android.controller.AccountController
import com.example.project_android.controller.CategoryController
import com.example.project_android.controller.CustomerController
import com.example.project_android.controller.OrderController
import com.example.project_android.controller.ProductController
import com.example.project_android.controller.ReportController
import com.example.project_android.model.database.DatabaseHelper
import org.json.JSONObject

object ApiClient {
    private lateinit var databaseHelper: DatabaseHelper

    fun init(context: Context) {
        if (!::databaseHelper.isInitialized) {
            databaseHelper = DatabaseHelper(context.applicationContext)
        }
    }

    fun get(path: String): ApiResult {
        return runCatching {
            val db = databaseHelper.readableDatabase
            when {
                Regex("^/customers/([^/]+)/detail$").matches(path) -> {
                    val id = Regex("^/customers/([^/]+)/detail$").find(path)!!.groupValues[1]
                    val detail = CustomerController(db).getCustomerDetail(id)
                        ?: return@runCatching error(404, "Customer not found")
                    ok(detail)
                }
                path.startsWith("/products") -> {
                    val detailMatch = Regex("^/products/([^/]+)/detail$").find(path)
                    if (detailMatch != null) {
                        val id = detailMatch.groupValues[1]
                        val detail = ProductController(db).getProductDetail(id)
                            ?: return@runCatching error(404, "Product not found")
                        ok(detail)
                    } else {
                        ok(ProductController(db).getProducts(path))
                    }
                }
                path.startsWith("/categories") -> ok(CategoryController(db).getCategories())
                path.startsWith("/customers") -> ok(CustomerController(db).getCustomers(path))
                path.startsWith("/accounts") -> ok(AccountController(db).getAccounts())
                path.startsWith("/orders") -> ok(OrderController(db).getOrders(path))
                path == "/reports/dashboard" -> ok(ReportController(db).getDashboard())
                path == "/reports/top-products" -> ok(ReportController(db).getTopProducts())
                else -> error(404, "Local route not found: $path")
            }
        }.getOrElse { error(500, it.message.orEmpty()) }
    }

    fun post(path: String, body: JSONObject): ApiResult {
        return runCatching {
            val db = databaseHelper.writableDatabase
            val paymentMatch = Regex("^/orders/([^/]+)/payment$").find(path)
            if (paymentMatch != null) {
                val code = paymentMatch.groupValues[1]
                val order = OrderController(db).confirmPayment(code, body)
                return@runCatching if (order != null) ok(order) else error(404, "Order not found")
            }

            when (path) {
                "/auth/login" -> {
                    val account = AccountController(db).login(body)

                    if (account == null) {
                        error(401, "Tài khoản hoặc mật khẩu không đúng")
                    } else {
                        ok(
                            JSONObject()
                                .put("token", "demo-account-${account.getString("id")}")
                                .put("user", account)
                        )
                    }
                }
                "/products" -> ok(ProductController(db).createProduct(body), 201)
                "/customers" -> ok(CustomerController(db).createCustomer(body), 201)
                "/accounts" -> ok(AccountController(db).createAccount(body), 201)
                "/orders" -> ok(OrderController(db).createOrder(body), 201)
                "/returns" -> ok(OrderController(db).createReturn(body), 201)
                else -> error(404, "Local route not found: $path")
            }
        }.getOrElse { error(500, it.message.orEmpty()) }
    }

    fun patch(path: String, body: JSONObject): ApiResult {
        return runCatching {
            val db = databaseHelper.writableDatabase
            val matchOrder = Regex("^/orders/([^/]+)/status$").find(path)
            if (matchOrder != null) {
                val code = matchOrder.groupValues[1]
                val status = body.optString("status", "renting")
                val order = OrderController(db).updateOrderStatus(code, status)
                return@runCatching if (order != null) ok(order) else error(404, "Order not found")
            }

            val matchCustomer = Regex("^/customers/([^/]+)/status$").find(path)
            if (matchCustomer != null) {
                val id = matchCustomer.groupValues[1]
                val status = body.optString("status", "active")
                val updated = CustomerController(db).updateCustomerStatus(id, status)
                return@runCatching if (updated != null) ok(updated) else error(404, "Customer not found")
            }

            error(404, "Local route not found: $path")
        }.getOrElse { error(500, it.message.orEmpty()) }
    }

    fun delete(path: String): ApiResult {
        return runCatching {
            val db = databaseHelper.writableDatabase
            val matchCustomer = Regex("^/customers/([^/]+)$").find(path)
            if (matchCustomer != null) {
                val id = matchCustomer.groupValues[1]
                val deleted = CustomerController(db).deleteCustomer(id)
                return@runCatching if (deleted) ok(true) else error(404, "Customer not found")
            }
            
            val matchProduct = Regex("^/products/([^/]+)$").find(path)
            if (matchProduct != null) {
                val id = matchProduct.groupValues[1]
                val deleted = ProductController(db).deleteProduct(id)
                return@runCatching if (deleted) ok(true) else error(404, "Product not found")
            }

            error(404, "Local route not found: $path")
        }.getOrElse { error(500, it.message.orEmpty()) }
    }

    fun put(path: String, body: JSONObject): ApiResult {
        return runCatching {
            val db = databaseHelper.writableDatabase
            val matchCustomer = Regex("^/customers/([^/]+)$").find(path)
            if (matchCustomer != null) {
                val id = matchCustomer.groupValues[1]
                val updated = CustomerController(db).updateCustomer(id, body)
                return@runCatching if (updated != null) ok(updated) else error(404, "Customer not found")
            }

            val matchProduct = Regex("^/products/([^/]+)$").find(path)
            if (matchProduct != null) {
                val id = matchProduct.groupValues[1]
                val updated = ProductController(db).updateProduct(id, body)
                return@runCatching if (updated != null) ok(updated) else error(404, "Product not found")
            }

            // E2E EDIT ORDER: PUT /orders/:code -> cập nhật thông tin đơn thuê
            val matchOrder = Regex("^/orders/([^/]+)$").find(path)
            if (matchOrder != null) {
                val code = matchOrder.groupValues[1]
                val updated = OrderController(db).updateOrder(code, body)
                return@runCatching if (updated != null) ok(updated) else error(404, "Order not found")
            }

            error(404, "Local route not found: $path")
        }.getOrElse { error(500, it.message.orEmpty()) }
    }

    private fun ok(data: Any, statusCode: Int = 200): ApiResult {
        return ApiResult(true, statusCode, JSONObject().put("data", data).toString())
    }

    private fun error(statusCode: Int, message: String): ApiResult {
        return ApiResult(false, statusCode, message)
    }
}

data class ApiResult(
    val isSuccess: Boolean,
    val statusCode: Int,
    val body: String
)
