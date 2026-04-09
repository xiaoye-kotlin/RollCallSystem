package com.rollcall.app.audio

import com.rollcall.app.state.AppState
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.vosk.Model
import org.vosk.Recognizer
import java.io.StringReader
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

/**
 * 语音识别管理器
 * 基于Vosk引擎实现离线语音识别
 * 用于语音唤醒点名功能
 */
object VoiceRecognizer {

    /**
     * 获取系统支持的音频格式
     * 按优先级尝试多种格式
     */
    private fun getSupportedAudioFormat(): AudioFormat? {
        val formats = listOf(
            AudioFormat(16000f, 16, 1, true, false),  // 16kHz 小端
            AudioFormat(44100f, 16, 1, true, false),  // 44.1kHz 小端
            AudioFormat(16000f, 16, 1, true, true),   // 16kHz 大端
            AudioFormat(44100f, 16, 1, true, true)    // 44.1kHz 大端
        )

        for (format in formats) {
            try {
                val info = DataLine.Info(TargetDataLine::class.java, format)
                if (AudioSystem.isLineSupported(info)) return format
            } catch (_: Exception) { }
        }
        return null
    }

    /**
     * 语音识别循环
     * 持续监听麦克风输入并识别语音
     */
    private suspend fun startRecognitionLoop(
        recognizer: Recognizer?,
        updateResult: (String) -> Unit
    ) {
        val audioFormat = getSupportedAudioFormat()
            ?: return updateResult("无法找到支持的音频格式")

        val buffer = ByteArray(4096)

        try {
            val line: TargetDataLine = AudioSystem.getTargetDataLine(audioFormat).apply {
                open(audioFormat)
                start()
            }

            val recordingDuration = 500
            val bytesPerSecond = audioFormat.frameSize * audioFormat.sampleRate
            val bufferSize = (recordingDuration / 1000.0 * bytesPerSecond).toInt()
            val recordedBytes = ByteArray(bufferSize)
            var totalBytesRead = 0

            // 监听语音识别开关状态
            AppState.isVoiceIdentify.collectLatest { active ->
                if (!active) {
                    line.stop()
                    line.close()
                    return@collectLatest
                }

                while (AppState.isVoiceIdentify.value) {
                    val bytesRead = line.read(buffer, 0, buffer.size)
                    if (totalBytesRead + bytesRead > bufferSize) {
                        val bytesToCopy = bufferSize - totalBytesRead
                        System.arraycopy(buffer, 0, recordedBytes, totalBytesRead, bytesToCopy)
                        totalBytesRead += bytesToCopy

                        if (recognizer?.acceptWaveForm(recordedBytes, bufferSize) == true) {
                            updateResult(recognizer.result)
                        } else {
                            recognizer?.partialResult?.let { updateResult(it) }
                        }
                        totalBytesRead = 0
                    } else {
                        System.arraycopy(buffer, 0, recordedBytes, totalBytesRead, bytesRead)
                        totalBytesRead += bytesRead
                    }
                    delay(100)
                }
            }
        } catch (e: Exception) {
            updateResult("录音设备初始化失败：${e.message}")
        }
    }

    /**
     * 启动语音识别
     * @param onResult 识别结果回调
     */
    fun startVoiceRecognition(onResult: (String) -> Unit) {
        val recognizer: Recognizer?
        val model: Model?

        try {
            val modelPath = "D:\\vosk-model-small-cn-0.22"
            model = Model(modelPath)
            recognizer = Recognizer(model, 16000f)
            onResult("模型加载成功！")
        } catch (e: Exception) {
            onResult("模型加载失败：${e.message}")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            startRecognitionLoop(recognizer) { result ->
                // 解析JSON格式的识别结果
                val jsonReader = JsonReader(StringReader(result))
                jsonReader.isLenient = true
                val jsonElement = JsonParser.parseReader(jsonReader)

                if (jsonElement.isJsonObject) {
                    val jsonObject = jsonElement.asJsonObject
                    val content = when {
                        jsonObject.has("partial") -> jsonObject.get("partial").asString
                        jsonObject.has("text") -> jsonObject.get("text").asString
                        else -> "没有可用的识别结果"
                    }
                    onResult(content)
                } else {
                    onResult(result)
                }
            }
        }
    }
}
