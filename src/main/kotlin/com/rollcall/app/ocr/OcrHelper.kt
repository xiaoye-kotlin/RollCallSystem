package com.rollcall.app.ocr

import io.github.mymonstercat.Model
import io.github.mymonstercat.ocr.InferenceEngine
import java.io.File

/**
 * OCR文字识别工具类
 * 使用RapidOCR（基于ONNX/NCNN）进行本地OCR识别
 * 完全内嵌在项目中，不依赖任何外部安装的程序
 * 解决了在学校电脑上因缺少Tesseract而闪退的问题
 */
class OcrHelper {

    /**
     * 识别图片中的文字
     * 优先使用ONNX引擎（兼容性最好），失败时自动切换到NCNN引擎
     *
     * @param imageFile 要识别的图片文件
     * @return 识别出的文字内容
     */
    fun recognizeImage(imageFile: File): String {
        return try {
            // 使用ONNX引擎（兼容性最好，不依赖系统环境）
            val engine = InferenceEngine.getInstance(Model.ONNX_PPOCR_V3)
            val result = engine.runOcr(imageFile.absolutePath)

            if (result?.strRes != null && result.strRes.isNotEmpty()) {
                result.strRes.trim()
            } else {
                ""
            }
        } catch (e: Exception) {
            println("ONNX引擎识别失败: ${e.message}")
            // 备选方案：使用NCNN引擎
            tryNcnnEngine(imageFile, e)
        } finally {
            // 清理临时图片文件
            cleanupFile(imageFile)
        }
    }

    /**
     * 使用NCNN引擎作为备选方案
     */
    private fun tryNcnnEngine(imageFile: File, originalError: Exception): String {
        return try {
            val engine = InferenceEngine.getInstance(Model.NCNN_PPOCR_V3)
            val result = engine.runOcr(imageFile.absolutePath)
            if (result?.strRes != null && result.strRes.isNotEmpty()) {
                result.strRes.trim()
            } else {
                ""
            }
        } catch (e: Exception) {
            println("NCNN备选引擎也失败: ${e.message}")
            "识别失败: ${originalError.message}"
        }
    }

    /**
     * 安全删除临时文件
     */
    private fun cleanupFile(file: File) {
        try {
            if (file.exists()) file.delete()
        } catch (_: Exception) {
            // 忽略删除失败
        }
    }
}
