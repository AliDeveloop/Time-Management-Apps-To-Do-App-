package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val time: String, // HH:mm format
    val date: String, // yyyy-MM-dd format
    val isCompleted: Boolean = false,
    val isRepeat: Boolean = false,
    val repeatFrequency: String = "NONE", // NONE, DAILY, WEEKLY
    val energyTag: String = "MEDIUM", // LOW, MEDIUM, HIGH
    val reminderMinutesBefore: Int = 30,
    val notificationStyle: String = "FRIENDLY", // FRIENDLY, MOTIVATIONAL, SERIOUS
    val isReshuffled: Boolean = false,
    val focusDurationSeconds: Int = 0
)
