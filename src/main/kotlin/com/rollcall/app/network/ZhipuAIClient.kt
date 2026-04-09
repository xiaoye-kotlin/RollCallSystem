package com.rollcall.app.network

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 智谱AI客户端
 * 用于调用智谱AI的API进行英语单词分析
 * 支持从本地配置文件读取API Key
 */
class ZhipuAIClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /** 智谱AI API地址 */
    private val apiUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions"

    /**
     * 从本地配置文件读取API Key
     * 配置文件路径: D:/Xiaoye/ai_config.txt
     */
    private val apiKey: String
        get() {
            val configFile = File("D:/Xiaoye/ai_config.txt")
            return if (configFile.exists()) {
                configFile.readText().trim()
            } else {
                ""
            }
        }

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
                    addProperty("model", "glm-4-flash")
                    addProperty("temperature", 0.1)
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

                val response = client.newCall(request).execute()
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
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e)
            }
        }.start()
    }
}
