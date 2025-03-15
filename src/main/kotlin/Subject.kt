import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

fun getNextClassIfDismissalTime(currentTime: String, todaySchedule: List<Subject>?): String? {
    if (todaySchedule.isNullOrEmpty()) {
        return null
    }

    // 定义时间格式
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // 处理时间字符串，确保两位数字格式
    val formattedCurrentTime = if (currentTime.length == 4) "0$currentTime" else currentTime

    // 将输入的时间字符串转换为 LocalTime 类型
    val currentLocalTime = LocalTime.parse(formattedCurrentTime, timeFormatter)

    // 遍历课程表
    for (i in todaySchedule.indices) {
        val subject = todaySchedule[i]

        // 确保课程的开始时间、结束时间、下课时间是两位数字格式
        val formattedStartTime = if (subject.startTime.length == 4) "0${subject.startTime}" else subject.startTime
        if (subject.endTime.length == 4) "0${subject.endTime}" else subject.endTime
        if (subject.dismissalTime.length == 4) "0${subject.dismissalTime}" else subject.dismissalTime

        // 将课程的开始时间、结束时间、下课时间转换为 LocalTime 类型
        val subjectStartTime = LocalTime.parse(formattedStartTime, timeFormatter)

        // 如果是第一节课，不需要检查上一节课
        if (i == 0) {
            // 如果当前时间早于这节课的开始时间，返回下节课的名称
            if (Duration.between(currentLocalTime, subjectStartTime).toMinutes() > 1) {
                return subject.subject
            }
        } else {
            // 获取上一节课
            val previousSubject = todaySchedule[i - 1]
            val previousFormattedEndTime =
                if (previousSubject.endTime.length == 4) "0${previousSubject.endTime}" else previousSubject.endTime
            val previousSubjectEndTime = LocalTime.parse(previousFormattedEndTime, timeFormatter)

            // 判断当前时间是否处于上一节课的下课时间和当前课的上课时间之间
            if (currentLocalTime.isAfter(previousSubjectEndTime) && currentLocalTime.isBefore(subjectStartTime)) {
                return subject.subject // 当前时间在这两节课之间，返回当前课
            }
        }
    }

    return "未下课"
}