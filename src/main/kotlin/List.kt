import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import okhttp3.Response

data class Student(
    val name: String = "",
    val probability: Int = 0
)

fun parseStudentJson(json: String): List<Student> {
    //使用 Gson 将 JSON 转换为 List<Student>
    val gson = Gson()
    val studentListType = object : TypeToken<List<Student>>() {}.type
    return gson.fromJson(json, studentListType)
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
    val gson = Gson()
    val type = object : TypeToken<Map<String, DailySchedule>>() {}.type
    return gson.fromJson(json, type)
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
    val gson = Gson()
    return gson.fromJson(json, TimeResponse::class.java)  // 使用 TimeResponse 类来解析
}
