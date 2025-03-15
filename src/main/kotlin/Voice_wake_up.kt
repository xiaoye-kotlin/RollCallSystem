import Global.isVoiceIdentify
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

// 获取支持的音频格式
fun getSupportedAudioFormat(): AudioFormat? {
    val possibleFormats = listOf(
        AudioFormat(16000f, 16, 1, true, false),  // 16kHz, 16位, 单声道, 小端字节序
        AudioFormat(44100f, 16, 1, true, false),  // 44.1kHz, 16位, 单声道, 小端字节序
        AudioFormat(16000f, 16, 1, true, true),   // 16kHz, 16位, 单声道, 大端字节序
        AudioFormat(44100f, 16, 1, true, true)    // 44.1kHz, 16位, 单声道, 大端字节序
    )

    for (format in possibleFormats) {
        try {
            val info = DataLine.Info(TargetDataLine::class.java, format)
            if (AudioSystem.isLineSupported(info)) {
                return format
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return null
}


// 进行语音识别
suspend fun startRecognitionLoop(
    recognizer: Recognizer?,
    updateResult: (String) -> Unit
) {
    val audioFormat = getSupportedAudioFormat() ?: return updateResult("无法找到支持的音频格式")

    val buffer = ByteArray(4096)

    try {
        // 在 try 中初始化 line
        val line: TargetDataLine = AudioSystem.getTargetDataLine(audioFormat).apply {
            open(audioFormat)
            start()
        }

        val recordingDuration = 500
        val bytesPerSecond = audioFormat.frameSize * audioFormat.sampleRate // 每秒读取的字节数
        val bufferSize = (recordingDuration / 1000.0 * bytesPerSecond).toInt()
        val recordedBytes = ByteArray(bufferSize)
        var totalBytesRead = 0

        // 使用 collectLatest 以实时响应 isVoiceIdentify 的状态变化
        isVoiceIdentify.collectLatest { identifyActive ->
            if (!identifyActive) {
                line.stop()
                line.close()
                return@collectLatest
            }

            // 语音识别循环
            while (isVoiceIdentify.value) {
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

                    // 重置计数
                    totalBytesRead = 0
                } else {
                    System.arraycopy(buffer, 0, recordedBytes, totalBytesRead, bytesRead)
                    totalBytesRead += bytesRead
                }

                // 检测间隔设置为100ms，提高实时性
                delay(100)
            }
        }
    } catch (e: Exception) {
        updateResult("初始化录音设备失败：${e.message}")
    }
}


fun voice(onResult: (String) -> Unit) {
    val recognizer: Recognizer?
    val model: Model?

    // 初始化模型
    try {
        val modelPath = "D:\\vosk-model-small-cn-0.22"
        model = Model(modelPath)  // 加载模型
        recognizer = Recognizer(model, 16000f)
        onResult("模型加载成功！")
    } catch (e: Exception) {
        onResult("模型加载失败：${e.message}")
        return
    }

    // 使用全局的 CoroutineScope 启动协程进行异步语音识别
    CoroutineScope(Dispatchers.IO).launch {
        startRecognitionLoop(recognizer) { result ->
            // 使用 JsonReader 解析 JSON 数据，开启宽松模式
            val jsonReader = JsonReader(StringReader(result))
            jsonReader.isLenient = true // 启用宽松解析
            val jsonElement = JsonParser.parseReader(jsonReader)

            // 确保是一个 JsonObject
            if (jsonElement.isJsonObject) {
                val jsonObject = jsonElement.asJsonObject

                // 获取内容
                val content = when {
                    jsonObject.has("partial") -> jsonObject.get("partial").asString
                    jsonObject.has("text") -> jsonObject.get("text").asString
                    else -> "没有可用的识别结果"
                }

                // 返回提取的内容
                onResult(content)
            } else {
                onResult(result)
            }
        }
    }
}