package com.rollcall.app.util

import com.rollcall.app.data.model.Subject
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 课程表工具类
 * 提供判断下节课的逻辑
 */
object ScheduleHelper {

    /**
     * 判断当前时间是否在课间，如果是则返回下节课名称
     * @param currentTime 当前时间字符串（HH:mm格式）
     * @param todaySchedule 今天的课程列表
     * @return 下节课名称，如果未下课返回"未下课"，如果没课返回null
     */
    fun getNextClassIfDismissalTime(currentTime: String, todaySchedule: List<Subject>?): String? {
        if (todaySchedule.isNullOrEmpty()) return null

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        // 确保时间格式为两位数（如 8:00 → 08:00）
        val formattedCurrentTime = if (currentTime.length == 4) "0$currentTime" else currentTime
        val currentLocalTime = LocalTime.parse(formattedCurrentTime, timeFormatter)

        for (i in todaySchedule.indices) {
            val subject = todaySchedule[i]

            // 格式化课程开始时间
            val formattedStartTime = if (subject.startTime.length == 4) "0${subject.startTime}" else subject.startTime
            val subjectStartTime = LocalTime.parse(formattedStartTime, timeFormatter)

            if (i == 0) {
                // 第一节课：如果当前时间早于上课时间超过1分钟，显示这节课
                if (Duration.between(currentLocalTime, subjectStartTime).toMinutes() > 1) {
                    return subject.subject
                }
            } else {
                // 其他课程：判断是否在上一节课下课后、这节课上课前
                val previousSubject = todaySchedule[i - 1]
                val previousFormattedEndTime =
                    if (previousSubject.endTime.length == 4) "0${previousSubject.endTime}" else previousSubject.endTime
                val previousSubjectEndTime = LocalTime.parse(previousFormattedEndTime, timeFormatter)

                if (currentLocalTime.isAfter(previousSubjectEndTime) && currentLocalTime.isBefore(subjectStartTime)) {
                    return subject.subject
                }
            }
        }

        return "未下课"
    }
}
