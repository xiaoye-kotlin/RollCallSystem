package com.rollcall.app.audio

import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64
import kotlin.concurrent.thread

/**
 * 音频播放管理器
 * 基于JavaFX MediaPlayer实现音频播放功能
 * 支持本地文件播放和网络下载后播放
 */
object AudioManager {

    /** 当前正在播放的播放器实例 */
    private var currentPlayer: MediaPlayer? = null
    private var currentTtsProcess: Process? = null

    init {
        // 初始化JavaFX运行环境（必须调用一次）
        JFXPanel()
    }

    /**
     * 播放本地音频文件
     * @param filePath 音频文件路径
     */
    fun playLocal(filePath: String) {
        Platform.runLater {
            currentPlayer?.dispose()
            val media = Media(File(filePath).toURI().toString())
            currentPlayer = MediaPlayer(media).apply {
                play()
                setOnEndOfMedia {
                    dispose()
                    currentPlayer = null
                }
            }
        }
    }

    /**
     * 停止当前播放
     */
    fun stop() {
        currentPlayer?.stop()
        currentPlayer?.dispose()
        currentPlayer = null
        currentTtsProcess?.destroyForcibly()
        currentTtsProcess = null
    }

    /**
     * 播放点名语音（先查本地缓存，没有则下载）
     * @param name 学生姓名
     * @param baseUrl API基础地址
     */
    fun playNameAudio(name: String, baseUrl: String, isInternetAvailable: Boolean) {
        val voiceDir = File("D:/Xiaoye/Voice/").apply { if (!exists()) mkdirs() }
        val fileName = name.hashCode().toString() + ".mp3"
        val localFile = File(voiceDir, fileName)

        if (localFile.exists()) {
            playLocal(localFile.absolutePath)
        } else if (isInternetAvailable) {
            val encodedName = URLEncoder.encode(name, "UTF-8")
            downloadAndPlay("$baseUrl/voice.php?text=$encodedName", localFile)
        }
    }

    fun playTextAudio(text: String, baseUrl: String, isInternetAvailable: Boolean) {
        val normalizedText = text.trim()
        if (normalizedText.isBlank()) return

        if (System.getProperty("os.name", "").lowercase().contains("win")) {
            speakWithWindowsTts(normalizedText)
            return
        }

        val voiceDir = File("D:/Xiaoye/Voice/Words/").apply { if (!exists()) mkdirs() }
        val fileName = normalizedText.hashCode().toString() + ".mp3"
        val localFile = File(voiceDir, fileName)

        if (localFile.exists()) {
            playLocal(localFile.absolutePath)
        } else if (isInternetAvailable) {
            thread(start = true, isDaemon = true, name = "word-audio-$fileName") {
                val encodedText = URLEncoder.encode(normalizedText, "UTF-8")
                val primaryUrl = "$baseUrl/voice.php?text=$encodedText"
                val fallbackUrl = "https://dict.youdao.com/dictvoice?audio=$encodedText&type=2"

                if (!downloadAndPlay(primaryUrl, localFile)) {
                    downloadAndPlay(fallbackUrl, localFile)
                }
            }
        }
    }

    private fun speakWithWindowsTts(text: String) {
        thread(start = true, isDaemon = true, name = "windows-tts") {
            try {
                val encoded = Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))
                val script = """
                    Add-Type -AssemblyName System.Speech
                    ${'$'}speaker = New-Object System.Speech.Synthesis.SpeechSynthesizer
                    ${'$'}speaker.Volume = 100
                    ${'$'}speaker.Rate = 0
                    ${'$'}targetText = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String(${'$'}args[0]))
                    ${'$'}voice = ${'$'}speaker.GetInstalledVoices() | Where-Object { ${'$'}_.VoiceInfo.Culture.Name -like 'en*' } | Select-Object -First 1
                    if (${'$'}voice) { ${'$'}speaker.SelectVoice(${'$'}voice.VoiceInfo.Name) }
                    ${'$'}speaker.Speak(${'$'}targetText)
                    ${'$'}speaker.Dispose()
                """.trimIndent()

                stop()
                val process = ProcessBuilder(
                    "powershell",
                    "-NoProfile",
                    "-Command",
                    script,
                    encoded
                ).redirectErrorStream(true).start()
                currentTtsProcess = process
                val output = process.inputStream.bufferedReader().readText()
                val exitCode = process.waitFor()
                if (currentTtsProcess == process) {
                    currentTtsProcess = null
                }
                if (exitCode != 0) {
                    println("Windows TTS失败: $output")
                }
            } catch (e: Exception) {
                println("Windows TTS异常: ${e.message}")
            }
        }
    }

    /**
     * 下载音频文件并播放
     */
    private fun downloadAndPlay(url: String, file: File): Boolean {
        try {
            URI(url).toURL().openStream().use { input ->
                Files.copy(input, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            playLocal(file.absolutePath)
            return true
        } catch (e: Exception) {
            println("音频下载失败: ${e.message}")
            return false
        }
    }
}

/**
 * 独立音频控制器
 * 用于倒计时、彩蛋等需要独立播放的场景
 */
class AudioController {

    private var mediaPlayer: MediaPlayer? = null

    init {
        JFXPanel() // 确保JavaFX已初始化
    }

    /**
     * 播放音频文件
     * @param filePath 文件路径
     * @param onFinished 播放完成回调
     */
    fun play(filePath: String, onFinished: (() -> Unit)? = null) {
        val media = Media(File(filePath).toURI().toString())
        mediaPlayer = MediaPlayer(media).apply {
            setOnEndOfMedia {
                println("音频播放完毕")
                onFinished?.invoke()
            }
            play()
        }
    }

    /** 停止播放 */
    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.dispose()
        mediaPlayer = null
    }
}
