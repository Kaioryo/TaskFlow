package com.taskflow.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AddTaskActivity : AppCompatActivity() {

    private lateinit var repository: TaskRepository
    private lateinit var networkHelper: NetworkHelper
    private var selectedPriority = "medium"
    private var selectedDate = "24-01-2024"
    private var selectedTime = "23:59"
    private var selectedReminderOffset: Long = 0
    private var editTaskId: Int = -1

    // Loading state
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSubmit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        val database = AppDatabase.getDatabase(this)
        repository = TaskRepository(database.taskDao())
        networkHelper = NetworkHelper(this)

        // Views
        val etTitle = findViewById<TextInputEditText>(R.id.et_title)
        val etDescription = findViewById<TextInputEditText>(R.id.et_description)
        val etSubmission = findViewById<TextInputEditText>(R.id.et_submission)
        val tvSetReminder = findViewById<TextView>(R.id.tv_set_reminder)
        val cardDatePicker = findViewById<CardView>(R.id.card_date_picker)
        val cardTimePicker = findViewById<CardView>(R.id.card_time_picker)
        val tvSelectedDate = findViewById<TextView>(R.id.tv_selected_date)
        val tvSelectedTime = findViewById<TextView>(R.id.tv_selected_time)
        val tvTimeDisplay = findViewById<TextView>(R.id.tv_time_display)
        val rgPriority = findViewById<android.widget.RadioGroup>(R.id.rg_priority)
        btnSubmit = findViewById(R.id.btn_submit)

        // ✅ PROGRESS BAR (tambahkan ini di layout XML juga)
        // progressBar = findViewById(R.id.progressBar)
        // Atau buat programmatically jika belum ada di layout

        // ✅ SET CURRENT DATE & TIME AS DEFAULT
        selectedDate = DateTimeHelper.getCurrentDate()
        selectedTime = DateTimeHelper.getCurrentTime()

        // Initialize displays
        tvSelectedDate.text = selectedDate
        tvSelectedTime.text = selectedTime
        tvTimeDisplay.text = selectedTime

        // Date Picker Click
        cardDatePicker.setOnClickListener {
            showDatePicker { date ->
                selectedDate = date
                tvSelectedDate.text = date
            }
        }

        // Time Picker Click
        cardTimePicker.setOnClickListener {
            showTimePicker { time ->
                selectedTime = time
                tvSelectedTime.text = time
                tvTimeDisplay.text = time
            }
        }

        // Reminder Click
        tvSetReminder.setOnClickListener {
            showReminderDialog()
        }

        // Priority Selection
        rgPriority.setOnCheckedChangeListener { _, checkedId ->
            selectedPriority = when (checkedId) {
                R.id.rb_high -> "high"
                R.id.rb_low -> "low"
                else -> "medium"
            }
        }

        // Edit Mode Check
        editTaskId = intent.getIntExtra("EDIT_TASK_ID", -1)
        Log.d("AddTaskActivity", "Edit mode: editTaskId = $editTaskId")

        if (editTaskId != -1) {
            btnSubmit.text = "Update Task"

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    Log.d("AddTaskActivity", "Loading task for edit, ID: $editTaskId")

                    val task = repository.getTaskById(editTaskId)

                    if (task != null) {
                        Log.d("AddTaskActivity", "Task loaded: ${task.title}")

                        withContext(Dispatchers.Main) {
                            etTitle.setText(task.title)
                            etDescription.setText(task.description)
                            etSubmission.setText(task.location)
                            selectedPriority = task.priority
                            selectedDate = task.dueDate
                            selectedTime = task.dueTime

                            tvSelectedDate.text = selectedDate
                            tvSelectedTime.text = selectedTime
                            tvTimeDisplay.text = selectedTime

                            // Load reminder info
                            if (task.reminderSet) {
                                // Calculate remaining offset (not past time)
                                val currentTime = System.currentTimeMillis()
                                if (task.reminderTime > currentTime) {
                                    selectedReminderOffset = task.reminderTime - currentTime
                                    tvSetReminder.text = "✅ Reminder active"
                                    Log.d("AddTaskActivity", "Reminder loaded: ${task.reminderTime}")
                                } else {
                                    tvSetReminder.text = "⏰ Set Reminder"
                                    Log.d("AddTaskActivity", "Reminder expired")
                                }
                            }

                            when(task.priority) {
                                "high" -> rgPriority.check(R.id.rb_high)
                                "low" -> rgPriority.check(R.id.rb_low)
                                else -> rgPriority.check(R.id.rb_medium)
                            }
                        }

                        Log.d("AddTaskActivity", "Task details populated successfully")
                    } else {
                        Log.e("AddTaskActivity", "Task not found for ID: $editTaskId")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@AddTaskActivity,
                                "Error: Task not found",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AddTaskActivity", "Error loading task: ${e.message}", e)
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@AddTaskActivity,
                            "Error loading task: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
            }
        }

        // Submit Button
        btnSubmit.setOnClickListener {
            val title = etTitle.text.toString()
            val description = etDescription.text.toString()
            val submission = etSubmission.text.toString().ifEmpty { "Google Classroom" }

            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter task title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveTask(title, description, submission)
        }
    }

    private fun saveTask(title: String, description: String, submission: String) {
        // ✅ SHOW LOADING STATE
        setLoadingState(true)

        lifecycleScope.launch(Dispatchers.IO) {  // ✅ Use Dispatchers.IO for all database operations
            try {
                if (editTaskId == -1) {
                    // ✅ CREATE NEW TASK
                    val task = Task(
                        title = title,
                        description = description.ifEmpty { "No description" },
                        location = submission,
                        dueDate = selectedDate,
                        dueTime = selectedTime,
                        priority = selectedPriority
                    )

                    // Insert and get the task ID
                    val taskId = repository.insertTask(task).toInt()
                    val insertedTask = task.copy(id = taskId)

                    // ✅ SET REMINDER IF SELECTED
                    if (selectedReminderOffset > 0) {
                        setTaskReminder(taskId, title, description)
                    }

                    // ✅ SYNC TO CLOUD (dengan offline support)
                    syncTaskToCloud(insertedTask)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@AddTaskActivity,
                            "✅ Task added successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        setLoadingState(false)
                        finish()
                    }

                } else {
                    // ✅ UPDATE EXISTING TASK - NOW WITH Dispatchers.IO!
                    val existingTask = repository.getTaskById(editTaskId)  // This needs IO thread!

                    if (existingTask != null) {
                        val updatedTask = existingTask.copy(
                            title = title,
                            description = description,
                            location = submission,
                            dueDate = selectedDate,
                            dueTime = selectedTime,
                            priority = selectedPriority
                        )
                        repository.updateTask(updatedTask)

                        // ✅ UPDATE REMINDER
                        if (selectedReminderOffset > 0) {
                            setTaskReminder(editTaskId, title, description)
                        } else if (existingTask.reminderSet && selectedReminderOffset == 0L) {
                            withContext(Dispatchers.Main) {
                                ReminderHelper.cancelReminder(this@AddTaskActivity, editTaskId)
                            }
                        }

                        // ✅ SYNC TO CLOUD
                        syncTaskToCloud(updatedTask)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@AddTaskActivity,
                                "✅ Task updated successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            setLoadingState(false)
                            finish()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@AddTaskActivity,
                                "❌ Error: Task not found",
                                Toast.LENGTH_SHORT
                            ).show()
                            setLoadingState(false)
                            finish()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AddTaskActivity", "Error saving task: ${e.message}", e)
                e.printStackTrace()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@AddTaskActivity,
                        "❌ Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    setLoadingState(false)
                }
            }
        }
    }

    // ✅ SYNC TO CLOUD WITH OFFLINE SUPPORT
    private suspend fun syncTaskToCloud(task: Task) {
        withContext(Dispatchers.IO) {
            try {
                if (networkHelper.isNetworkAvailable()) {
                    // Online: sync to Firebase
                    FirebaseManager.syncTaskToCloud(task)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@AddTaskActivity,
                            "☁️ Synced to cloud",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    Log.d("AddTaskActivity", "Task synced to cloud: ${task.title}")
                } else {
                    // Offline: saved locally, will sync automatically when online
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@AddTaskActivity,
                            "📱 Saved locally. Will sync when online.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    Log.d("AddTaskActivity", "Offline mode: Task saved locally")
                }
            } catch (e: Exception) {
                Log.e("AddTaskActivity", "Sync error: ${e.message}", e)

                // Don't show error to user - silent fail for better UX
                // Task is already saved locally, sync will retry later
            }
        }
    }

    // ✅ LOADING STATE MANAGEMENT
    private fun setLoadingState(isLoading: Boolean) {
        btnSubmit.isEnabled = !isLoading
        btnSubmit.text = if (isLoading) {
            if (editTaskId == -1) "Creating..." else "Updating..."
        } else {
            if (editTaskId == -1) "Create Task" else "Update Task"
        }

        // Jika ada progressBar di layout
        // progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            val formattedDate = String.format("%02d-%02d-%04d", day, month + 1, year)
            onDateSelected(formattedDate)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            val formattedTime = String.format("%02d:%02d", hour, minute)
            onTimeSelected(formattedTime)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    private fun showReminderDialog() {
        val options = arrayOf(
            "⏰ 15 minutes before",
            "⏰ 30 minutes before",
            "⏰ 1 hour before",
            "⏰ 1 day before",
            "🚫 No reminder"
        )

        AlertDialog.Builder(this)
            .setTitle("Set Reminder")
            .setItems(options) { _, which ->
                selectedReminderOffset = when (which) {
                    0 -> 15 * 60 * 1000L
                    1 -> 30 * 60 * 1000L
                    2 -> 60 * 60 * 1000L
                    3 -> 24 * 60 * 60 * 1000L
                    else -> 0L
                }

                val reminderText = if (selectedReminderOffset > 0) {
                    "✅ Reminder: ${options[which]}"
                } else {
                    "⏰ Set Reminder"
                }

                findViewById<TextView>(R.id.tv_set_reminder).text = reminderText
                Toast.makeText(this, reminderText, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private suspend fun setTaskReminder(taskId: Int, title: String, description: String) {
        try {
            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
            val deadline = sdf.parse("$selectedDate $selectedTime")
            val deadlineMillis = deadline?.time ?: return
            val reminderMillis = deadlineMillis - selectedReminderOffset

            if (reminderMillis < System.currentTimeMillis()) {
                withContext(Dispatchers.Main) {  // ✅ Switch to Main thread for Toast
                    Toast.makeText(
                        this@AddTaskActivity,
                        "⚠️ Reminder time is in the past",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }

            val task = repository.getTaskByIdOnce(taskId)
            if (task != null) {
                val taskWithReminder = task.copy(
                    reminderTime = reminderMillis,
                    reminderSet = true
                )
                repository.updateTask(taskWithReminder)

                // ✅ Call ReminderHelper on Main thread (system API)
                withContext(Dispatchers.Main) {
                    ReminderHelper.setReminder(this@AddTaskActivity, taskWithReminder, reminderMillis)

                    Toast.makeText(
                        this@AddTaskActivity,
                        "🔔 Reminder scheduled!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                Log.d("AddTaskActivity", "Reminder set successfully for task $taskId")
            }
        } catch (e: Exception) {
            Log.e("AddTaskActivity", "Error setting reminder: ${e.message}", e)

            withContext(Dispatchers.Main) {  // ✅ Switch to Main thread for Toast
                Toast.makeText(
                    this@AddTaskActivity,
                    "❌ Error setting reminder: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
