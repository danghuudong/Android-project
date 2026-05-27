package com.example.project_android.utils

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object ImageUtils {
    const val EMPTY_IMAGE_URL = ""
    private val imageExecutor = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun bindImage(target: ImageView, imageUrl: String, fallbackResId: Int) {
        // Reset truoc de ImageView khong giu anh cu khi render lai danh sach.
        target.setImageResource(fallbackResId)
        target.tag = imageUrl

        if (imageUrl.isBlank()) return

        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            bindRemoteImage(target, imageUrl, fallbackResId)
            return
        }

        try {
            val uri = Uri.parse(imageUrl)
            val bitmap = target.context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }

            if (bitmap != null) {
                target.setImageBitmap(bitmap)
            } else {
                target.setImageURI(uri)
            }
        } catch (_: Exception) {
            target.setImageResource(fallbackResId)
        }
    }

    private fun bindRemoteImage(target: ImageView, imageUrl: String, fallbackResId: Int) {
        imageExecutor.execute {
            val bitmap = try {
                val connection = URL(imageUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 8_000
                connection.readTimeout = 8_000
                connection.instanceFollowRedirects = true
                connection.inputStream.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            } catch (_: Exception) {
                null
            }

            mainHandler.post {
                // Neu ImageView da duoc bind sang item khac thi khong set anh cu vao nua.
                if (target.tag != imageUrl) return@post

                if (bitmap != null) {
                    target.setImageBitmap(bitmap)
                } else {
                    target.setImageResource(fallbackResId)
                }
            }
        }
    }
}
