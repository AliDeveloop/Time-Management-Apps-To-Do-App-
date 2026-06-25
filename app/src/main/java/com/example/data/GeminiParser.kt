package com.example.data

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object GeminiParser {
    private const val TAG = "GeminiParser"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Parses a natural language task description.
     * Tries the Gemini API first, then falls back to local parsing.
     */
    suspend fun parseTask(input: String, todayDate: String): List<ParsedTask> {
        if (input.isBlank()) return emptyList()

        // 1. Try Gemini API
        val geminiResult = parseWithGemini(input, todayDate)
        if (geminiResult != null && geminiResult.isNotEmpty()) {
            return geminiResult
        }

        // 2. Fallback to Local Parsing
        Log.d(TAG, "Gemini failed or API key missing. Falling back to local parsing.")
        return listOf(parseLocally(input, todayDate))
    }

    private fun parseWithGemini(input: String, todayDate: String): List<ParsedTask>? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
            Log.w(TAG, "Gemini API key is placeholder or empty.")
            return null
        }

        try {
            val systemInstruction = """
                You are RoozAra's smart NLP daily assistant. Parse the user's input task descriptions into a structured JSON list of tasks.
                Each parsed task MUST have:
                1. 'title': string (e.g. "Gym", "Study", "Meeting")
                2. 'time': string in HH:mm format (e.g. "17:00", "08:30"). If no time is specified, default to a sensible time based on context or "09:00".
                3. 'date': string in yyyy-MM-dd format. The today reference is $todayDate. If a day of the week is specified, resolve it to the correct date.
                4. 'energyTag': string, one of "LOW", "MEDIUM", "HIGH". Suggest based on activity.
                5. 'isRepeat': boolean. True if repeating daily/weekly.
                6. 'repeatFrequency': string, one of "NONE", "DAILY", "WEEKLY".
                
                Input text: "$input"
                
                Respond ONLY with a JSON object in this format:
                {
                  "tasks": [
                    { "title": "Gym", "time": "17:00", "date": "2026-06-25", "energyTag": "HIGH", "isRepeat": false, "repeatFrequency": "NONE" }
                  ]
                }
                Do not include any markdown format tags like ```json.
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", systemInstruction)
                            })
                        })
                    })
                })
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "API call failed: ${response.code} ${response.message}")
                    return null
                }
                val responseString = response.body?.string() ?: return null
                Log.d(TAG, "Gemini raw response: $responseString")

                val jsonResponse = JSONObject(responseString)
                val text = jsonResponse.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()

                // Clean response in case markdown tags are returned
                val cleanedText = text.replace("```json", "").replace("```", "").trim()
                val parsedRoot = JSONObject(cleanedText)
                val tasksArray = parsedRoot.getJSONArray("tasks")
                val result = mutableListOf<ParsedTask>()
                for (i in 0 until tasksArray.length()) {
                    val taskObj = tasksArray.getJSONObject(i)
                    result.add(
                        ParsedTask(
                            title = taskObj.optString("title", "Task"),
                            time = taskObj.optString("time", "09:00"),
                            date = taskObj.optString("date", todayDate),
                            energyTag = taskObj.optString("energyTag", "MEDIUM"),
                            isRepeat = taskObj.optBoolean("isRepeat", false),
                            repeatFrequency = taskObj.optString("repeatFrequency", "NONE")
                        )
                    )
                }
                return result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in parseWithGemini: ${e.message}", e)
            return null
        }
    }

    /**
     * Highly responsive offline regex parsing.
     */
    fun parseLocally(input: String, todayDate: String): ParsedTask {
        var title = input.trim()
        var time = "09:00"
        var date = todayDate
        var energyTag = "MEDIUM"
        var isRepeat = false
        var repeatFrequency = "NONE"

        // Lowercase for easier matching
        val lower = input.lowercase()

        // 1. Detect Repeat Frequency
        if (lower.contains("every day") || lower.contains("daily") || lower.contains("هر روز")) {
            isRepeat = true
            repeatFrequency = "DAILY"
        } else if (lower.contains("every week") || lower.contains("weekly") || lower.contains("هفتگی")) {
            isRepeat = true
            repeatFrequency = "WEEKLY"
        }

        // 2. Detect Energy Tag
        if (lower.contains("gym") || lower.contains("workout") || lower.contains("run") || lower.contains("high energy") || lower.contains("ورزش")) {
            energyTag = "HIGH"
        } else if (lower.contains("sleep") || lower.contains("meditate") || lower.contains("chill") || lower.contains("low energy") || lower.contains("مدیتیشن")) {
            energyTag = "LOW"
        }

        // 3. Extract time (e.g. 5 PM, 5:30, at 17:00, ساعت ۵)
        val timePattern = Pattern.compile("(\\b(?:at|at:)\\s*)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b", Pattern.CASE_INSENSITIVE)
        val matcher = timePattern.matcher(input)
        if (matcher.find()) {
            val hourStr = matcher.group(2)
            val minStr = matcher.group(3) ?: "00"
            val amPm = matcher.group(4)

            var hour = hourStr?.toIntOrNull() ?: 9
            val min = minStr.toIntOrNull() ?: 0

            if (amPm != null) {
                if (amPm.equals("pm", ignoreCase = true) && hour < 12) hour += 12
                if (amPm.equals("am", ignoreCase = true) && hour == 12) hour = 0
            }

            time = String.format(Locale.US, "%02d:%02d", hour, min)

            // Strip the time portion from the title
            val matchedText = matcher.group(0)
            if (matchedText != null) {
                title = title.replace(matchedText, "").trim()
            }
        }

        // 4. Extract date (e.g., tomorrow, monday, tuesday, etc.)
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        if (lower.contains("tomorrow") || lower.contains("فردا")) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            date = sdf.format(calendar.time)
            title = title.replace("tomorrow", "", ignoreCase = true)
                .replace("فردا", "").trim()
        } else if (lower.contains("next week") || lower.contains("هفته بعد")) {
            calendar.add(Calendar.DAY_OF_YEAR, 7)
            date = sdf.format(calendar.time)
        } else {
            // Check for week days: monday, tuesday, etc.
            val days = listOf("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday")
            for ((idx, day) in days.withIndex()) {
                if (lower.contains(day)) {
                    val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    // Calendar.SUNDAY is 1, Calendar.SATURDAY is 7
                    val targetDayOfWeek = idx + 1
                    var daysDiff = targetDayOfWeek - currentDayOfWeek
                    if (daysDiff <= 0) {
                        daysDiff += 7 // Next week's weekday
                    }
                    calendar.add(Calendar.DAY_OF_YEAR, daysDiff)
                    date = sdf.format(calendar.time)
                    title = title.replace(day, "", ignoreCase = true).trim()
                    break
                }
            }
        }

        // Clean up prepositions and remaining fluff
        title = title.replace(Regex("\\b(at|on|for|in)\\b", RegexOption.IGNORE_CASE), "").trim()
        title = title.replace(Regex("\\s+"), " ") // normalize spacing
        if (title.isBlank()) title = "Task"

        return ParsedTask(
            title = title,
            time = time,
            date = date,
            energyTag = energyTag,
            isRepeat = isRepeat,
            repeatFrequency = repeatFrequency
        )
    }

    data class ParsedTask(
        val title: String,
        val time: String,
        val date: String,
        val energyTag: String,
        val isRepeat: Boolean,
        val repeatFrequency: String
    )
}
