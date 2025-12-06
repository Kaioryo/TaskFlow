package com.taskflow.app

import android.app.Application

class TaskFlowApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // ✅ Initialize RetrofitClient dengan context untuk cache
        RetrofitClient.initialize(this)
    }
}
