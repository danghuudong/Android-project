package com.example.project_android.navigation

import android.app.Activity
import android.content.Intent
import com.example.project_android.view.activity.AddCustomerActivity
import com.example.project_android.view.activity.AddDiscountCodeActivity
import com.example.project_android.view.activity.AddProductActivity
import com.example.project_android.view.activity.CategoryManagementActivity
import com.example.project_android.view.activity.CreateOrderActivity
import com.example.project_android.view.activity.CustomerDetailActivity
import com.example.project_android.view.activity.ProductDetailActivity
import com.example.project_android.view.activity.CustomerActivity
import com.example.project_android.view.activity.DashboardActivity
import com.example.project_android.view.activity.DiscountCodeActivity
import com.example.project_android.view.activity.DiscountCodeDetailActivity
import com.example.project_android.view.activity.OrderActivity
import com.example.project_android.view.activity.OrderDetailActivity
import com.example.project_android.view.activity.PaymentDetailActivity
import com.example.project_android.view.activity.ProductActivity
import com.example.project_android.view.activity.ReturnProcessActivity

object AppNavigator {
    fun openDashboard(activity: Activity) = activity.open(DashboardActivity::class.java)
    fun openDiscountCodes(activity: Activity) = activity.open(DiscountCodeActivity::class.java)
    fun openAddDiscountCode(activity: Activity) = activity.open(AddDiscountCodeActivity::class.java)
    fun openEditDiscountCode(activity: Activity, discountId: String) {
        activity.startActivity(Intent(activity, AddDiscountCodeActivity::class.java).apply {
            putExtra(AddDiscountCodeActivity.EXTRA_DISCOUNT_ID, discountId)
        })
    }
    fun openDiscountCodeDetail(activity: Activity, discountId: String) {
        activity.startActivity(Intent(activity, DiscountCodeDetailActivity::class.java).apply {
            putExtra(DiscountCodeDetailActivity.EXTRA_DISCOUNT_ID, discountId)
        })
    }
    fun openCategoryManagement(activity: Activity) = activity.open(CategoryManagementActivity::class.java)
    fun openProducts(activity: Activity) = activity.open(ProductActivity::class.java)
    fun openAddProduct(activity: Activity) = activity.open(AddProductActivity::class.java)
    fun openEditProduct(activity: Activity, productId: String) {
        activity.startActivity(Intent(activity, AddProductActivity::class.java).apply {
            putExtra("product_id", productId)
        })
    }
    fun openProductDetail(activity: Activity, productId: String) {
        activity.startActivity(Intent(activity, ProductDetailActivity::class.java).apply {
            putExtra("product_id", productId)
        })
    }
    fun openOrders(activity: Activity) = activity.open(OrderActivity::class.java)
    fun openOrdersWithStatus(activity: Activity, status: String) {
        activity.startActivity(Intent(activity, OrderActivity::class.java).apply {
            putExtra("order_status", status)
        })
    }
    fun openCreateOrder(activity: Activity) = activity.open(CreateOrderActivity::class.java)
    fun openEditOrder(activity: Activity, orderCode: String) {
        activity.startActivity(Intent(activity, CreateOrderActivity::class.java).apply {
            putExtra("edit_order_code", orderCode)
        })
    }
    fun openCreateOrder(activity: Activity, customerName: String, customerPhone: String) {
        activity.startActivity(Intent(activity, CreateOrderActivity::class.java).apply {
            putExtra("customer_name", customerName)
            putExtra("customer_phone", customerPhone)
        })
    }
    fun openCustomers(activity: Activity) = activity.open(CustomerActivity::class.java)
    fun openAddCustomer(activity: Activity) = activity.open(AddCustomerActivity::class.java)
    fun openCustomerDetail(activity: Activity, customerId: String) {
        activity.startActivity(Intent(activity, CustomerDetailActivity::class.java).apply {
            putExtra("customer_id", customerId)
        })
    }
    fun openEditCustomer(activity: Activity, customerId: String) {
        activity.startActivity(Intent(activity, AddCustomerActivity::class.java).apply {
            putExtra("customer_id", customerId)
        })
    }
    fun openOrders(activity: Activity, customerPhone: String) {
        activity.startActivity(Intent(activity, OrderActivity::class.java).apply {
            putExtra("customer_phone", customerPhone)
        })
    }
    fun openReturn(activity: Activity, orderCode: String) {
        activity.startActivity(Intent(activity, ReturnProcessActivity::class.java).apply {
            putExtra("order_code", orderCode)
        })
    }
    fun openPaymentDetail(activity: Activity, orderCode: String) {
        activity.startActivity(Intent(activity, PaymentDetailActivity::class.java).apply {
            putExtra("order_code", orderCode)
        })
    }
    fun openPaymentDetail(activity: Activity) = activity.open(PaymentDetailActivity::class.java)
    fun openOrderDetail(activity: Activity, orderCode: String) {
        activity.startActivity(Intent(activity, OrderDetailActivity::class.java).apply {
            putExtra("order_code", orderCode)
        })
    }
    fun openOrderDetail(activity: Activity) = activity.open(OrderDetailActivity::class.java)
    fun openReturn(activity: Activity) = activity.open(ReturnProcessActivity::class.java)

    private fun Activity.open(target: Class<*>) {
        startActivity(Intent(this, target))
    }
}
