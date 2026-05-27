package com.example.project_android.utils

object CurrencyUtils {
    fun formatVnd(value: Double): String = "${String.format("%,.0f", value)}đ"
}
