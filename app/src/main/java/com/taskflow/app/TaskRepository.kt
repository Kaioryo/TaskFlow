package com.taskflow.app

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val incompleteTasks: Flow<List<Task>> = taskDao.getIncompleteTasks()

    // ✅ Get all tasks once (for sync)
    val allTasksOnce: List<Task>
        get() = taskDao.getAllTasksOnce()

    suspend fun insertTask(task: Task): Long {
        return taskDao.insertTask(task)
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }

    suspend fun getTaskById(id: Int): Task? {
        return taskDao.getTaskById(id)
    }

    // ✅ Get task by ID once (synchronous)
    suspend fun getTaskByIdOnce(id: Int): Task? {
        return taskDao.getTaskByIdOnce(id)
    }

    // ✅ NEW: Delete all tasks (for logout)
    suspend fun deleteAllTasks() {
        taskDao.deleteAllTasks()
    }
}
