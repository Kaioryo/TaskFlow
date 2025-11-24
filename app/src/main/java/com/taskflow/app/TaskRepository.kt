package com.taskflow.app

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    // Get all tasks as Flow (real-time updates)
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    // Get incomplete tasks
    val incompleteTasks: Flow<List<Task>> = taskDao.getIncompleteTasks()

    // Insert task
    suspend fun insertTask(task: Task): Long {
        return taskDao.insertTask(task)
    }

    // Update task
    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    // Delete task
    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }

    // Get task by ID (suspend version)
    suspend fun getTaskById(id: Int): Task? {
        return taskDao.getTaskByIdOnce(id)
    }

    suspend fun getTaskByIdOnce(id: Int): Task? {
        return taskDao.getTaskByIdOnce(id)
    }

    // Mark as completed
    suspend fun markAsCompleted(id: Int) {
        taskDao.markAsCompleted(id)
    }

    // Get tasks by priority
    fun getTasksByPriority(priority: String): Flow<List<Task>> {
        return taskDao.getTasksByPriority(priority)
    }
}
