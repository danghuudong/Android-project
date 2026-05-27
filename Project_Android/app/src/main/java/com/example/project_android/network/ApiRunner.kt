package com.example.project_android.network

import android.app.Activity
import android.widget.Toast

fun Activity.runApi(
    loadingMessage: String? = null,
    request: () -> ApiResult,
    failureMessage: ((ApiResult) -> String)? = null,
    onSuccess: (ApiResult) -> Unit
) {
    ApiClient.init(this)

    if (loadingMessage != null) {
        Toast.makeText(this, loadingMessage, Toast.LENGTH_SHORT).show()
    }

    // Khong thao tac SQLite tren main thread de UI khong bi dung.
    Thread {
        val result = request()

        runOnUiThread {
            if (result.isSuccess) {
                onSuccess(result)
            } else {
                Toast.makeText(
                    this,
                    failureMessage?.invoke(result)
                        ?: "SQLite loi (${result.statusCode}): ${result.body}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }.start()
}
