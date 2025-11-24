package com.taskflow.app

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "location")
    val location: String,

    @ColumnInfo(name = "due_date")
    val dueDate: String,

    @ColumnInfo(name = "due_time")
    val dueTime: String,

    @ColumnInfo(name = "priority")
    val priority: String = "medium",

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "reminderTime")
    val reminderTime: Long = 0,

    @ColumnInfo(name = "reminderSet")
    val reminderSet: Boolean = false
)
