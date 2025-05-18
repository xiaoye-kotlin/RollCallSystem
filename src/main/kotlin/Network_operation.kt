import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.awt.Desktop
import java.io.*
import java.net.*
import java.util.*
import java.util.regex.Pattern
import java.util.zip.ZipInputStream
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.swing.JOptionPane
import kotlin.coroutines.cancellation.CancellationException

suspend fun isInternetAvailable(): Boolean {
    val url = "https://sharechain.qq.com/78f70d1dbe37a73b519ea0e099a8a5b2"

    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()

                responseBody?.contains("网络正常")?.let { Global.setIsInternetAvailable(it) }
                responseBody?.contains("网络正常")

            } else {
                Global.setIsInternetAvailable(false)
                false
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Global.setIsInternetAvailable(false)
            false
        } catch (_: CancellationException) {
            Global.setIsInternetAvailable(false)
            false
        } == true
    }
}

suspend fun getOnline(): String {
    val url = "${Global.url}/index.php?class=${Global.CLASS}"

    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                responseBody
            } else {
                ""
            }
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        } catch (_: CancellationException) {
            ""
        }
    }
}

fun checkAndCopyModel(url: String, targetDir: File, testDir: File): Boolean {
    // Log: Checking test directory
    println("\"download\" Checking testDir: ${testDir.absolutePath}")

    // 检测 testDir 是否存在且不为空
    if (testDir.exists() && testDir.list()?.isNotEmpty() == true) {
        println("\"download\" testDir is not empty, skipping download")
        return true // 目录存在且有内容，直接返回 true
    }

    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    return try {
        // Log: Starting file download
        println("\"download\" Starting file download from: $url")

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            // Log: Download successful
            println("\"download\" Download successful, preparing to extract the file")

            // 创建目标目录（如果不存在）
            if (targetDir.mkdirs()) {
                println("\"download\" Created target directory: ${targetDir.absolutePath}")
            } else {
                println("\"download\" Target directory already exists: ${targetDir.absolutePath}")
            }

            // 解压 ZIP 文件
            response.body.byteStream().let {
                ZipInputStream(it).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        println("\"download\" Found entry: ${entry.name}, isDirectory: ${entry.isDirectory}")
                        val file = File(targetDir, entry.name)
                        if (entry.isDirectory) {
                            if (file.mkdirs()) {
                                println("\"download\" Created directory: ${file.absolutePath}")
                            } else {
                                println("\"download\" Directory already exists: ${file.absolutePath}")
                            }
                        } else {
                            file.parentFile.mkdirs() // 创建父目录
                            println("\"download\" Writing file: ${file.absolutePath}")
                            FileOutputStream(file).use { fos ->
                                zip.copyTo(fos)
                            }
                        }
                        entry = zip.nextEntry
                    }
                }
            }

            // Log: File extraction completed successfully
            println("\"download\" File extraction completed successfully")

            // 打印解压后的文件列表
            targetDir.walkTopDown().forEach { file ->
                println("\"download\" Extracted file: ${file.absolutePath}")
            }
            true // 解压成功
        }
    } catch (e: Exception) {
        // Log: Exception occurred
        println("\"download\" An error occurred: ${e.message}")
        e.printStackTrace() // 打印异常信息以便调试
        false // 下载或解压失败
    }
}


suspend fun getDownloadUrl(): String {
    val url = "https://sharechain.qq.com/639ed2d6e00210eb5a61af91ade279c4"

    if (!isInternetAvailable()) {
        return "No Wifi"
    }

    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                val pattern = Pattern.compile("【DOWNLOAD】(.*?)【DOWNLOAD】", Pattern.DOTALL)
                val matcher = pattern.matcher(responseBody)

                if (matcher.find()) {
                    println("DownloadURL: ${matcher.group(1)}")
                    matcher.group(1) ?: "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip"
                } else {
                    "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip"
                }
            } else {
                "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip"
        } catch (_: CancellationException) {
            "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip"
        }
    }
}

