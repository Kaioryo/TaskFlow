package com.taskflow.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etUsername = findViewById<EditText>(R.id.et_username)
        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val etConfirmPassword = findViewById<EditText>(R.id.et_confirm_password)
        val btnRegister = findViewById<Button>(R.id.btn_register)
        val tvLogin = findViewById<TextView>(R.id.tv_login)

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            when {
                username.isEmpty() || email.isEmpty() || password.isEmpty() -> {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                password != confirmPassword -> {
                    Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                password.length < 6 -> {
                    Toast.makeText(this, "Password min 6 chars", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            lifecycleScope.launch {
                btnRegister.isEnabled = false
                btnRegister.text = "Registering..."
                val result = FirebaseManager.signUp(email, password, username)
                result.onSuccess { user ->
                    // Ambil data user dari Firestore untuk simpan username
                    val userModel = FirebaseManager.getUserData(user.uid)
                    val sharedPref = getSharedPreferences("TaskFlowPrefs", MODE_PRIVATE).edit()
                    sharedPref.putString("email", email)
                    sharedPref.putString("uid", user.uid)
                    sharedPref.putString("username", userModel?.username ?: username)
                    sharedPref.apply()

                    // Redirect ke Home
                    startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                    finish()
                }.onFailure { error ->
                    btnRegister.isEnabled = true
                    btnRegister.text = "Register"
                    Toast.makeText(
                        this@RegisterActivity,
                        "❌ Registration failed: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        tvLogin.setOnClickListener {
            finish()
        }
    }
}
