package com.rollcall.app.network

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.rollcall.app.state.AppState
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 智谱AI客户端
 * 用于调用智谱AI的API进行英语单词分析
 * 配置全部来自远程下发并缓存在AppState
 */
class ZhipuAIClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private val apiUrl: String
        get() = AppState.aiApiUrl.trim().ifEmpty { AppState.DEFAULT_AI_API_URL }

    private val apiKey: String
        get() = AppState.aiApiKey.trim()

    private val model: String
        get() = AppState.aiModel.trim().ifEmpty { AppState.DEFAULT_AI_MODEL }

    private val temperature: Double
        get() = AppState.aiTemperature.coerceIn(0.0, 1.5)

    /**
     * 异步发送问题到智谱AI
     * @param question 问题内容
     * @param onSuccess 成功回调，返回AI回答
     * @param onError 失败回调
     */
    fun askQuestionAsync(
        question: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        Thread {
            try {
                val key = apiKey
                if (key.isEmpty()) {
                    onError(Exception("API Key未配置"))
                    return@Thread
                }

                // 构建请求体
                val message = JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", question)
                }
                val messages = JsonArray().apply { add(message) }
                val json = JsonObject().apply {
                    addProperty("model", model)
                    addProperty("temperature", temperature)
                    add("messages", messages)
                }

                val requestBody = gson.toJson(json)
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(apiUrl)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer $key")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body.string()
                        val content = gson.fromJson(responseBody, JsonObject::class.java)
                            .getAsJsonArray("choices")[0]
                            .asJsonObject.getAsJsonObject("message")
                            .get("content").asString
                        onSuccess(content)
                    } else {
                        onError(Exception("API请求失败: ${response.code}"))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e)
            }
        }.start()
    }
}
