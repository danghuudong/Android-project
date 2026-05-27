package com.example.project_android.model.entity

data class DiscountCode(
    val id: String,
    val programName: String,
    val code: String,
    val discountType: String,
    val discountValue: Double,
    val minimumOrder: Double,
    val maximumDiscount: Double,
    val requiredTotalSpent: Double,
    val usageLimit: Int,
    val usedCount: Int,
    val totalSaved: Double,
    val generatedRevenue: Double,
    val startDate: String,
    val endDate: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)
