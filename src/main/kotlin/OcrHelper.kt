import io.github.mymonstercat.Model
import io.github.mymonstercat.ocr.InferenceEngine
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

fun downloadEnglishLanguagePack() {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata")
        .build()

    try {
        println("Downloading languagePack...")

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                response.body.byteStream().use { inputStream ->
                    val outputPath = Paths.get("D:\\Xiaoye\\Tesseract-OCR\\tessdata\\eng.traineddata")
                    Files.createDirectories(outputPath.parent)
                    Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING)
                }
                println("Has Downloaded trainedData")
            } else {
                println("Err to Download trainedData, status code: ${response.code}")
            }
        }
    } catch (e: Exception) {
        println("Err to Download trainedData: ${e.message}")
    }
}

class OcrHelper {
    private val engine: InferenceEngine by lazy {
        InferenceEngine.getInstance(Model.NCNN_PPOCR_V3)
    }

    private val tesseractPath = "D:/OCR/tesseract.exe"
    private val language = "chi_sim+eng"

    /**
     * 通过命令行调用 tesseract.exe
     */
    private fun recognizeByCmd(imagePath: String): String {
        val process = ProcessBuilder(
            tesseractPath,
            imagePath,
            "stdout",
            "-l",
            language
        )
            .redirectErrorStream(true) // 合并 stderr，防止阻塞
            .start()

        return process.inputStream
            .bufferedReader(Charset.forName("UTF-8"))
            .readText()
            .trim()
    }

    /**
     * 对外统一接口（和你原来的一样）
     */
    fun recognizeImage(imageFile: File): String {
        return try {
            recognizeByCmd(imageFile.absolutePath)
        } catch (e: Exception) {
            "识别失败: ${e.message}"
        } finally {
            // 无论成功还是失败，都尝试删除图片
            try {
                if (imageFile.exists()) {
                    imageFile.delete()
                }
            } catch (_: Exception) {
                // 删除失败直接忽略，避免影响 OCR 结果
            }
        }
    }

}