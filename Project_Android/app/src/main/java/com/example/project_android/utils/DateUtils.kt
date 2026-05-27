package com.example.project_android.utils

object DateUtils {
    fun nowIso(): String = java.text.SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        java.util.Locale.US
    ).format(java.util.Date())
}
