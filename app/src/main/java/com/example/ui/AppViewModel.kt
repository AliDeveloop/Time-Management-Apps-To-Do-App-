package com.example.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.GeminiParser
import com.example.data.Task
import com.example.data.TaskRepository
import com.example.ui.theme.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AppViewModel(
    application: Application,
    private val repository: TaskRepository
) : AndroidViewModel(application) {

    // Language settings
    var currentLanguage by mutableStateOf(AppLanguage.EN)
        private set

    // Auto-reshuffle settings
    var isAutoReshuffleEnabled by mutableStateOf(true)
        private set

    // Notification Style
    var notificationStyle by mutableStateOf("FRIENDLY")
        private set

    // Current Date filter
    private val _currentDate = MutableStateFlow(getTodayDateString())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    // Sync Status
    var syncStatus by mutableStateOf("")
        private set

    // Active Focus Mode variables
    var activeFocusTask by mutableStateOf<Task?>(null)
        private set
    var focusTimerSeconds by mutableIntStateOf(0)
        private set
    var isFocusTimerRunning by mutableStateOf(false)
        private set

    // Parsing Status
    var isParsing by mutableStateOf(false)
        private set

    // Observed Tasks list for the currently selected date
    val tasksForDate: StateFlow<List<Task>> = _currentDate
        .flatMapLatest { date -> repository.getTasksForDate(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All Tasks (for Analytics)
    val allTasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Alert Notification State (for immersive in-app reminder testing)
    var activeNotificationMsg by mutableStateOf<String?>(null)
        private set

    init {
        // Start simulated timer/reminder ticker
        startNotificationTicker()
    }

    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
    }

    fun setAutoReshuffle(enabled: Boolean) {
        isAutoReshuffleEnabled = enabled
    }

    fun updateNotificationStyle(style: String) {
        notificationStyle = style
    }

    fun setDate(date: String) {
        _currentDate.value = date
    }

    // Task actions
    fun addTask(title: String, time: String, date: String, energyTag: String, isRepeat: Boolean, repeatFrequency: String) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                time = time,
                date = date,
                energyTag = energyTag,
                isRepeat = isRepeat,
                repeatFrequency = repeatFrequency,
                notificationStyle = notificationStyle
            )
            repository.insertTask(task)
        }
    }

    /**
     * Parse natural language task with local fallback and Gemini
     */
    fun addSmartTask(inputText: String) {
        viewModelScope.launch {
            isParsing = true
            try {
                val parsedList = GeminiParser.parseTask(inputText, _currentDate.value)
                for (parsed in parsedList) {
                    val task = Task(
                        title = parsed.title,
                        time = parsed.time,
                        date = parsed.date,
                        energyTag = parsed.energyTag,
                        isRepeat = parsed.isRepeat,
                        repeatFrequency = parsed.repeatFrequency,
                        notificationStyle = notificationStyle
                    )
                    repository.insertTask(task)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error parsing smart task", e)
            } finally {
                isParsing = false
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            repository.updateTask(updated)
            
            // If completed in focus mode, record focus duration
            if (activeFocusTask?.id == task.id && updated.isCompleted) {
                completeFocusMode()
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
            if (activeFocusTask?.id == task.id) {
                exitFocusMode()
            }
        }
    }

    // Auto Reshuffle Algorithm
    fun triggerReshuffle() {
        viewModelScope.launch {
            val dateStr = _currentDate.value
            val currentTasks = tasksForDate.value
            if (currentTasks.isEmpty()) return@launch

            val (completed, unfinished) = currentTasks.partition { it.isCompleted }
            if (unfinished.isEmpty()) return@launch

            // Get busy times
            val busyTimes = completed.map { it.time }.toSet()

            // Sort unfinished tasks
            val sortedUnfinished = unfinished.sortedBy { it.time }

            // Find best available time slot starting from earliest slot or current hour
            val sdf = SimpleDateFormat("HH:mm", Locale.US)
            val calendar = Calendar.getInstance()
            val startHour = calendar.get(Calendar.HOUR_OF_DAY)

            var allocatedHour = if (startHour < 22) startHour + 1 else 9
            var allocatedMin = 0

            for (task in sortedUnfinished) {
                // Find next free time
                var timeSlotFound = false
                while (!timeSlotFound && allocatedHour < 24) {
                    val candidateTime = String.format(Locale.US, "%02d:%02d", allocatedHour, allocatedMin)
                    if (!busyTimes.contains(candidateTime)) {
                        val updatedTask = task.copy(
                            time = candidateTime,
                            isReshuffled = true
                        )
                        repository.updateTask(updatedTask)
                        timeSlotFound = true
                    }
                    allocatedHour += 1 // check next hour
                }
            }
            showTemporaryStatus("reshuffle_success")
        }
    }

    // Focus Mode Support
    fun startFocusMode(task: Task) {
        activeFocusTask = task
        focusTimerSeconds = 0
        isFocusTimerRunning = true
        
        viewModelScope.launch {
            while (isFocusTimerRunning && activeFocusTask?.id == task.id) {
                delay(1000)
                focusTimerSeconds++
            }
        }
    }

    fun exitFocusMode() {
        isFocusTimerRunning = false
        activeFocusTask = null
        focusTimerSeconds = 0
    }

    private fun completeFocusMode() {
        val task = activeFocusTask ?: return
        viewModelScope.launch {
            val updated = task.copy(
                isCompleted = true,
                focusDurationSeconds = focusTimerSeconds
            )
            repository.updateTask(updated)
            exitFocusMode()
        }
    }

    // Backup & Restore Support (Local JSON backup acting as Google Drive Backup abstraction)
    fun backupToLocalDrive() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tasks = allTasks.value
                val rootArray = JSONArray()
                for (task in tasks) {
                    val obj = JSONObject().apply {
                        put("title", task.title)
                        put("time", task.time)
                        put("date", task.date)
                        put("isCompleted", task.isCompleted)
                        put("isRepeat", task.isRepeat)
                        put("repeatFrequency", task.repeatFrequency)
                        put("energyTag", task.energyTag)
                        put("reminderMinutesBefore", task.reminderMinutesBefore)
                        put("notificationStyle", task.notificationStyle)
                        put("isReshuffled", task.isReshuffled)
                        put("focusDurationSeconds", task.focusDurationSeconds)
                    }
                    rootArray.put(obj)
                }

                val backupFile = File(getApplication<Application>().filesDir, "roozara_backup.json")
                backupFile.writeText(rootArray.toString())
                
                withContext(Dispatchers.Main) {
                    syncStatus = "sync_status_success"
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Backup failed", e)
                withContext(Dispatchers.Main) {
                    syncStatus = "Error: ${e.localizedMessage}"
                }
            }
        }
    }

    fun restoreFromLocalDrive() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val backupFile = File(getApplication<Application>().filesDir, "roozara_backup.json")
                if (!backupFile.exists()) {
                    withContext(Dispatchers.Main) {
                        syncStatus = "No backup file found."
                    }
                    return@launch
                }

                val backupContent = backupFile.readText()
                val rootArray = JSONArray(backupContent)
                val restoredTasks = mutableListOf<Task>()

                for (i in 0 until rootArray.length()) {
                    val obj = rootArray.getJSONObject(i)
                    restoredTasks.add(
                        Task(
                            title = obj.getString("title"),
                            time = obj.getString("time"),
                            date = obj.getString("date"),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            isRepeat = obj.optBoolean("isRepeat", false),
                            repeatFrequency = obj.optString("repeatFrequency", "NONE"),
                            energyTag = obj.optString("energyTag", "MEDIUM"),
                            reminderMinutesBefore = obj.optInt("reminderMinutesBefore", 30),
                            notificationStyle = obj.optString("notificationStyle", "FRIENDLY"),
                            isReshuffled = obj.optBoolean("isReshuffled", false),
                            focusDurationSeconds = obj.optInt("focusDurationSeconds", 0)
                        )
                    )
                }

                repository.clearAllTasks()
                repository.insertTasks(restoredTasks)

                withContext(Dispatchers.Main) {
                    syncStatus = "sync_status_restored"
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Restore failed", e)
                withContext(Dispatchers.Main) {
                    syncStatus = "Error: ${e.localizedMessage}"
                }
            }
        }
    }

    private fun showTemporaryStatus(statusKey: String) {
        viewModelScope.launch {
            syncStatus = statusKey
            delay(4000)
            syncStatus = ""
        }
    }

    fun clearNotification() {
        activeNotificationMsg = null
    }

    // Reminder Ticker (Checks if any task is within 30 minutes of current system time)
    private fun startNotificationTicker() {
        viewModelScope.launch {
            while (true) {
                delay(15000) // check every 15 seconds
                
                val todayTasks = tasksForDate.value
                val sdf = SimpleDateFormat("HH:mm", Locale.US)
                val currentTime = sdf.format(Date())

                val currentParts = currentTime.split(":")
                val currentMinutes = currentParts[0].toInt() * 60 + currentParts[1].toInt()

                for (task in todayTasks) {
                    if (task.isCompleted) continue
                    
                    val taskParts = task.time.split(":")
                    if (taskParts.size == 2) {
                        val taskMinutes = taskParts[0].toInt() * 60 + taskParts[1].toInt()
                        val diff = taskMinutes - currentMinutes
                        
                        // If upcoming within 30 minutes exactly or roughly (between 0 and 30 mins)
                        if (diff in 0..30) {
                            val alertMsg = when (task.notificationStyle) {
                                "MOTIVATIONAL" -> "⚡ RoozAra: Prepare! \"${task.title}\" is scheduled at ${task.time}. Fuel your passion, make today count!"
                                "SERIOUS" -> "🎯 RoozAra: Attention! \"${task.title}\" starts in $diff minutes (at ${task.time}). Prepare and execute. No excuses."
                                else -> "😊 RoozAra: Friendly heads up! \"${task.title}\" is coming up at ${task.time}. You got this!"
                            }
                            activeNotificationMsg = alertMsg
                            break // only show one alert at a time to prevent overlay clutter
                        }
                    }
                }
            }
        }
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    // Factory Class
    class Factory(
        private val application: Application,
        private val repository: TaskRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
                return AppViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
