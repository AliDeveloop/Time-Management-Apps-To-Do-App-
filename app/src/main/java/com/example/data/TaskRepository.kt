package com.example.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    fun getTasksForDate(date: String): Flow<List<Task>> = taskDao.getTasksForDate(date)

    fun getTaskById(id: Int): Flow<Task?> = taskDao.getTaskById(id)

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun insertTasks(tasks: List<Task>) = taskDao.insertTasks(tasks)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun clearAllTasks() = taskDao.clearAllTasks()
}