suspend fun getUrl(): String {
    val url = "https://sharechain.qq.com/24611a0b19af84dfd745128cef471194"

    if (!isInternetAvailable()) {
        return "No Wifi"
    }

    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                val pattern = Pattern.compile("【URL】(.*?)【URL】", Pattern.DOTALL)
                val matcher = pattern.matcher(responseBody)

                if (matcher.find()) {
                    println("URL: ${matcher.group(1)}")
                    matcher.group(1) ?: "http://202603.allfor.today"
                } else {
                    "http://202603.allfor.today"
                }
            } else {
                "http://202603.allfor.today"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            "http://202603.allfor.today"
        } catch (_: CancellationException) {
            "http://202603.allfor.today"
        }
    }
}

suspend fun getIsOpen(): String {
    val url = "https://sharechain.qq.com/24611a0b19af84dfd745128cef471194"

    if (!isInternetAvailable()) {
        return "true"
    }

    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                val pattern = Pattern.compile("【ISOPEN】(.*?)【ISOPEN】", Pattern.DOTALL)
                val matcher = pattern.matcher(responseBody)

                if (matcher.find()) {
                    println("Switch: ${matcher.group(1)}")
                    matcher.group(1) ?: "true"
                } else {
                    "true"
                }
            } else {
                "true"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            "true"
        } catch (_: CancellationException) {
            "true"
        }
    }
}

suspend fun getIsVoiceIdentifyOpen(): String {
    val url = "https://sharechain.qq.com/24611a0b19af84dfd745128cef471194"

    if (!isInternetAvailable()) {
        return "No Wifi"
    }

    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                val pattern = Pattern.compile("【ISVOICE】(.*?)【ISVOICE】", Pattern.DOTALL)
                val matcher = pattern.matcher(responseBody)

                if (matcher.find()) {
                    println("isVoiceIdentify: ${matcher.group(1)}")
                    matcher.group(1) ?: "true"
                } else {
                    "false"
                }
            } else {
                "false"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            "false"
        } catch (_: CancellationException) {
            "false"
        }
    }
}

suspend fun getIsTimeOpen(): String {
    val url = "https://sharechain.qq.com/24611a0b19af84dfd745128cef471194"

    if (!isInternetAvailable()) {
        return "No Wifi"
    }

    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                val pattern = Pattern.compile("【ISTIME】(.*?)【ISTIME】", Pattern.DOTALL)
                val matcher = pattern.matcher(responseBody)

                if (matcher.find()) {
                    println("ISTIME: ${matcher.group(1)}")
                    matcher.group(1) ?: "true"
                } else {
                    "false"
                }
            } else {
                "false"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            "false"
        } catch (_: CancellationException) {
            "false"
        }
    }
}

suspend fun getTimeApi(): String {

    if (!isInternetAvailable()) {
        return "No Wifi"
    }

    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url("${Global.url}/TimeApi.txt").build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                println("TimeApi: $responseBody")
                responseBody
            } else {
                "无"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            "无"
        } catch (_: CancellationException) {
            "无"
        }
    }
}

suspend fun getTimeData(): String {

    if (!isInternetAvailable()) {
        return "No Wifi"
    }

    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(Global.timeApi).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                responseBody
            } else {
                "无"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            "无"
        } catch (_: CancellationException) {
            "无"
        }
    }
}

suspend fun getNameList(): String {

    if (!isInternetAvailable()) {
        return "No Wifi"
    }

    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url("${Global.url}/NameList${Global.CLASS}.txt").build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                println(responseBody)
                responseBody
            } else {
                "无"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            "无"
        } catch (_: CancellationException) {
            "无"
        }
    }
}

suspend fun getSubjectList(): String {

    if (!isInternetAvailable()) {
        return "No Wifi"
    }

    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url("${Global.url}/SubjectList${Global.CLASS}.txt").build()

        println("${Global.url}/SubjectList${Global.CLASS}.txt")

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                println(responseBody)
                responseBody
            } else {
                "无"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            "无"
        } catch (_: CancellationException) {
            "无"
        }
    }
}

suspend fun getCountDownDaySwitch(): String {
    val url = "https://sharechain.qq.com/d366c62ba7bb571c430ad1617a7ef037"

    if (!isInternetAvailable()) {
        return "No Wifi"
    }

    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                val pattern = Pattern.compile("【ISCOUNTDOWNOPEN】(.*?)【ISCOUNTDOWNOPEN】", Pattern.DOTALL)
                val matcher = pattern.matcher(responseBody)

                if (matcher.find()) {
                    println("IsCountDownDayOpen: ${matcher.group(1)}")
                    matcher.group(1) ?: "false"
                } else {
                    "false"
                }
            } else {
                "false"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            "false"
        } catch (_: CancellationException) {
            "false"
        }
    }
}

