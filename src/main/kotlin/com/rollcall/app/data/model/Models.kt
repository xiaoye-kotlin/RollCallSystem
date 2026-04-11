package com.rollcall.app.data.model

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

/**
 * 学生数据模型
 * @param name 学生姓名
 * @param probability 被点名的概率权重
 */
data class Student(
    val name: String = "",
    val probability: Int = 0
)

/**
 * 解析学生JSON数据
 * @param json JSON字符串
 * @return 学生列表
 */
fun parseStudentJson(json: String): List<Student> {
    return try {
        val gson = Gson()
        val type = object : TypeToken<List<Student>>() {}.type
        gson.fromJson(json, type)
    } catch (e: JsonSyntaxException) {
        println("解析学生数据失败: ${e.message}")
        emptyList()
    } catch (e: Exception) {
        println("未知错误: ${e.message}")
        emptyList()
    }
}

/**
 * 课程数据模型
 * @param period 课程节次
 * @param subject 课程名称
 * @param startTime 上课时间
 * @param endTime 下课时间
 * @param dismissalTime 放学时间
 */
data class Subject(
    val period: Int = 0,
    val subject: String = "",
    @SerializedName("start_time") val startTime: String = "",
    @SerializedName("end_time") val endTime: String = "",
    @SerializedName("dismissal_time") val dismissalTime: String = ""
)

/**
 * 每日课程表数据模型
 * @param schedule 当天的课程列表
 */
data class DailySchedule(
    val schedule: List<Subject> = listOf()
)

/**
 * 解析课程表JSON数据
 * @param json JSON字符串
 * @return 按星期分组的课程表
 */
fun parseSubjectJson(json: String): Map<String, DailySchedule> {
    return try {
        val gson = Gson()
        val type = object : TypeToken<Map<String, DailySchedule>>() {}.type
        gson.fromJson(json, type)
    } catch (e: JsonSyntaxException) {
        println("解析课程数据失败: ${e.message}")
        emptyMap()
    } catch (e: Exception) {
        println("未知错误: ${e.message}")
        emptyMap()
    }
}

/**
 * 时间API响应数据模型
 */
data class TimeResponse(
    @SerializedName("date") val date: String = "",
    @SerializedName("weekday") val weekday: String = "",
    @SerializedName("timestamp") val timestamp: Long = 0L,
    @SerializedName("remark") val remark: String = ""
)

/**
 * 解析时间API返回的JSON数据
 */
fun parseTimeJsonResponse(json: String): TimeResponse {
    return try {
        val gson = Gson()
        gson.fromJson(json, TimeResponse::class.java)
    } catch (e: JsonSyntaxException) {
        println("解析时间数据失败: ${e.message}")
        TimeResponse()
    } catch (e: Exception) {
        println("未知错误: ${e.message}")
        TimeResponse()
    }
}

/**
 * 英语单词识别结果数据模型
 * @param word 单词
 * @param type 词性
 * @param meaning 中文释义
 * @param category 类型（new_word=生词, familiar_new_meaning=熟词生义）
 */
data class WordItem(
    val word: String,
    val type: String,
    val meaning: String,
    val category: String,
    val example: String = "",
    val root: String = ""
)

data class VocabularyBookEntry(
    val word: String,
    val type: String,
    val meaning: String,
    val category: String,
    val example: String = "",
    val root: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val addedCount: Int = 1,
    val isLearned: Boolean = false
)
