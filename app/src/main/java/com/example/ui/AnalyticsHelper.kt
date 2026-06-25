package com.example.ui

import com.example.data.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class LifeAnalytics(
    val completedCount: Int,
    val missedCount: Int,
    val pendingCount: Int,
    val productivityScore: Int,
    val weeklyCompletionRate: Int,
    val mostProductiveDay: String,
    val leastProductiveDay: String,
    val weeklyTrend: List<Int>, // completion counts per day of week
    val monthlyPerformanceLabel: String, // Disciplined, Focused, Irregular
    val longestStreak: Int,
    val highestFocusDurationMinutes: Int
)

object AnalyticsHelper {

    fun calculateAnalytics(allTasks: List<Task>): LifeAnalytics {
        if (allTasks.isEmpty()) {
            return LifeAnalytics(
                completedCount = 0, missedCount = 0, pendingCount = 0, productivityScore = 0,
                weeklyCompletionRate = 0, mostProductiveDay = "N/A", leastProductiveDay = "N/A",
                weeklyTrend = listOf(0, 0, 0, 0, 0, 0, 0), monthlyPerformanceLabel = "Irregular",
                longestStreak = 0, highestFocusDurationMinutes = 0
            )
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdf.format(Date())

        // 1. Daily Counts for Today
        val todayTasks = allTasks.filter { it.date == todayStr }
        val completedToday = todayTasks.count { it.isCompleted }
        val pendingToday = todayTasks.count { !it.isCompleted }
        
        // A missed task is either an unfinished task from a past date,
        // or a task whose time has passed today.
        val sdfTime = SimpleDateFormat("HH:mm", Locale.US)
        val currentTimeStr = sdfTime.format(Date())
        val missedToday = todayTasks.count { !it.isCompleted && it.time < currentTimeStr }
        val pastUnfinished = allTasks.filter { it.date < todayStr && !it.isCompleted }.count()
        val totalMissed = missedToday + pastUnfinished

        val todayTotal = todayTasks.size
        val score = if (todayTotal > 0) (completedToday * 100) / todayTotal else 0

        // 2. Weekly calculations
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val sevenDaysAgoStr = sdf.format(calendar.time)
        val weeklyTasks = allTasks.filter { it.date >= sevenDaysAgoStr }
        val weeklyCompleted = weeklyTasks.count { it.isCompleted }
        val weeklyTotal = weeklyTasks.size
        val weeklyRate = if (weeklyTotal > 0) (weeklyCompleted * 100) / weeklyTotal else 0

        // Day of week completion mapper
        val dayOfWeekMap = mutableMapOf<Int, Int>() // Calendar day of week to completion count
        val dayOfWeekTotalMap = mutableMapOf<Int, Int>()
        for (task in allTasks) {
            try {
                val parsedDate = sdf.parse(task.date)
                if (parsedDate != null) {
                    val cal = Calendar.getInstance()
                    cal.time = parsedDate
                    val dow = cal.get(Calendar.DAY_OF_WEEK)
                    dayOfWeekTotalMap[dow] = (dayOfWeekTotalMap[dow] ?: 0) + 1
                    if (task.isCompleted) {
                        dayOfWeekMap[dow] = (dayOfWeekMap[dow] ?: 0) + 1
                    }
                }
            } catch (e: Exception) {
                // ignore parsing error
            }
        }

        // English day names for mapping
        val dayNames = mapOf(
            Calendar.SUNDAY to "Sun",
            Calendar.MONDAY to "Mon",
            Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu",
            Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat"
        )

        var maxCompleted = -1
        var mostProd = "N/A"
        var minCompleted = Int.MAX_VALUE
        var leastProd = "N/A"

        for (dow in Calendar.SUNDAY..Calendar.SATURDAY) {
            val completedCount = dayOfWeekMap[dow] ?: 0
            val totalCount = dayOfWeekTotalMap[dow] ?: 0
            val dayName = dayNames[dow] ?: "N/A"

            if (totalCount > 0) {
                if (completedCount > maxCompleted) {
                    maxCompleted = completedCount
                    mostProd = dayName
                }
                if (completedCount < minCompleted) {
                    minCompleted = completedCount
                    leastProd = dayName
                }
            }
        }

        // 3. Weekly trend line points (completion count for last 7 calendar days)
        val weeklyTrend = mutableListOf<Int>()
        val trendCal = Calendar.getInstance()
        trendCal.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0..6) {
            val dateToCheck = sdf.format(trendCal.time)
            val completedOnDate = allTasks.filter { it.date == dateToCheck && it.isCompleted }.size
            weeklyTrend.add(completedOnDate)
            trendCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // 4. Monthly Label
        // If productivity rate is very high (>80) label is Disciplined, if average and focus mode used is Focused, otherwise Irregular
        val totalCompletedAll = allTasks.count { it.isCompleted }
        val overallRate = (totalCompletedAll * 100) / allTasks.size
        val hasFocusTime = allTasks.any { it.focusDurationSeconds > 0 }
        
        val label = when {
            overallRate >= 75 -> "Disciplined"
            hasFocusTime && overallRate >= 45 -> "Focused"
            else -> "Irregular"
        }

        // 5. Longest Streak (Consecutive days with at least one completed task)
        val completedDates = allTasks
            .filter { it.isCompleted }
            .map { it.date }
            .distinct()
            .sorted()

        var longestStreak = 0
        var currentStreak = 0
        var previousDate: Date? = null

        val dayInMillis = 24 * 60 * 60 * 1000L

        for (dateStr in completedDates) {
            try {
                val currentDate = sdf.parse(dateStr) ?: continue
                if (previousDate == null) {
                    currentStreak = 1
                } else {
                    val diff = currentDate.time - previousDate.time
                    if (diff <= dayInMillis + 1000L) { // consecutive or same day
                        if (diff > 1000L) { // consecutive day
                            currentStreak++
                        }
                    } else {
                        currentStreak = 1
                    }
                }
                if (currentStreak > longestStreak) {
                    longestStreak = currentStreak
                }
                previousDate = currentDate
            } catch (e: Exception) {
                // ignore
            }
        }

        // 6. Highest Focus Duration
        val maxFocusSeconds = allTasks.maxOfOrNull { it.focusDurationSeconds } ?: 0
        val maxFocusMins = maxFocusSeconds / 60

        return LifeAnalytics(
            completedCount = completedToday,
            missedCount = totalMissed,
            pendingCount = pendingToday,
            productivityScore = score,
            weeklyCompletionRate = weeklyRate,
            mostProductiveDay = mostProd,
            leastProductiveDay = leastProd,
            weeklyTrend = weeklyTrend,
            monthlyPerformanceLabel = label,
            longestStreak = longestStreak,
            highestFocusDurationMinutes = maxFocusMins
        )
    }
}
