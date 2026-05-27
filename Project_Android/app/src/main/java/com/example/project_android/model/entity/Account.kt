package com.example.project_android.model.entity

data class Account(
    val id: String,
    val fullName: String,
    val email: String,
    val password: String,
    val role: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)
