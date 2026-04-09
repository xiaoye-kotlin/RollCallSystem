import io.github.mymonstercat.Model
import io.github.mymonstercat.ocr.InferenceEngine
import java.io.File

/**
 * OCR识别工具类
 * 使用RapidOCR（基于ONNX/NCNN）进行本地OCR识别
 * 不依赖外部安装的Tesseract，解决学校电脑闪退问题
 */
class OcrHelper {

    /**
     * 使用RapidOCR进行图片文字识别
     * RapidOCR是纯Java实现，打包进项目中，无需安装任何外部程序
     * 支持中英文混合识别
     */
    fun recognizeImage(imageFile: File): String {
        return try {
            // 使用RapidOCR的ONNX引擎进行识别（兼容性最好，不依赖系统环境）
            val engine = InferenceEngine.getInstance(Model.ONNX_PPOCR_V3)
            val result = engine.runOcr(imageFile.absolutePath)

            // 提取识别结果文本
            if (result != null && result.strRes != null && result.strRes.isNotEmpty()) {
                result.strRes.trim()
            } else {
                ""
            }
        } catch (e: Exception) {
            println("RapidOCR识别失败: ${e.message}")
            e.printStackTrace()
            // 如果ONNX引擎失败，尝试使用NCNN引擎作为备选方案
            try {
                val fallbackEngine = InferenceEngine.getInstance(Model.NCNN_PPOCR_V3)
                val fallbackResult = fallbackEngine.runOcr(imageFile.absolutePath)
                if (fallbackResult != null && fallbackResult.strRes != null && fallbackResult.strRes.isNotEmpty()) {
                    fallbackResult.strRes.trim()
                } else {
                    ""
                }
            } catch (e2: Exception) {
                println("NCNN备选引擎也失败: ${e2.message}")
                "识别失败: ${e.message}"
            }
        } finally {
            // 无论成功还是失败，都尝试删除临时图片
            try {
                if (imageFile.exists()) {
                    imageFile.delete()
                }
            } catch (_: Exception) {
                // 删除失败直接忽略，避免影响OCR结果
            }
        }
    }
}