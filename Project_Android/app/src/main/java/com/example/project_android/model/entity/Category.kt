package com.example.project_android.model.entity

data class Category(
    val id: String,
    val name: String,
    val description: String,
    val iconUri: String,
    val productCount: Int,
    val createdAt: String,
    val updatedAt: String
)
