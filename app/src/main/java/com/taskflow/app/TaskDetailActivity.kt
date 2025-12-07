package com.taskflow.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var repository: TaskRepository
    private var taskId: Int = -1

    companion object {
        private const val TAG = "TaskDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate started")

        try {
            setContentView(R.layout.activity_task_detail)

            // Initialize database
            val database = AppDatabase.getDatabase(this)
            repository = TaskRepository(database.taskDao())

            // Get task ID from intent
            taskId = intent.getIntExtra("TASK_ID", -1)
            Log.d(TAG, "Received taskId: $taskId")

            if (taskId == -1) {
                Log.e(TAG, "Invalid task ID!")
                Toast.makeText(this, "Error: Invalid task ID", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Views
            val tvTitle = findViewById<TextView>(R.id.tv_detail_title)
            val tvDesc = findViewById<TextView>(R.id.tv_detail_desc)
            val tvLocation = findViewById<TextView>(R.id.tv_detail_location)
            val tvTime = findViewById<TextView>(R.id.tv_detail_time)
            val btnDelete = findViewById<Button>(R.id.btn_delete)
            val btnEdit = findViewById<Button>(R.id.btn_edit)
            val btnBack = findViewById<ImageButton>(R.id.btn_back)

            Log.d(TAG, "Views initialized")

            // Load task from database
            loadTaskDetails(tvTitle, tvDesc, tvLocation, tvTime)

            // Delete button
            btnDelete.setOnClickListener {
                Log.d(TAG, "Delete button clicked for taskId: $taskId")
                deleteTask()
            }

            // Edit button
            btnEdit.setOnClickListener {
                Log.d(TAG, "Edit button clicked for taskId: $taskId")
                editTask()
            }

            btnBack.setOnClickListener {
                finish()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error loading task: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadTaskDetails(
        tvTitle: TextView,
        tvDesc: TextView,
        tvLocation: TextView,
        tvTime: TextView
    ) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Loading task details for ID: $taskId")

                val task = repository.getTaskById(taskId)

                if (task != null) {
                    Log.d(TAG, "Task found: ${task.title}")

                    tvTitle.text = task.title
                    tvDesc.text = task.description
                    tvLocation.text = task.location
                    tvTime.text = "${task.dueDate} ${task.dueTime}"

                    Log.d(TAG, "Task details loaded successfully")
                } else {
                    Log.e(TAG, "Task not found in database for ID: $taskId")
                    Toast.makeText(
                        this@TaskDetailActivity,
                        "Task not found",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading task: ${e.message}", e)
                Toast.makeText(
                    this@TaskDetailActivity,
                    "Error loading task: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun deleteTask() {
        if (taskId == -1) {
            Log.e(TAG, "Cannot delete: Invalid task ID")
            return
        }

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Attempting to delete task ID: $taskId")

                val task = repository.getTaskById(taskId)

                if (task != null) {
                    Log.d(TAG, "Task found, deleting: ${task.title}")

                    repository.deleteTask(task)

                    // Cancel reminder if set
                    if (task.reminderSet) {
                        Log.d(TAG, "Canceling reminder for task ID: $taskId")
                        ReminderHelper.cancelReminder(this@TaskDetailActivity, taskId)
                    }

                    Toast.makeText(
                        this@TaskDetailActivity,
                        "✅ Task deleted!",
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.d(TAG, "Task deleted successfully")
                    finish()
                } else {
                    Log.e(TAG, "Task not found for deletion")
                    Toast.makeText(
                        this@TaskDetailActivity,
                        "Task not found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting task: ${e.message}", e)
                Toast.makeText(
                    this@TaskDetailActivity,
                    "Error deleting task: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun editTask() {
        if (taskId == -1) {
            Log.e(TAG, "Cannot edit: Invalid task ID")
            Toast.makeText(this, "Error: Invalid task ID", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            Log.d(TAG, "Launching AddTaskActivity in edit mode for task ID: $taskId")

            val intent = Intent(this, AddTaskActivity::class.java)
            intent.putExtra("EDIT_TASK_ID", taskId)
            startActivity(intent)
            finish()

            Log.d(TAG, "Edit intent launched successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error launching edit: ${e.message}", e)
            Toast.makeText(this, "Error opening edit: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
    }
}
