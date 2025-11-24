package com.taskflow.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var repository: TaskRepository
    private var taskId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail)

        // Initialize database
        val database = AppDatabase.getDatabase(this)
        repository = TaskRepository(database.taskDao())

        // Get task ID from intent
        taskId = intent.getIntExtra("TASK_ID", -1)

        // Views
        val tvTitle = findViewById<TextView>(R.id.tv_detail_title)
        val tvDesc = findViewById<TextView>(R.id.tv_detail_desc)
        val tvLocation = findViewById<TextView>(R.id.tv_detail_location)
        val tvTime = findViewById<TextView>(R.id.tv_detail_time)
        val btnDelete = findViewById<Button>(R.id.btn_delete)
        val btnEdit = findViewById<Button>(R.id.btn_edit)

        // Load task from database
        if (taskId != -1) {
            lifecycleScope.launch {
                val task = repository.getTaskById(taskId)
                if (task != null) {
                    tvTitle.text = task.title
                    tvDesc.text = task.description
                    tvLocation.text = task.location
                    tvTime.text = task.dueTime
                } else {
                    Toast.makeText(this@TaskDetailActivity, "Task not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        // Delete button - HAPUS DARI DATABASE
        btnDelete.setOnClickListener {
            if (taskId != -1) {
                lifecycleScope.launch {
                    val task = repository.getTaskById(taskId)
                    if (task != null) {
                        repository.deleteTask(task)
                        Toast.makeText(this@TaskDetailActivity, "Task deleted!", Toast.LENGTH_SHORT).show()
                        finish() // Kembali ke TasksFragment
                    }
                }
            }
        }

        btnEdit.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java)
            intent.putExtra("EDIT_TASK_ID", taskId)
            startActivity(intent)
            finish()
        }
    }
}
