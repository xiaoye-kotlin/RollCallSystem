package com.rollcall.app.network

import com.rollcall.app.state.AppState
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

/**
 * 网络操作工具类
 * 负责所有网络请求、文件下载、远程配置获取等操作
 */
object NetworkHelper {

    /** 默认HTTP客户端 */
    private val client = OkHttpClient()

    /** 带超时配置的下载专用客户端 */
    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // ==================== 网络状态检测 ====================

    /**
     * 检测网络是否可用
     * 通过访问特定URL来判断网络连通性
     */
    suspend fun isInternetAvailable(): Boolean {
        val url = "https://sharechain.qq.com/78f70d1dbe37a73b519ea0e099a8a5b2"
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body.string()
                        val available = body.contains("网络正常")
                        AppState.setIsInternetAvailable(available)
                        available
                    } else {
                        AppState.setIsInternetAvailable(false)
                        false
                    }
                }
            } catch (e: IOException) {
                AppState.setIsInternetAvailable(false)
                false
            } catch (_: CancellationException) {
                AppState.setIsInternetAvailable(false)
                false
            }
        }
    }

    // ==================== 在线状态上报 ====================

    /** 上报在线状态 */
    suspend fun getOnline(): String {
        val url = "${AppState.url}/index.php?class=${AppState.CLASS}"
        return fetchString(url)
    }

    // ==================== 远程配置获取 ====================

    /** 获取全局API域名 */
    suspend fun getUrl(): String {
        return fetchConfigValue(
            "https://sharechain.qq.com/24611a0b19af84dfd745128cef471194",
            "【URL】", "http://202603.allfor.today"
        )
    }

    /** 获取程序开关状态 */
    suspend fun getIsOpen(): String {
        if (!isInternetAvailable()) return "true"
        return fetchConfigValue(
            "https://sharechain.qq.com/24611a0b19af84dfd745128cef471194",
            "【ISOPEN】", "true"
        )
    }

    /** 获取语音识别开关 */
    suspend fun getIsVoiceIdentifyOpen(): String {
        return fetchConfigValue(
            "https://sharechain.qq.com/24611a0b19af84dfd745128cef471194",
            "【ISVOICE】", "false"
        )
    }

    /** 获取时间提醒开关 */
    suspend fun getIsTimeOpen(): String {
        return fetchConfigValue(
            "https://sharechain.qq.com/24611a0b19af84dfd745128cef471194",
            "【ISTIME】", "false"
        )
    }

    /** 获取文件下载域名 */
    suspend fun getDownloadUrl(): String {
        return fetchConfigValue(
            "https://sharechain.qq.com/639ed2d6e00210eb5a61af91ade279c4",
            "【DOWNLOAD】",
            "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip"
        )
    }

    /** 获取时间API地址 */
    suspend fun getTimeApi(): String {
        if (!isInternetAvailable()) return "No Wifi"
        return fetchString("${AppState.url}/TimeApi.txt", "无")
    }

    /** 获取时间数据 */
    suspend fun getTimeData(): String {
        if (!isInternetAvailable()) return "No Wifi"
        return fetchString(AppState.timeApi, "无")
    }

    /** 获取学生名单 */
    suspend fun getNameList(): String {
        if (!isInternetAvailable()) return "No Wifi"
        return fetchString("${AppState.url}/NameList${AppState.CLASS}.txt", "无")
    }

    /** 获取课程表 */
    suspend fun getSubjectList(): String {
        if (!isInternetAvailable()) return "No Wifi"
        return fetchString("${AppState.url}/SubjectList${AppState.CLASS}.txt", "无")
    }

    /** 获取倒数日开关 */
    suspend fun getCountDownDaySwitch(): String {
        return fetchConfigValue(
            "https://sharechain.qq.com/d366c62ba7bb571c430ad1617a7ef037",
            "【ISCOUNTDOWNOPEN】", "false"
        )
    }

    /** 获取倒数日名称 */
    suspend fun getCountDownName(): String {
        return fetchConfigValue(
            "https://sharechain.qq.com/a94ef650a54e7cc5db49996ebf02865c",
            "【COUNTDOWNNAME】", "高考"
        )
    }

    /** 获取倒数日时间 */
    suspend fun getCountDownTime(): String {
        return fetchConfigValue(
            "https://sharechain.qq.com/a94ef650a54e7cc5db49996ebf02865c",
            "【COUNTDOWN】", "2026-6-7"
        )
    }

    /** 获取幸运学生 */
    suspend fun getLuckyGuy(): String {
        return fetchConfigValue(
            "https://sharechain.qq.com/d366c62ba7bb571c430ad1617a7ef037",
            "【LUCKYGUY】", "显福"
        )
    }

    /** 获取倒霉学生 */
    suspend fun getPoolGuy(): String {
        return fetchConfigValue(
            "https://sharechain.qq.com/c11d6e162220c343798e02290e909f1d",
            "【POOLGUY】", "显福"
        )
    }

    /** 获取彩蛋开关 */
    suspend fun getEasterEggSwitch(): String {
        return fetchConfigValue(
            "https://sharechain.qq.com/f46ec9220d2fc7236a420b9f80534c10",
            "【ISEASTEREGG】", "false"
        )
    }

    /** 获取倒计时开关 */
    suspend fun getCountDownSwitch(): String {
        return fetchConfigValue(
            "https://sharechain.qq.com/f46ec9220d2fc7236a420b9f80534c10",
            "【ISCOUNTDOWN】", "false"
        )
    }

    /** 获取壁纸开关 */
    suspend fun getWallpaperSwitch(): String {
        return fetchConfigValue(
            "https://sharechain.qq.com/f46ec9220d2fc7236a420b9f80534c10",
            "【ISWALLPAPER】", "false"
        )
    }

    /** 获取删除壁纸开关 */
    suspend fun getDeleteWallpaperSwitch(): String {
        return fetchConfigValue(
            "https://sharechain.qq.com/f46ec9220d2fc7236a420b9f80534c10",
            "【ISDELETEWALLPAPER】", "false"
        )
    }

    /** 获取学习模式开关 */
    suspend fun getLearningSwitch(): String {
        return fetchConfigValue(
            "https://sharechain.qq.com/f46ec9220d2fc7236a420b9f80534c10",
            "【ISLEARNING】", "false"
        )
    }

    /** 获取闹钟开关 */
    suspend fun getAlarmClock(): String {
        return fetchConfigValue(
            "https://sharechain.qq.com/f46ec9220d2fc7236a420b9f80534c10",
            "【ISALARMCLOCK】", "false"
        )
    }

    // ==================== 文件下载 ====================

    /**
     * 检查并下载模型/资源文件
     * 如果目标目录已存在且不为空则跳过下载
     * 支持ZIP文件自动解压
     *
     * @param url 下载地址
     * @param targetDir 下载到的目标目录
     * @param testDir 检测是否已存在的目录
     * @return 是否成功
     */
    fun checkAndCopyModel(url: String, targetDir: File, testDir: File): Boolean {
        // 如果目标目录已有内容，跳过下载
        if (testDir.exists() && testDir.isDirectory) {
            val files = testDir.listFiles()
            if (!files.isNullOrEmpty()) {
                println("\"download\" 目标目录已存在，跳过下载")
                return true
            }
        }

        return try {
            println("\"download\" 开始下载: $url")
            val request = Request.Builder().url(url).build()

            downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("下载失败: ${response.code} ${response.message}")
                }

                targetDir.mkdirs()
                val fileName = getFileNameFromUrl(url)
                val outputFile = File(targetDir, fileName)
                val isZip = fileName.endsWith(".zip", ignoreCase = true) ||
                        url.endsWith(".zip", ignoreCase = true)

                if (isZip) {
                    // ZIP文件自动解压（带Zip Slip路径遍历防护）
                    println("\"download\" 检测到ZIP文件，正在解压...")
                    val canonicalTargetDir = targetDir.canonicalFile
                    val canonicalTargetPath = canonicalTargetDir.path + File.separator
                    response.body.byteStream().use { inputStream ->
                        ZipInputStream(inputStream).use { zip ->
                            var entry = zip.nextEntry
                            var count = 0
                            while (entry != null) {
                                count++
                                val file = File(canonicalTargetDir, entry.name)
                                val canonicalFile = file.canonicalFile
                                // 防止Zip Slip攻击：验证解压路径不超出目标目录
                                if (!canonicalFile.path.startsWith(canonicalTargetPath)) {
                                    throw IOException("ZIP条目包含非法路径: ${entry.name}")
                                }
                                if (entry.isDirectory) {
                                    canonicalFile.mkdirs()
                                } else {
                                    canonicalFile.parentFile?.mkdirs()
                                    FileOutputStream(canonicalFile).use { fos -> zip.copyTo(fos) }
                                }
                                entry = zip.nextEntry
                            }
                            println("\"download\" 解压完成，共${count}个文件")
                        }
                    }
                } else {
                    // 普通文件直接保存
                    response.body.byteStream().use { input ->
                        FileOutputStream(outputFile).use { output -> input.copyTo(output) }
                    }
                }

                println("\"download\" 处理完成")
                true
            }
        } catch (e: Exception) {
            println("\"download\" 发生错误: ${e.message}")
            false
        }
    }

    // ==================== 浏览器操作 ====================

    /** 在默认浏览器中打开URL */
    fun openInBrowser(url: String) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
            } else {
                JOptionPane.showMessageDialog(null, "桌面操作不被支持")
            }
        } catch (e: URISyntaxException) {
            JOptionPane.showMessageDialog(null, "URL格式错误: ${e.message}")
        } catch (e: IOException) {
            JOptionPane.showMessageDialog(null, "无法打开URL: ${e.message}")
        }
    }

    // ==================== 私有工具方法 ====================

    /**
     * 从远程配置页面提取特定标记包裹的值
     * 格式：【TAG】value【TAG】
     */
    private suspend fun fetchConfigValue(url: String, tag: String, default: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body.string()
                        val pattern = Pattern.compile("${tag}(.*?)${tag}", Pattern.DOTALL)
                        val matcher = pattern.matcher(body)
                        if (matcher.find()) {
                            matcher.group(1) ?: default
                        } else {
                            default
                        }
                    } else {
                        default
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                default
            } catch (_: CancellationException) {
                default
            }
        }
    }

    /** 简单获取URL返回的文本内容 */
    private suspend fun fetchString(url: String, default: String = ""): String {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body.string()
                    } else {
                        default
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                default
            } catch (_: CancellationException) {
                default
            }
        }
    }

    /** 从URL中提取文件名 */
    private fun getFileNameFromUrl(url: String): String {
        return try {
            val path = URI(url).path
            if (path.isNotEmpty()) {
                val name = path.substringAfterLast('/')
                if (name.isNotEmpty()) return name
            }
            "downloaded_file"
        } catch (e: Exception) {
            "downloaded_file"
        }
    }
}
