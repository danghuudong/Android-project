package com.example.project_android.network

import com.example.project_android.BuildConfig
import java.net.HttpURLConnection
import java.net.URL

object DashboardReportUploader {

    fun upload(fileName: String, bytes: ByteArray): Result<String> {
        return runCatching {
            val connection = (URL(BuildConfig.REPORT_EXPORT_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5_000
                readTimeout = 10_000
                doOutput = true
                setRequestProperty(
                    "Content-Type",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
                setRequestProperty("X-File-Name", fileName)
                setFixedLengthStreamingMode(bytes.size)
            }

            try {
                connection.outputStream.use { it.write(bytes) }
                val body = if (connection.responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                }

                if (connection.responseCode !in 200..299) {
                    throw IllegalStateException("M\u00e1y t\u00ednh t\u1eeb ch\u1ed1i l\u01b0u file (${connection.responseCode}): $body")
                }
                body.ifBlank { fileName }
            } finally {
                connection.disconnect()
            }
        }
    }
}
