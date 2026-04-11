package com.rollcall.app.ocr

import io.github.mymonstercat.Model
import io.github.mymonstercat.ocr.InferenceEngine
import io.github.mymonstercat.ocr.config.HardwareConfig
import java.io.File
import java.io.IOException
import java.lang.reflect.Field
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.atomic.AtomicBoolean

/**
 * OCR文字识别工具类
 * 对RapidOCR做一层隔离，修复它在多引擎切换和临时目录复用时的状态污染问题。
 */
class OcrHelper {

    /**
     * 识别图片中的文字
     * 优先使用ONNX引擎，失败时再尝试NCNN。
     * 注意：此方法不会删除传入的图片文件，调用者需自行管理文件生命周期
     *
     * @param imageFile 要识别的图片文件
     * @return 识别出的文字内容
     */
    fun recognizeImage(imageFile: File): String {
        require(imageFile.exists()) { "OCR截图文件不存在: ${imageFile.absolutePath}" }

        val failures = mutableListOf<String>()

        synchronized(OCR_LOCK) {
            for (candidate in ENGINE_CANDIDATES) {
                val text = runCatching {
                    recognizeWithEngine(imageFile, candidate)
                }.onFailure { error ->
                    println("${candidate.label} OCR识别失败: ${error.message}")
                    failures += "${candidate.label}: ${error.message ?: error::class.java.simpleName}"
                }.getOrNull()

                if (text != null) {
                    return text
                }
            }
        }

        throw IllegalStateException(
            buildString {
                append("OCR初始化失败")
                if (failures.isNotEmpty()) {
                    append("，已尝试：")
                    append(failures.joinToString(" | "))
                }
            }
        )
    }

    private fun recognizeWithEngine(imageFile: File, candidate: OcrEngineCandidate): String {
        ensureModelFiles(candidate)
        val targetEngineType = candidate.model.modelType
        val shouldResetNativeLoader = activeEngineType != null && activeEngineType != targetEngineType
        resetRapidOcrRuntime(resetNativeLoader = shouldResetNativeLoader)
        activeEngineType = targetEngineType

        val engine = InferenceEngine.getInstance(candidate.model, candidate.hardwareConfig)
        val result = engine.runOcr(imageFile.absolutePath)
        return result?.strRes?.trim().orEmpty()
    }

    private fun ensureModelFiles(candidate: OcrEngineCandidate) {
        val targetDir = candidate.modelDir
        val isBroken = candidate.requiredFiles.any { name ->
            val file = File(targetDir, name)
            !file.exists() || file.length() <= 0L
        }

        if (isBroken && targetDir.exists()) {
            targetDir.deleteRecursively()
        }

        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw IOException("无法创建OCR模型目录: ${targetDir.absolutePath}")
        }

        candidate.requiredFiles.forEach { fileName ->
            val targetFile = File(targetDir, fileName)
            if (targetFile.exists() && targetFile.length() > 0L) {
                return@forEach
            }
            copyResourceToFile("models/$fileName", targetFile)
        }
    }

    private fun copyResourceToFile(resourcePath: String, targetFile: File) {
        targetFile.parentFile?.mkdirs()
        val inputStream = OcrHelper::class.java.classLoader.getResourceAsStream(resourcePath)
            ?: throw IOException("OCR模型资源缺失: $resourcePath")

        inputStream.use { stream ->
            Files.copy(stream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        if (!targetFile.exists() || targetFile.length() <= 0L) {
            throw IOException("OCR模型写入失败: ${targetFile.absolutePath}")
        }
    }

    private fun resetRapidOcrRuntime(resetNativeLoader: Boolean) {
        setStaticField("io.github.mymonstercat.JarFileUtil", "tempDir", null)
        setStaticField("io.github.mymonstercat.ocr.InferenceEngine", "inferenceEngine", null)
        setStaticField("io.github.mymonstercat.ocr.InferenceEngine", "modelsLoader", null)

        if (resetNativeLoader) {
            setStaticField("io.github.mymonstercat.ocr.InferenceEngine", "nativeLoader", null)

            val isLibraryLoadedField = getDeclaredField(
                "io.github.mymonstercat.ocr.InferenceEngine",
                "isLibraryLoaded"
            )
            val state = isLibraryLoadedField.get(null) as? AtomicBoolean
                ?: throw IllegalStateException("无法重置RapidOCR库状态")
            state.set(false)
        }
    }

    private fun setStaticField(className: String, fieldName: String, value: Any?) {
        val field = getDeclaredField(className, fieldName)
        field.set(null, value)
    }

    private fun getDeclaredField(className: String, fieldName: String): Field {
        val clazz = Class.forName(className)
        return clazz.getDeclaredField(fieldName).apply {
            isAccessible = true
        }
    }

    private companion object {
        private val OCR_LOCK = Any()
        private val OCR_BASE_DIR = File(System.getProperty("java.io.tmpdir"), "ocrJava")
        private val CPU_ONLY_CONFIG = HardwareConfig(
            (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1),
            -1
        )
        private var activeEngineType: String? = null

        private val ENGINE_CANDIDATES: List<OcrEngineCandidate> by lazy {
            buildList {
                add(
                    OcrEngineCandidate(
                        label = "ONNX",
                        model = Model.ONNX_PPOCR_V3,
                        hardwareConfig = CPU_ONLY_CONFIG,
                        requiredFiles = listOf(
                            "ch_PP-OCRv3_det_infer.onnx",
                            "ch_ppocr_mobile_v2.0_cls_infer.onnx",
                            "ch_PP-OCRv3_rec_infer.onnx",
                            "ppocr_keys_v1.txt"
                        )
                    )
                )

                if (!System.getProperty("os.arch").contains("86") || System.getProperty("os.arch").contains("64")) {
                    add(
                        OcrEngineCandidate(
                            label = "NCNN",
                            model = Model.NCNN_PPOCR_V3,
                            hardwareConfig = CPU_ONLY_CONFIG,
                            requiredFiles = listOf(
                                "ch_PP-OCRv3_det_infer.bin",
                                "ch_PP-OCRv3_det_infer.param",
                                "ch_ppocr_mobile_v2.0_cls_infer.bin",
                                "ch_ppocr_mobile_v2.0_cls_infer.param",
                                "ch_PP-OCRv3_rec_infer.bin",
                                "ch_PP-OCRv3_rec_infer.param",
                                "ppocr_keys_v1.txt"
                            )
                        )
                    )
                }
            }
        }

        private data class OcrEngineCandidate(
            val label: String,
            val model: Model,
            val hardwareConfig: HardwareConfig,
            val requiredFiles: List<String>
        ) {
            val modelDir: File = File(OCR_BASE_DIR, model.modelType)
        }
    }
}
