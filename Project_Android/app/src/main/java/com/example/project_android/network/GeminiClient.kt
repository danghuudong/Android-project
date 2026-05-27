package com.example.project_android.network

import com.example.project_android.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiClient {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    fun generateBusinessInsight(prompt: String): Result<String> {
        return generateText(prompt)
    }

    fun generateText(prompt: String): Result<String> {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Chua cau hinh GEMINI_API_KEY trong local.properties"))
        }

        return runCatching {
            val requestBody = buildRequestBody(prompt).toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/${BuildConfig.GEMINI_MODEL}:generateContent")
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("Gemini API loi ${response.code}: ${body.take(180)}")
                }

                extractText(JSONObject(body))
                    .ifBlank { throw IOException("Gemini khong tra ve noi dung") }
            }
        }
    }

    private fun buildRequestBody(prompt: String): JSONObject {
        return JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.4)
                    .put("maxOutputTokens", 900)
                    .put(
                        "thinkingConfig",
                        JSONObject().put("thinkingBudget", 0)
                    )
            )
    }

    private fun extractText(response: JSONObject): String {
        val candidates = response.optJSONArray("candidates") ?: return ""
        val parts = candidates
            .optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts") ?: return ""

        val result = StringBuilder()
        for (index in 0 until parts.length()) {
            val text = parts.optJSONObject(index)?.optString("text").orEmpty()
            if (text.isNotBlank()) {
                if (result.isNotEmpty()) result.append("\n")
                result.append(text)
            }
        }
        return result.toString().trim()
    }
}
