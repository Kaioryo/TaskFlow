package com.taskflow.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TasksFragment : Fragment() {

    private lateinit var repository: TaskRepository
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var networkHelper: NetworkHelper

    // Loading views
    private var progressBar: ProgressBar? = null
    private var tvSyncStatus: TextView? = null

    // ✅ SYNC STATE
    private var isSyncing = false
    private var lastSyncTime: Long = 0
    private val MIN_SYNC_INTERVAL = 30_000L // 30 detik
    private var isFirstLoad = true

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

        taskAdapter = TaskAdapter(
            onTaskClick = { task ->
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

        progressBar = view.findViewById(R.id.progressBar)
        tvSyncStatus = view.findViewById(R.id.tv_sync_status)

        // FAB untuk add task
        view.findViewById<FloatingActionButton>(R.id.fab_add_task).setOnClickListener {
            startActivity(Intent(requireContext(), AddTaskActivity::class.java))
        }

        // ✅ NO SYNC BUTTON - Auto sync only!

        // Load tasks
        loadTasks()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("TasksFragment", "✅ onViewCreated - preparing auto sync...")

        viewLifecycleOwner.lifecycleScope.launch {
            delay(1000)

            if (FirebaseManager.isUserLoggedIn()) {
                Log.d("TasksFragment", "✅ User logged in, starting auto sync...")
                autoSyncTasks(forceSync = true)
            } else {
                Log.e("TasksFragment", "❌ User not logged in, skipping sync")
                tvSyncStatus?.text = "⚠️ Not logged in"
            }
        }
    }

    private fun loadTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.allTasks.collect { tasks ->
                taskAdapter.updateTasks(tasks)
                updateSyncStatus(tasks.size)

                if (isFirstLoad && tasks.isEmpty() && networkHelper.isNetworkAvailable()) {
                    Log.d("TasksFragment", "⚠️ No local tasks on first load, triggering sync...")
                    isFirstLoad = false
                    delay(500)
                    autoSyncTasks(forceSync = true)
                }
            }
        }
    }

    private fun autoSyncTasks(forceSync: Boolean = false) {
        if (isSyncing && !forceSync) {
            Log.d("TasksFragment", "⚠️ Sync already in progress, skipping...")
            return
        }

        val currentTime = System.currentTimeMillis()
        if (!forceSync && (currentTime - lastSyncTime) < MIN_SYNC_INTERVAL) {
            val waitTime = (MIN_SYNC_INTERVAL - (currentTime - lastSyncTime)) / 1000
            Log.d("TasksFragment", "⚠️ Sync too soon, skipping... (wait ${waitTime}s)")
            return
        }

        if (!networkHelper.isNetworkAvailable()) {
            tvSyncStatus?.text = "📱 Offline mode"
            Log.d("TasksFragment", "📱 No internet, skipping sync")
            return
        }

        if (!FirebaseManager.isUserLoggedIn()) {
            tvSyncStatus?.text = "⚠️ Not logged in"
            Log.e("TasksFragment", "❌ Not logged in, cannot sync")
            return
        }

        isSyncing = true
        lastSyncTime = currentTime

        progressBar?.visibility = View.VISIBLE
        tvSyncStatus?.text = "🔄 Auto syncing..."
        Log.d("TasksFragment", "🔄 Starting auto sync...")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    Log.d("TasksFragment", "📤 Uploading local tasks to cloud...")
                    val localTasks = repository.allTasksOnce
                    Log.d("TasksFragment", "📊 Found ${localTasks.size} local tasks")

                    for (task in localTasks) {
                        FirebaseManager.syncTaskToCloud(task)
                    }

                    delay(500)

                    Log.d("TasksFragment", "📥 Downloading tasks from cloud...")
                    val cloudTasks = FirebaseManager.getTasksFromCloud()
                    Log.d("TasksFragment", "📊 Found ${cloudTasks.size} cloud tasks")

                    if (cloudTasks.isNotEmpty()) {
                        var addedCount = 0
                        var updatedCount = 0

                        for (cloudTask in cloudTasks) {
                            val localTask = repository.getTaskByIdOnce(cloudTask.id)

                            if (localTask == null) {
                                repository.insertTask(cloudTask)
                                addedCount++
                                Log.d("TasksFragment", "➕ Added task from cloud: ${cloudTask.title}")
                            } else if (localTask.createdAt < cloudTask.createdAt) {
                                repository.updateTask(cloudTask)
                                updatedCount++
                                Log.d("TasksFragment", "🔄 Updated task from cloud: ${cloudTask.title}")
                            }
                        }

                        withContext(Dispatchers.Main) {
                            tvSyncStatus?.text = "✅ Synced (${cloudTasks.size} tasks)"

                            if (addedCount > 0 || updatedCount > 0) {
                                Toast.makeText(
                                    requireContext(),
                                    "✅ Auto synced: +$addedCount ↻$updatedCount",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            Log.d("TasksFragment", "✅ Sync completed: ${cloudTasks.size} tasks")
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            tvSyncStatus?.text = "✅ Auto sync active"
                            Log.d("TasksFragment", "✅ Sync completed (no cloud tasks)")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvSyncStatus?.text = "⚠️ Sync error"
                    Log.e("TasksFragment", "❌ Sync failed: ${e.message}", e)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    progressBar?.visibility = View.GONE
                    isSyncing = false
                }
            }
        }
    }

    private fun updateSyncStatus(taskCount: Int) {
        if (networkHelper.isNetworkAvailable()) {
            tvSyncStatus?.text = "✅ Synced ($taskCount tasks)"
        } else {
            tvSyncStatus?.text = "📱 Offline ($taskCount tasks)"
        }
    }

    private fun deleteTask(task: Task) {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.deleteTask(task)

            if (networkHelper.isNetworkAvailable()) {
                withContext(Dispatchers.IO) {
                    FirebaseManager.deleteTaskFromCloud(task.id)
                }
                Toast.makeText(requireContext(), "🗑️ Deleted & synced", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "🗑️ Deleted locally", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun markTaskAsComplete(task: Task) {
        viewLifecycleOwner.lifecycleScope.launch {
            val completedTask = task.copy(isCompleted = !task.isCompleted)
            repository.updateTask(completedTask)

            if (networkHelper.isNetworkAvailable()) {
                withContext(Dispatchers.IO) {
                    FirebaseManager.syncTaskToCloud(completedTask)
                }
            }

            val statusText = if (completedTask.isCompleted) "✅ Completed!" else "🔄 Reopened"
            Toast.makeText(requireContext(), statusText, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("TasksFragment", "🔄 Fragment resumed...")

        if (!isFirstLoad) {
            viewLifecycleOwner.lifecycleScope.launch {
                delay(500)
                autoSyncTasks()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressBar = null
        tvSyncStatus = null
    }
}
