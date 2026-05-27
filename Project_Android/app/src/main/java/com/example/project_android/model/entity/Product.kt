package com.example.project_android.model.entity

data class Product(
    val id: String,
    val name: String,
    val category: String,
    val size: String,
    val rentalPrice: Double,
    val deposit: Double,
    val quantity: Int,
    val status: String,
    val imageUrl: String = "",
    val description: String = "",
    val createdAt: String,
    val updatedAt: String
)
