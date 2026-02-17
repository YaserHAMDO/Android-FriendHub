package com.engyh.friendhub.presentation.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    fun getAgeFromBirthdateSafe(birthDateString: String): Int? {
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
            val birthDate = LocalDate.parse(birthDateString, formatter)
            Period.between(birthDate, LocalDate.now()).years
        } catch (e: Exception) {
            null
        }
    }

    fun formatTimestamp(timestamp: Timestamp): String {
        val messageDate = timestamp.toDate()
        val currentDate = Date()

        val calendarMessage = Calendar.getInstance().apply { time = messageDate }
        val calendarToday = Calendar.getInstance().apply { time = currentDate }

        val sameDay =
            calendarMessage.get(Calendar.YEAR) == calendarToday.get(Calendar.YEAR) &&
                    calendarMessage.get(Calendar.DAY_OF_YEAR) == calendarToday.get(Calendar.DAY_OF_YEAR)

        if (sameDay) {
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            return timeFormat.format(messageDate)
        }

        calendarToday.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday =
            calendarMessage.get(Calendar.YEAR) == calendarToday.get(Calendar.YEAR) &&
                    calendarMessage.get(Calendar.DAY_OF_YEAR) == calendarToday.get(Calendar.DAY_OF_YEAR)


        if (yesterday) {
            return "Yesterday"
        }

        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        return dateFormat.format(messageDate)
    }
}
