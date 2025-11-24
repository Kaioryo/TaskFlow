package com.taskflow.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddTaskActivity : AppCompatActivity() {

    private lateinit var repository: TaskRepository
    private var selectedPriority = "medium"
    private var selectedDate = "24-01-2024"
    private var selectedTime = "23:59"
    private var selectedReminderOffset: Long = 0
    private var editTaskId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        val database = AppDatabase.getDatabase(this)
        repository = TaskRepository(database.taskDao())

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
        val rgPriority = findViewById<RadioGroup>(R.id.rg_priority)
        val btnSubmit = findViewById<Button>(R.id.btn_submit)

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

        if (editTaskId != -1) {
            btnSubmit.text = "Update Task"

            lifecycleScope.launch {
                val task = repository.getTaskById(editTaskId)
                if (task != null) {
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
                        tvSetReminder.text = "✅ Reminder active"
                    }

                    when(task.priority) {
                        "high" -> rgPriority.check(R.id.rb_high)
                        "low" -> rgPriority.check(R.id.rb_low)
                        else -> rgPriority.check(R.id.rb_medium)
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

            lifecycleScope.launch {
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

                        // ✅ SET REMINDER IF SELECTED
                        if (selectedReminderOffset > 0) {
                            setTaskReminder(taskId, title, description)
                        }

                        Toast.makeText(
                            this@AddTaskActivity,
                            "✅ Task added successfully!",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {
                        // ✅ UPDATE EXISTING TASK
                        val existingTask = repository.getTaskById(editTaskId)
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
                                // Cancel reminder if user removed it
                                ReminderHelper.cancelReminder(this@AddTaskActivity, editTaskId)
                            }

                            Toast.makeText(
                                this@AddTaskActivity,
                                "✅ Task updated successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@AddTaskActivity,
                        "❌ Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
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
                    0 -> 15 * 60 * 1000L       // 15 minutes
                    1 -> 30 * 60 * 1000L       // 30 minutes
                    2 -> 60 * 60 * 1000L       // 1 hour
                    3 -> 24 * 60 * 60 * 1000L  // 1 day
                    else -> 0L                  // No reminder
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
            // Calculate reminder time
            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
            val deadline = sdf.parse("$selectedDate $selectedTime")
            val deadlineMillis = deadline?.time ?: return
            val reminderMillis = deadlineMillis - selectedReminderOffset

            // Check if reminder is in the future
            if (reminderMillis < System.currentTimeMillis()) {
                Toast.makeText(
                    this,
                    "⚠️ Reminder time is in the past",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            // Get the task and update with reminder info
            val task = repository.getTaskByIdOnce(taskId)
            if (task != null) {
                val taskWithReminder = task.copy(
                    reminderTime = reminderMillis,
                    reminderSet = true
                )
                repository.updateTask(taskWithReminder)

                // Schedule the alarm
                ReminderHelper.setReminder(this, taskWithReminder, reminderMillis)

                Toast.makeText(
                    this,
                    "🔔 Reminder scheduled!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "❌ Error setting reminder: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
