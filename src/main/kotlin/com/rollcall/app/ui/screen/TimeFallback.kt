package com.rollcall.app.ui.screen

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

internal fun resolveWeekKey(remoteWeek: String): String {
    if (remoteWeek != "无" && remoteWeek.isNotBlank()) {
        return remoteWeek
    }
    return when (LocalDate.now().dayOfWeek.value) {
        1 -> "星期一"
        2 -> "星期二"
        3 -> "星期三"
        4 -> "星期四"
        5 -> "星期五"
        6 -> "星期六"
        7 -> "星期日"
        else -> "无"
    }
}

internal fun resolveCurrentTimeStr(remoteTime: String): String {
    if (remoteTime != "无" && remoteTime.isNotBlank()) {
        return remoteTime
    }
    return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
}
