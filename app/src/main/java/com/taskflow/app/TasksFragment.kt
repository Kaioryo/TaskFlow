package com.taskflow.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class TasksFragment : Fragment() {

    private lateinit var repository: TaskRepository
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var networkHelper: NetworkHelper

    // Loading views
    private var progressBar: ProgressBar? = null
    private var tvSyncStatus: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)

        // Initialize
        val database = AppDatabase.getDatabase(requireContext())
        repository = TaskRepository(database.taskDao())
        networkHelper = NetworkHelper(requireContext())

        // Setup RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_tasks)

        // ✅ INITIALIZE ADAPTER dengan constructor yang benar
        taskAdapter = TaskAdapter(
            tasks = emptyList(),
            onTaskClick = { task ->
                // Navigate to detail or edit
                val intent = Intent(requireContext(), AddTaskActivity::class.java)
                intent.putExtra("EDIT_TASK_ID", task.id)
                startActivity(intent)
            },
            onTaskDelete = { task ->
                deleteTask(task)
            },
            onTaskToggle = { task ->
                markTaskAsComplete(task)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = taskAdapter

        // ✅ LOADING VIEWS (optional - jika ada di layout)
        progressBar = view.findViewById(R.id.progressBar)
        tvSyncStatus = view.findViewById(R.id.tv_sync_status)

        // FAB untuk add task
        view.findViewById<FloatingActionButton>(R.id.fab_add_task).setOnClickListener {
            startActivity(Intent(requireContext(), AddTaskActivity::class.java))
        }

        // ✅ SYNC BUTTON (optional - jika ada di layout)
        view.findViewById<View>(R.id.btn_sync)?.setOnClickListener {
            syncTasksFromCloud()
        }

        // Load tasks
        loadTasks()

        return view
    }

    private fun loadTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.allTasks.collect { tasks ->
                // ✅ GUNAKAN updateTasks() bukan submitList()
                taskAdapter.updateTasks(tasks)

                // Update sync status
                if (networkHelper.isNetworkAvailable()) {
                    tvSyncStatus?.text = "✅ Synced"
                } else {
                    tvSyncStatus?.text = "📱 Offline"
                }
            }
        }
    }

    // ✅ SYNC FROM CLOUD
    private fun syncTasksFromCloud() {
        if (!networkHelper.isNetworkAvailable()) {
            Toast.makeText(requireContext(), "❌ No internet connection", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar?.visibility = View.VISIBLE
        tvSyncStatus?.text = "🔄 Syncing..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val cloudTasks = FirebaseManager.getTasksFromCloud()

                // Merge with local tasks
                for (task in cloudTasks) {
                    repository.insertTask(task)
                }

                Toast.makeText(
                    requireContext(),
                    "✅ Synced ${cloudTasks.size} tasks from cloud",
                    Toast.LENGTH_SHORT
                ).show()

                tvSyncStatus?.text = "✅ Synced"
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "❌ Sync failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                tvSyncStatus?.text = "❌ Sync failed"
            } finally {
                progressBar?.visibility = View.GONE
            }
        }
    }

    private fun deleteTask(task: Task) {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.deleteTask(task)

            // ✅ DELETE FROM CLOUD
            if (networkHelper.isNetworkAvailable()) {
                FirebaseManager.deleteTaskFromCloud(task.id)
                Toast.makeText(requireContext(), "☁️ Deleted from cloud", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "📱 Deleted locally. Will sync when online.", Toast.LENGTH_SHORT).show()
            }

            Toast.makeText(requireContext(), "🗑️ Task deleted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun markTaskAsComplete(task: Task) {
        viewLifecycleOwner.lifecycleScope.launch {
            val completedTask = task.copy(isCompleted = !task.isCompleted)
            repository.updateTask(completedTask)

            // ✅ SYNC TO CLOUD
            if (networkHelper.isNetworkAvailable()) {
                FirebaseManager.syncTaskToCloud(completedTask)
            }

            val statusText = if (completedTask.isCompleted) "✅ Task completed!" else "🔄 Task reopened"
            Toast.makeText(requireContext(), statusText, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressBar = null
        tvSyncStatus = null
    }
}
