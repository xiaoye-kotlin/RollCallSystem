package com.rollcall.app.ocr

import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Robot
import java.awt.image.BufferedImage
import java.io.File
import java.lang.reflect.Method
import javax.imageio.ImageIO

/**
 * 屏幕截图工具类
 * 支持高分辨率截图（Java 9+）和多显示器
 */
object ScreenshotHelper {
    private val ocrHelper = OcrHelper()

    /**
     * 静默截图
     * 调用者负责管理返回截图文件生命周期
     */
    fun takeSilentScreenshot(): File {
        val outputFile = File.createTempFile("rollcall_ocr_", ".png").apply {
            deleteOnExit()
        }

        try {
            val robot = Robot()
            val screenBounds = getAllScreenBounds()
            val image = tryGetHighResolutionScreenshot(robot, screenBounds)
                ?: robot.createScreenCapture(screenBounds)

            ImageIO.write(image, "png", outputFile)
            return outputFile
        } catch (e: Exception) {
            try { if (outputFile.exists()) outputFile.delete() } catch (_: Exception) { }
            throw e
        }
    }

    /**
     * 静默截图并进行OCR识别
     * 调用者负责管理返回的截图文件生命周期
     * @return Pair<截图文件, 识别结果文字>
     */
    fun takeSilentScreenshotAndRecognize(): Pair<File, String> {
        val outputFile = takeSilentScreenshot()
        try {
            val ocrResult = ocrHelper.recognizeImage(outputFile)
            return Pair(outputFile, ocrResult)
        } catch (e: Exception) {
            try { if (outputFile.exists()) outputFile.delete() } catch (_: Exception) { }
            throw e
        }
    }

    /**
     * 获取所有屏幕的总边界（支持多显示器）
     */
    private fun getAllScreenBounds(): Rectangle {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val screens = ge.screenDevices

        var minX = 0; var minY = 0; var maxX = 0; var maxY = 0

        for (screen in screens) {
            val bounds = screen.defaultConfiguration.bounds
            if (bounds.x < minX) minX = bounds.x
            if (bounds.y < minY) minY = bounds.y
            val currentMaxX = bounds.x + bounds.width
            val currentMaxY = bounds.y + bounds.height
            if (currentMaxX > maxX) maxX = currentMaxX
            if (currentMaxY > maxY) maxY = currentMaxY
        }

        return Rectangle(minX, minY, maxX - minX, maxY - minY)
    }

    /**
     * 尝试获取高分辨率截图（Java 9+特性）
     * 如果不支持则返回null，调用者使用普通截图
     */
    private fun tryGetHighResolutionScreenshot(robot: Robot, screenBounds: Rectangle): BufferedImage? {
        return try {
            val method: Method = robot.javaClass.getMethod(
                "createMultiResolutionScreenCapture",
                Rectangle::class.java
            )
            val multiResImage = method.invoke(robot, screenBounds)
            val getVariantsMethod = multiResImage.javaClass.getMethod("getResolutionVariants")
            val variants = getVariantsMethod.invoke(multiResImage) as List<*>

            // 选择分辨率最高的变体
            var bestImage: BufferedImage? = null
            var maxPixels = 0
            for (variant in variants) {
                if (variant is BufferedImage) {
                    val pixels = variant.width * variant.height
                    if (pixels > maxPixels) {
                        maxPixels = pixels
                        bestImage = variant
                    }
                }
            }
            bestImage ?: variants.firstOrNull() as? BufferedImage
        } catch (_: Exception) {
            null
        }
    }
}