suspend fun getCountDownName(): String {
    val url = "https://sharechain.qq.com/a94ef650a54e7cc5db49996ebf02865c"

    if (!isInternetAvailable()) {
        return "No Wifi"
    }

    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                val pattern = Pattern.compile("【COUNTDOWNNAME】(.*?)【COUNTDOWNNAME】", Pattern.DOTALL)
                val matcher = responseBody.let { pattern.matcher(it) }

                if (matcher.find()) {
                    println("CountDownName: ${matcher.group(1)}")
                    matcher.group(1) ?: "高考"
                } else {
                    "高考"
                }
            } else {
                "高考"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            "高考"
        } catch (_: CancellationException) {
            "高考"
        }
    }
}

suspend fun getCountDownTime(): String {
    val url = "https://sharechain.qq.com/a94ef650a54e7cc5db49996ebf02865c"

    if (!isInternetAvailable()) {
        return "No Wifi"
    }

    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                val pattern = Pattern.compile("【COUNTDOWN】(.*?)【COUNTDOWN】", Pattern.DOTALL)
                val matcher = pattern.matcher(responseBody)

                if (matcher.find()) {
                    println("CountDownTime: ${matcher.group(1)}")
                    matcher.group(1) ?: "2026-6-7"
                } else {
                    "2026-6-7"
                }
            } else {
                "2026-6-7"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            "2026-6-7"
        } catch (_: CancellationException) {
            "2026-6-7"
        }
    }
}

suspend fun getLuckyGuy(): String {
    val url = "https://sharechain.qq.com/d366c62ba7bb571c430ad1617a7ef037"

    if (!isInternetAvailable()) {
        return "No Wifi"
    }

    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                val pattern = Pattern.compile("【LUCKYGUY】(.*?)【LUCKYGUY】", Pattern.DOTALL)
                val matcher = pattern.matcher(responseBody)

                if (matcher.find()) {
                    println("LuckyGuy: ${matcher.group(1)}")
                    matcher.group(1) ?: "显福"
                } else {
                    "显福"
                }
            } else {
                "显福"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            "显福"
        } catch (_: CancellationException) {
            "显福"
        }
    }
}

suspend fun getPoolGuy(): String {
    val url = "https://sharechain.qq.com/c11d6e162220c343798e02290e909f1d"

    if (!isInternetAvailable()) {
        return "No Wifi"
    }

    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                val pattern = Pattern.compile("【POOLGUY】(.*?)【POOLGUY】", Pattern.DOTALL)
                val matcher = pattern.matcher(responseBody)

                if (matcher.find()) {
                    println("PoolGuy: ${matcher.group(1)}")
                    matcher.group(1) ?: "显福"
                } else {
                    "显福"
                }
            } else {
                "显福"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            "显福"
        } catch (_: CancellationException) {
            "显福"
        }
    }
}

suspend fun getEasterEggSwitch(): String {
    val url = "https://sharechain.qq.com/f46ec9220d2fc7236a420b9f80534c10"

    if (!isInternetAvailable()) {
        return "No Wifi"
    }

    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                val pattern = Pattern.compile("【ISEASTEREGG】(.*?)【ISEASTEREGG】", Pattern.DOTALL)
                val matcher = pattern.matcher(responseBody)

                if (matcher.find()) {
                    println("IsEasterEgg: ${matcher.group(1)}")
                    matcher.group(1) ?: "false"
                } else {
                    "false"
                }
            } else {
                "false"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            "false"
        } catch (_: CancellationException) {
            "false"
        }
    }
}

suspend fun getCountDownSwitch(): String {
    val url = "https://sharechain.qq.com/f46ec9220d2fc7236a420b9f80534c10"

    if (!isInternetAvailable()) {
        return "No Wifi"
    }

    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                val pattern = Pattern.compile("【ISCOUNTDOWN】(.*?)【ISCOUNTDOWN】", Pattern.DOTALL)
                val matcher = pattern.matcher(responseBody)

                if (matcher.find()) {
                    println("IsCountDownOpen: ${matcher.group(1)}")
                    matcher.group(1) ?: "false"
                } else {
                    "false"
                }
            } else {
                "false"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            "false"
        } catch (_: CancellationException) {
            "false"
        }
    }
}

