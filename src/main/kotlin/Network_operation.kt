import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.awt.Desktop
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import java.util.zip.ZipInputStream
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
                val responseBody = response.body.string()

                responseBody.contains("网络正常").let { Global.setIsInternetAvailable(it) }
                responseBody.contains("网络正常")

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

// 检查是否应该跳过下载
private fun shouldSkipDownload(testDir: File, targetDir: File): Boolean {
    // 检查 testDir 是否存在且不为空
    val testDirValid = testDir.exists() && testDir.list()?.isNotEmpty() == true

    // 检查 targetDir 是否存在且不为空（作为备用检查）
    val targetDirValid = targetDir.exists() && targetDir.list()?.isNotEmpty() == true

    return testDirValid || targetDirValid
}

// 验证下载是否真的成功
private fun verifyDownloadSuccess(targetDir: File): Boolean {
    return try {
        val files = targetDir.walkTopDown().filter { it.isFile }.toList()
        println("\"download\" Verification: Found ${files.size} files in target directory")
        files.isNotEmpty() // 如果目录中有文件，认为下载成功
    } catch (e: Exception) {
        println("\"download\" Verification failed: ${e.message}")
        false
    }
}

fun checkAndCopyModel(url: String, targetDir: File, testDir: File): Boolean {

    // ✅ 1. 先检查 testDir 是否存在且有内容
    if (testDir.exists() && testDir.isDirectory) {
        val files = testDir.listFiles()
        if (!files.isNullOrEmpty()) {
            println("\"download\" testDir already exists and is not empty, skip download")
            return true
        }
    }

    // 创建带有超时设置的 OkHttpClient
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val request = Request.Builder().url(url).build()

    return try {
        println("\"download\" Starting file download from: $url")

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response - ${response.message}")
            }

            println("\"download\" Download successful")

            // 创建目标目录（如果不存在）
            targetDir.mkdirs()

            val contentLength = response.body.contentLength()
            if (contentLength > 0) {
                println("\"download\" File size: ${contentLength / 1024 / 1024} MB")
            }

            val fileName = getFileNameFromUrl(url)
            val outputFile = File(targetDir, fileName)

            val isZipFile = fileName.endsWith(".zip", ignoreCase = true) ||
                    url.endsWith(".zip", ignoreCase = true)

            if (isZipFile) {
                println("\"download\" Detected ZIP file, extracting...")
                response.body.byteStream().use { inputStream ->
                    ZipInputStream(inputStream).use { zip ->
                        var entry = zip.nextEntry
                        var fileCount = 0
                        while (entry != null) {
                            fileCount++
                            val file = File(targetDir, entry.name)
                            if (entry.isDirectory) {
                                file.mkdirs()
                            } else {
                                file.parentFile?.mkdirs()
                                FileOutputStream(file).use { fos ->
                                    zip.copyTo(fos)
                                }
                            }
                            entry = zip.nextEntry
                        }
                        println("\"download\" Total files extracted: $fileCount")
                    }
                }
            } else {
                println("\"download\" Detected non-ZIP file, saving directly")
                response.body.byteStream().use { inputStream ->
                    FileOutputStream(outputFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }

            println("\"download\" Processing completed successfully")
            true
        }
    } catch (e: Exception) {
        println("\"download\" An error occurred: ${e.message}")
        false
    }
}

// 从 URL 中提取文件名
private fun getFileNameFromUrl(url: String): String {
    return try {
        // 从 URL 路径中获取文件名
        val path = URI(url).path
        if (path.isNotEmpty()) {
            val fileName = path.substringAfterLast('/')
            if (fileName.isNotEmpty()) {
                return fileName
            }
        }
        // 如果无法从 URL 获取文件名，使用默认名称
        "downloaded_file"
    } catch (e: Exception) {
        // 如果 URL 解析失败，使用默认名称
        "downloaded_file"
    }
}

suspend fun getDownloadUrl(): String {
    val url = "https://sharechain.qq.com/639ed2d6e00210eb5a61af91ade279c4"



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

suspend fun getLearningSwitch(): String {
    val url = "https://sharechain.qq.com/f46ec9220d2fc7236a420b9f80534c10"



    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                val pattern = Pattern.compile("【ISLEARNING】(.*?)【ISLEARNING】", Pattern.DOTALL)
                val matcher = pattern.matcher(responseBody)

                if (matcher.find()) {
                    println("IsLearning: ${matcher.group(1)}")
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

suspend fun getAlarmClock(): String {
    val url = "https://sharechain.qq.com/f46ec9220d2fc7236a420b9f80534c10"



    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                val pattern = Pattern.compile("【ISALARMCLOCK】(.*?)【ISALARMCLOCK】", Pattern.DOTALL)
                val matcher = pattern.matcher(responseBody)

                if (matcher.find()) {
                    println("ISALARMCLOCK: ${matcher.group(1)}")
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


/*// AI

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
}*/