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

/**
 * 音频播放管理器
 * 基于JavaFX MediaPlayer实现音频播放功能
 * 支持本地文件播放和网络下载后播放
 */
object AudioManager {

    /** 当前正在播放的播放器实例 */
    private var currentPlayer: MediaPlayer? = null

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

    /**
     * 下载音频文件并播放
     */
    private fun downloadAndPlay(url: String, file: File) {
        try {
            URI(url).toURL().openStream().use { input ->
                Files.copy(input, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            playLocal(file.absolutePath)
        } catch (e: Exception) {
            println("音频下载失败: ${e.message}")
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
