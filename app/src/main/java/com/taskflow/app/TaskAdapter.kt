package com.taskflow.app

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private var tasks: List<Task>,
    private val onTaskClick: (Task) -> Unit,
    private val onTaskDelete: (Task) -> Unit,
    private val onTaskToggle: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_task_title)
        val tvDescription: TextView = itemView.findViewById(R.id.tv_task_description)
        val tvDueDate: TextView = itemView.findViewById(R.id.tv_task_due_date)
        val tvTimeRemaining: TextView = itemView.findViewById(R.id.tv_time_remaining)
        val cbCompleted: CheckBox = itemView.findViewById(R.id.cb_task_completed)
        val btnDelete: ImageView = itemView.findViewById(R.id.btn_delete_task)
        val cardView: CardView = itemView.findViewById(R.id.card_task)
        val tvPriority: TextView = itemView.findViewById(R.id.tv_priority)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        holder.tvTitle.text = task.title
        holder.tvDescription.text = task.description
        holder.tvDueDate.text = "${task.dueDate} • ${task.dueTime}"

        // Priority badge
        holder.tvPriority.text = task.priority.uppercase()
        when (task.priority.lowercase()) {
            "high" -> {
                holder.tvPriority.setBackgroundColor(Color.parseColor("#FFCDD2"))
                holder.tvPriority.setTextColor(Color.parseColor("#D32F2F"))
            }
            "low" -> {
                holder.tvPriority.setBackgroundColor(Color.parseColor("#C8E6C9"))
                holder.tvPriority.setTextColor(Color.parseColor("#388E3C"))
            }
            else -> { // medium
                holder.tvPriority.setBackgroundColor(Color.parseColor("#FFE0B2"))
                holder.tvPriority.setTextColor(Color.parseColor("#F57C00"))
            }
        }

        // Time remaining with real-time calculation
        val timeRemaining = DateTimeHelper.getTimeRemaining(task.dueDate, task.dueTime)
        holder.tvTimeRemaining.text = timeRemaining

        // Color based on urgency
        when {
            DateTimeHelper.isOverdue(task.dueDate, task.dueTime) -> {
                holder.tvTimeRemaining.setTextColor(Color.parseColor("#D32F2F")) // Red
            }
            DateTimeHelper.isDueSoon(task.dueDate, task.dueTime) -> {
                holder.tvTimeRemaining.setTextColor(Color.parseColor("#F57C00")) // Orange
            }
            else -> {
                holder.tvTimeRemaining.setTextColor(Color.parseColor("#388E3C")) // Green
            }
        }

        holder.cbCompleted.isChecked = task.isCompleted

        // Strike-through if completed
        if (task.isCompleted) {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvDescription.alpha = 0.5f
            holder.tvDueDate.alpha = 0.5f
        } else {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvDescription.alpha = 1.0f
            holder.tvDueDate.alpha = 1.0f
        }

        // Click listeners
        holder.cbCompleted.setOnClickListener {
            onTaskToggle(task)
        }

        holder.btnDelete.setOnClickListener {
            onTaskDelete(task)
        }

        holder.cardView.setOnClickListener {
            onTaskClick(task)
        }
    }

    override fun getItemCount() = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}
