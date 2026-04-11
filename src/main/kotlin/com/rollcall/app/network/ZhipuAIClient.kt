package com.rollcall.app.network

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.rollcall.app.state.AppState
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.SocketTimeoutException
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.io.File
import kotlin.math.min

/**
 * 智谱AI客户端
 * 用于调用智谱AI的API进行英语单词分析
 * 配置全部来自远程下发并缓存在AppState
 */
class ZhipuAIClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(150, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val http1Client = client.newBuilder()
        .protocols(listOf(Protocol.HTTP_1_1))
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

                executeWithRetry(request, onSuccess, onError)
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e)
            }
        }.start()
    }

    fun askVisionQuestionAsync(
        imageFile: File,
        prompt: String,
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

                val imageBase64 = Base64.getEncoder().encodeToString(imageFile.readBytes())
                println(
                    "AI vision request -> model=$model, url=$apiUrl, imageBytes=${imageFile.length()}, base64Length=${imageBase64.length}"
                )
                val contentArray = JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("type", "text")
                        addProperty("text", prompt)
                    })
                    add(JsonObject().apply {
                        addProperty("type", "image_url")
                        add("image_url", JsonObject().apply {
                            addProperty("url", "data:image/png;base64,$imageBase64")
                        })
                    })
                }
                val message = JsonObject().apply {
                    addProperty("role", "user")
                    add("content", contentArray)
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

                println("AI vision payload preview -> ${requestBody.toString().take(300)}")
                executeWithRetry(request, onSuccess, onError)
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e)
            }
        }.start()
    }

    private fun executeWithRetry(
        request: Request,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val attempts = listOf(
            "primary" to client,
            "http1-fallback" to http1Client
        )

        var lastError: Exception? = null

        for ((label, callClient) in attempts) {
            try {
                callClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val bodyPreview = response.body.string().take(300)
                        println("AI response failed[$label] -> code=${response.code}, body=$bodyPreview")
                        throw Exception("API请求失败[$label]: ${response.code} ${bodyPreview.ifBlank { response.message }}")
                    }

                    val responseBody = response.body.string()
                    println("AI response success[$label] -> bodyPreview=${responseBody.take(500)}")
                    val content = extractAssistantContent(responseBody)
                    onSuccess(content)
                    return
                }
            } catch (e: SocketTimeoutException) {
                println("AI请求超时[$label]: ${e.message}")
                lastError = Exception("AI请求超时[$label]，已尝试重试", e)
            } catch (e: Exception) {
                val shouldRetry = label == "primary" && looksTransient(e)
                if (!shouldRetry) {
                    onError(e)
                    return
                }
                println("AI请求失败[$label]，准备降级重试: ${e.message}")
                lastError = e
            }
        }

        onError(lastError ?: Exception("AI请求失败"))
    }

    private fun looksTransient(error: Exception): Boolean {
        val message = error.message.orEmpty().lowercase()
        return error is SocketTimeoutException ||
            "timeout" in message ||
            "stream was reset" in message ||
            "connection reset" in message ||
            "unexpected end of stream" in message ||
            "502" in message ||
            "503" in message ||
            "504" in message ||
            "bad gateway" in message ||
            "service unavailable" in message ||
            "gateway timeout" in message
    }

    private fun extractAssistantContent(responseBody: String): String {
        val messageContent = gson.fromJson(responseBody, JsonObject::class.java)
            .getAsJsonArray("choices")[0]
            .asJsonObject.getAsJsonObject("message")
            .get("content")

        return when {
            messageContent.isJsonPrimitive -> messageContent.asString
            messageContent.isJsonArray -> {
                messageContent.asJsonArray.joinToString("\n") { item ->
                    val obj = item.asJsonObject
                    when {
                        obj.has("text") -> obj.get("text").asString
                        obj.has("content") -> obj.get("content").asString
                        else -> obj.toString()
                    }
                }
            }
            else -> messageContent.toString()
        }
    }
}