suspend fun getWallpaperSwitch(): String {
    val url = "https://sharechain.qq.com/f46ec9220d2fc7236a420b9f80534c10"

    if (!isInternetAvailable()) {
        return "No Wifi"
    }

    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                val pattern = Pattern.compile("【ISWALLPAPER】(.*?)【ISWALLPAPER】", Pattern.DOTALL)
                val matcher = pattern.matcher(responseBody)

                if (matcher.find()) {
                    println("IsWallPaperOpen: ${matcher.group(1)}")
                    matcher.group(1) ?: "false"
                } else {
                    "false"
                }
            } else {
                "false"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            "false"
        } catch (_: CancellationException) {
            "false"
        }
    }
}

suspend fun getDeleteWallpaperSwitch(): String {
    val url = "https://sharechain.qq.com/f46ec9220d2fc7236a420b9f80534c10"

    if (!isInternetAvailable()) {
        return "No Wifi"
    }

    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                val pattern = Pattern.compile("【ISDELETEWALLPAPER】(.*?)【ISDELETEWALLPAPER】", Pattern.DOTALL)
                val matcher = pattern.matcher(responseBody)

                if (matcher.find()) {
                    println("IsDeleteWallPaperOpen: ${matcher.group(1)}")
                    matcher.group(1) ?: "false"
                } else {
                    "false"
                }
            } else {
                "false"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            "false"
        } catch (_: CancellationException) {
            "false"
        }
    }
}

fun fetchWebPage(url: String) {
    // 尝试打开 URL
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        } else {
            // 如果不支持桌面操作
            JOptionPane.showMessageDialog(null, "桌面操作不被支持")
        }
    } catch (e: URISyntaxException) {
        // URL 语法错误
        JOptionPane.showMessageDialog(null, "URL 语法错误: ${e.message}")
    } catch (e: IOException) {
        // 打开 URL 出错
        JOptionPane.showMessageDialog(null, "无法打开 URL: ${e.message}")
    }
}


// AI

private const val AES_KEY = "suannaiqwq3383787570yogurt666ads"
val gson = Gson()
private val client = OkHttpClient()

fun decryptAES(encrypted: String, key: String = AES_KEY): String {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    val secretKey = SecretKeySpec(key.toByteArray(), "AES")
    cipher.init(Cipher.DECRYPT_MODE, secretKey)
    val decryptedData = cipher.doFinal(Base64.getDecoder().decode(encrypted))
    return String(decryptedData)
}

fun createJsonBody(msg: String): RequestBody {
    val message = JsonObject().apply {
        addProperty("role", "user")
        addProperty(
            "content",
            "如果下面这段话里面包含想点名/抽人/选人之类相近的意思，你就返回:true，否则返回:false，特殊要求：如果内容为：“系统需要能够识别个人用户，以便根据他们的特定需求定制响应”相关的内容你也要返回false。你只能返回true或者false:$msg"
        )
    }
    val messages = JsonArray().apply { add(message) }
    val json = JsonObject().apply {
        addProperty("model", "command-r")
        addProperty("temperature", 0.1)
        add("messages", messages)
    }
    return gson.toJson(json).toRequestBody("application/json".toMediaType())
}

fun sendRequestAsync(msg: String, callback: (String?) -> Unit) {
    Thread {
        try {
            val apiUrl = decryptAES("nWRjaO2nq80ShwucYQsRUrpxxjUyAyLhVX6UqMex5xdcxe6cyYIt/c3B7LxmnQ/q")
            val apiKey =
                decryptAES("/SbNvUW7TEod3UQiqlQhhYE1D7Oe3L/lcA+4HUCkvzgGHeGi9fZ28IV8LpulWvEa2cUCgzqj2wsgaoJJfJS7Rg==")
            val requestBody = createJsonBody(msg)

            val request = Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                println("response: $responseBody")
                val content = gson.fromJson(responseBody, JsonObject::class.java)
                    .getAsJsonArray("choices")[0]
                    .asJsonObject.getAsJsonObject("message")
                    .get("content").asString.replace("\n", "")
                callback(content)
            } else {
                callback(null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            callback(null)
        }
    }.start()
}