package com.taskflow.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class TasksFragment : Fragment() {

    private lateinit var rvTasks: RecyclerView
    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var tvDate: TextView
    private lateinit var repository: TaskRepository
    private lateinit var taskAdapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            Log.d("TasksFragment", "onCreateView started")
            inflater.inflate(R.layout.fragment_tasks, container, false)
        } catch (e: Exception) {
            Log.e("TasksFragment", "Error inflating layout: ${e.message}")
            Toast.makeText(requireContext(), "Error loading tasks", Toast.LENGTH_SHORT).show()
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            Log.d("TasksFragment", "onViewCreated started")

            // Initialize views
            rvTasks = view.findViewById(R.id.rv_tasks)
            fabAddTask = view.findViewById(R.id.fab_add_task)
            tvDate = view.findViewById(R.id.tv_date)

            Log.d("TasksFragment", "Views initialized")

            // Initialize database
            val database = AppDatabase.getDatabase(requireContext())
            repository = TaskRepository(database.taskDao())

            Log.d("TasksFragment", "Database initialized")

            // Set current date
            tvDate.text = DateTimeHelper.getCurrentDate().replace("-", "\n")

            setupRecyclerView()
            loadTasksFromDatabase()

            fabAddTask.setOnClickListener {
                startActivity(Intent(requireContext(), AddTaskActivity::class.java))
            }

            Log.d("TasksFragment", "Setup completed")

        } catch (e: Exception) {
            Log.e("TasksFragment", "Error in onViewCreated: ${e.message}", e)
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupRecyclerView() {
        // ✅ PASS ALL 4 REQUIRED PARAMETERS
        taskAdapter = TaskAdapter(
            tasks = emptyList(),
            onTaskClick = { task ->
                // Edit task
                val intent = Intent(requireContext(), AddTaskActivity::class.java)
                intent.putExtra("EDIT_TASK_ID", task.id)
                startActivity(intent)
            },
            onTaskDelete = { task ->
                // Delete task with confirmation
                showDeleteConfirmation(task)
            },
            onTaskToggle = { task ->
                // Toggle completion status
                lifecycleScope.launch {
                    val updatedTask = task.copy(isCompleted = !task.isCompleted)
                    repository.updateTask(updatedTask)
                    Toast.makeText(
                        requireContext(),
                        if (updatedTask.isCompleted) "✅ Task completed!" else "⏳ Task reopened",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        rvTasks.layoutManager = LinearLayoutManager(requireContext())
        rvTasks.adapter = taskAdapter

        Log.d("TasksFragment", "RecyclerView setup completed")
    }

    private fun loadTasksFromDatabase() {
        try {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    Log.d("TasksFragment", "Loading tasks from database...")
                    repository.incompleteTasks.collect { tasks ->
                        Log.d("TasksFragment", "Tasks loaded: ${tasks.size} items")
                        taskAdapter.updateTasks(tasks)
                    }
                } catch (e: Exception) {
                    Log.e("TasksFragment", "Error loading tasks: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("TasksFragment", "Error in loadTasksFromDatabase: ${e.message}", e)
        }
    }

    private fun showDeleteConfirmation(task: Task) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete \"${task.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    repository.deleteTask(task)
                    Toast.makeText(requireContext(), "🗑️ Task deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        try {
            loadTasksFromDatabase()
        } catch (e: Exception) {
            Log.e("TasksFragment", "Error in onResume: ${e.message}", e)
        }
    }
}
