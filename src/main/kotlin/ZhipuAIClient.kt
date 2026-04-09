import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 智谱AI客户端 - 用于调用智谱AI的API进行英语单词分析
 */
class ZhipuAIClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // 智谱AI API地址
    private val apiUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions"

    // API Key - 需要替换为实际的key
    private val apiKey: String
        get() {
            // 尝试从本地配置文件读取
            val configFile = java.io.File("D:/Xiaoye/ai_config.txt")
            return if (configFile.exists()) {
                configFile.readText().trim()
            } else {
                "" // 如果没有配置文件，返回空字符串
            }
        }

    /**
     * 异步发送问题到智谱AI
     * @param question 要发送的问题
     * @param onSuccess 成功回调，返回AI的回答
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
                    onError(Exception("API Key未配置，请在D:/Xiaoye/ai_config.txt中配置"))
                    return@Thread
                }

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
                    onError(Exception("API请求失败: ${response.code} ${response.message}"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e)
            }
        }.start()
    }
}
