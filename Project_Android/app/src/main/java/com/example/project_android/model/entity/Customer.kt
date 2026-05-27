package com.example.project_android.model.entity

data class Customer(
    val id: String,
    val fullName: String,
    val phone: String,
    val email: String = "",
    val address: String = "",
    val note: String = "",
    val createdAt: String,
    val updatedAt: String
)
