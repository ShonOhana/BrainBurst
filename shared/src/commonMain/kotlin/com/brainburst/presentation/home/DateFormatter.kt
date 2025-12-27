package com.brainburst.presentation.home

import kotlinx.datetime.LocalDate

/**
 * Formats a date string (yyyy-MM-dd) to a display format like "Friday, Dec 26"
 */
object DateFormatter {
    fun formatPuzzleDate(dateStr: String): String {
        return try {
            // Parse the date string (yyyy-MM-dd) - ISO format
            val date = LocalDate.parse(dateStr)
            
            // Calculate day of week (Monday = 0, Sunday = 6)
            // LocalDate.dayOfWeek is an enum, we need to get the ordinal
            val dayOfWeekName = formatDayOfWeek(date.dayOfWeek.ordinal)
            val monthName = formatMonth(date.monthNumber)
            val day = date.dayOfMonth
            
            "$dayOfWeekName, $monthName $day"
        } catch (e: Exception) {
            // Fallback to the original date string if parsing fails
            dateStr
        }
    }
    
    private fun formatDayOfWeek(ordinal: Int): String {
        // LocalDate.dayOfWeek: Monday = 0, Sunday = 6
        return when (ordinal) {
            0 -> "Monday"
            1 -> "Tuesday"
            2 -> "Wednesday"
            3 -> "Thursday"
            4 -> "Friday"
            5 -> "Saturday"
            6 -> "Sunday"
            else -> ""
        }
    }
    
    private fun formatMonth(monthNumber: Int): String {
        return when (monthNumber) {
            1 -> "Jan"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Apr"
            5 -> "May"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Aug"
            9 -> "Sep"
            10 -> "Oct"
            11 -> "Nov"
            12 -> "Dec"
            else -> ""
        }
    }
}

