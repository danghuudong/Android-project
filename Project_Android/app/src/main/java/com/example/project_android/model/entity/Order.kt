package com.example.project_android.model.entity

data class Order(
    val id: String,
    val code: String,
    val customerName: String,
    val customerPhone: String,
    val pickupDate: String,
    val returnDate: String,
    val totalAmount: Double,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)
