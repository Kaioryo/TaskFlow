package com.taskflow.app

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // INSERT - Tambah task baru
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    // UPDATE - Edit task
    @Update
    suspend fun updateTask(task: Task)

    // DELETE - Hapus task
    @Delete
    suspend fun deleteTask(task: Task)

    // READ - Ambil semua tasks (sorted by due_time)
    @Query("SELECT * FROM tasks ORDER BY due_time ASC")
    fun getAllTasks(): Flow<List<Task>>

    // READ - Ambil tasks by priority
    @Query("SELECT * FROM tasks WHERE priority = :priority ORDER BY due_time ASC")
    fun getTasksByPriority(priority: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskByIdOnce(taskId: Int): Task?

    // READ - Ambil incomplete tasks only
    @Query("SELECT * FROM tasks WHERE is_completed = 0 ORDER BY due_time ASC")
    fun getIncompleteTasks(): Flow<List<Task>>

    // UPDATE - Mark as completed
    @Query("UPDATE tasks SET is_completed = 1 WHERE id = :taskId")
    suspend fun markAsCompleted(taskId: Int)

    // DELETE - Hapus semua tasks
    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}
