import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

// 学生数据模型
data class Student(
    val name: String = "",
    val probability: Int = 0
)

fun parseStudentJson(json: String): List<Student> {
    return try {
        val gson = Gson()
        val studentListType = object : TypeToken<List<Student>>() {}.type
        gson.fromJson(json, studentListType)
    } catch (e: JsonSyntaxException) {
        println("解析学生数据时发生错误: ${e.message}")
        emptyList()  // 返回空列表或你希望的默认值
    } catch (e: Exception) {
        println("发生未知错误: ${e.message}")
        emptyList()
    }
}

// 课程数据模型
data class Subject(
    val period: Int = 0,                // 添加默认值
    val subject: String = "",
    @SerializedName("start_time") val startTime: String = "",
    @SerializedName("end_time") val endTime: String = "",
    @SerializedName("dismissal_time") val dismissalTime: String = ""
)

// 每日课程表数据模型
data class DailySchedule(
    val schedule: List<Subject> = listOf()  // 添加默认值
)

// 解析 JSON 字符串并返回 Map 类型
fun parseSubjectJson(json: String): Map<String, DailySchedule> {
    return try {
        val gson = Gson()
        val type = object : TypeToken<Map<String, DailySchedule>>() {}.type
        gson.fromJson(json, type)
    } catch (e: JsonSyntaxException) {
        println("解析课程数据时发生错误: ${e.message}")
        emptyMap()  // 返回空的 Map 或你希望的默认值
    } catch (e: Exception) {
        println("发生未知错误: ${e.message}")
        emptyMap()
    }
}

// 定义 Root 类
data class TimeResponse(
    @SerializedName("date") val date: String = "",
    @SerializedName("weekday") val weekday: String = "",
    @SerializedName("timestamp") val timestamp: Long = 0L,
    @SerializedName("remark") val remark: String = ""
)

// 解析 JSON 数据
fun parseTimeJsonResponse(json: String): TimeResponse {
    return try {
        val gson = Gson()
        gson.fromJson(json, TimeResponse::class.java)
    } catch (e: JsonSyntaxException) {
        println("解析时间数据时发生错误: ${e.message}")
        TimeResponse()  // 返回默认值的实例
    } catch (e: Exception) {
        println("发生未知错误: ${e.message}")
        TimeResponse()  // 返回默认值的实例
    }
}