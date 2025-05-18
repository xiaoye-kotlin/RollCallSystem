import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


fun calculateDaysBetweenDates(date1: String, date2: String): Long {
    val formatter = DateTimeFormatter.ofPattern("yyyy-M-d")

    return try {
        val parsedDate1 = LocalDate.parse(date1.trim(), formatter)
        val parsedDate2 = LocalDate.parse(date2.trim(), formatter)

        ChronoUnit.DAYS.between(parsedDate1, parsedDate2).let { days ->
            if (days < 0) -days else days
        }
    } catch (e: Exception) {
        -1L
    }
}

@Composable
fun countdownDay() {

    val date = Global.date.collectAsState()
    var betweenDates: Long by remember { mutableStateOf(0) }
    betweenDates = calculateDaysBetweenDates(date.value, Global.countdownTime)

    if (betweenDates != -1L) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (betweenDates == 0L) {
                Text(
                    modifier = Modifier,
                    text = Global.countdownName,
                    fontSize = 25.sp,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold
                    ),
                )
                Text(
                    modifier = Modifier,
                    text = "就是今天！",
                    fontSize = 25.sp,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    modifier = Modifier,
                    text = "距离${Global.countdownName}还有 ",
                    fontSize = 25.sp,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    modifier = Modifier,
                    text = betweenDates.toString(),
                    fontSize = 25.sp,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold
                    ),
                )
                Text(
                    modifier = Modifier,
                    text = " 天",
                    fontSize = 25.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}