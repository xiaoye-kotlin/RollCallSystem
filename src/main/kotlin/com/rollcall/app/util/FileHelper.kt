package com.rollcall.app.util

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/** Top-level delegates for convenience */
fun readFromFile(filePath: String): String = FileHelper.readFromFile(filePath)
fun writeToFile(filePath: String, content: String) = FileHelper.writeToFile(filePath, content)
fun createDirectory(directoryPath: String) = FileHelper.createDirectory(directoryPath)
fun deleteFileOrDirectory(filePath: String) = FileHelper.deleteFileOrDirectory(filePath)

/**
 * 文件操作工具类
 * 提供文件的读写、删除、目录创建等操作
 */
object FileHelper {

    private val gson = Gson()

    /**
     * 创建目录（如果不存在）
     * @param directoryPath 目录路径
     */
    fun createDirectory(directoryPath: String) {
        val path: Path = Paths.get(directoryPath)
        if (!Files.exists(path)) {
            Files.createDirectories(path)
        }
    }

    /**
     * 写入文件内容（自动创建父目录）
     * @param filePath 文件路径
     * @param content 文件内容
     */
    fun writeToFile(filePath: String, content: String) {
        val directoryPath = File(filePath).parent
        if (directoryPath != null) {
            createDirectory(directoryPath)
        }
        File(filePath).writeText(content)
    }

    /**
     * 读取文件内容
     * @param filePath 文件路径
     * @return 文件内容，文件不存在返回"404"
     */
    fun readFromFile(filePath: String): String {
        val file = File(filePath)
        return if (file.exists()) {
            file.readText()
        } else {
            "404"
        }
    }

    /**
     * 递归删除文件或目录
     * @param filePath 文件或目录路径
     */
    fun deleteFileOrDirectory(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            if (file.isDirectory) {
                file.listFiles()?.forEach { deleteFileOrDirectory(it.absolutePath) }
            }
            file.delete()
        }
    }

    /**
     * 验证JSON字符串是否合法
     * @param jsonString JSON字符串
     * @return 是否为有效的JSON
     */
    fun isValidJson(jsonString: String): Boolean {
        return try {
            Gson().fromJson(jsonString, Any::class.java)
            true
        } catch (_: JsonSyntaxException) {
            false
        }
    }

    /**
     * 记录学生被点名次数
     * @param studentName 学生姓名
     */
    fun recordAttendance(studentName: String) {
        val filePath = "D:/Xiaoye/StatisticalData.json"
        val jsonData = readFromFile(filePath)

        val type = object : TypeToken<MutableMap<String, Int>>() {}.type
        val dataMap: MutableMap<String, Int> = if (jsonData != "404") {
            gson.fromJson(jsonData, type)
        } else {
            mutableMapOf()
        }

        // 更新点名次数
        dataMap[studentName] = (dataMap[studentName] ?: 0) + 1

        // 写回文件
        writeToFile(filePath, gson.toJson(dataMap))
    }
}
