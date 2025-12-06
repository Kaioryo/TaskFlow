package com.taskflow.app

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY due_date ASC, due_time ASC")
    fun getAllTasks(): Flow<List<Task>>

    // ✅ NEW: Get all tasks once (for sync)
    @Query("SELECT * FROM tasks ORDER BY due_date ASC, due_time ASC")
    fun getAllTasksOnce(): List<Task>

    @Query("SELECT * FROM tasks WHERE is_completed = 0 ORDER BY due_date ASC, due_time ASC")
    fun getIncompleteTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskById(id: Int): Task?

    // ✅ NEW: Get task by ID once (synchronous)
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskByIdOnce(id: Int): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)
}
