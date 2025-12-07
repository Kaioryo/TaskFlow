package com.taskflow.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FirebaseManager.isUserLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val tvRegister = findViewById<TextView>(R.id.tv_register)
        val btnGoogleSignIn = findViewById<Button>(R.id.btn_google_signin)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                btnLogin.isEnabled = false
                btnLogin.text = "Logging in..."
                val result = FirebaseManager.signIn(email, password)
                result.onSuccess { user ->
                    // ✅ CHECK & CLEAR if different user
                    checkAndClearDatabaseIfNeeded(user.uid)

                    val userModel = FirebaseManager.getUserData(user.uid)
                    getSharedPreferences("TaskFlowPrefs", Context.MODE_PRIVATE).edit()
                        .putString("email", user.email)
                        .putString("uid", user.uid)
                        .putString("username", userModel?.username ?: user.email?.substringBefore("@"))
                        .apply()
                    Toast.makeText(this@LoginActivity, "✅ Welcome back!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }// In signIn function
                result.onFailure { error ->
                    btnLogin.isEnabled = true
                    btnLogin.text = "Login"

                    // ✅ USE ErrorHandler
                    ErrorHandler.handleException(
                        this@LoginActivity,
                        error as Exception,
                        "Login failed"
                    )
                }
            }
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnGoogleSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account.idToken!!)
                } else {
                    Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val googleCredential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(googleCredential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        lifecycleScope.launch {
                            // ✅ CHECK & CLEAR if different user
                            checkAndClearDatabaseIfNeeded(user.uid)

                            val userModel = FirebaseManager.getUserData(user.uid)
                            getSharedPreferences("TaskFlowPrefs", Context.MODE_PRIVATE).edit()
                                .putString("email", user.email)
                                .putString("uid", user.uid)
                                .putString("username", userModel?.username ?: user.displayName ?: user.email)
                                .apply()

                            Toast.makeText(this@LoginActivity, "✅ Welcome ${user.displayName}!", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    }
                } else {
                    if (task.exception is FirebaseAuthUserCollisionException) {
                        val exc = task.exception as FirebaseAuthUserCollisionException
                        val email = exc.email
                        val input = EditText(this)
                        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

                        AlertDialog.Builder(this)
                            .setTitle("Akun sudah terdaftar")
                            .setMessage("Akun ini sudah terdaftar secara manual. Silakan masukkan password email untuk menggabungkan login manual dan Google!")
                            .setView(input)
                            .setPositiveButton("Link") { _, _ ->
                                val password = input.text.toString()
                                val manualCred = EmailAuthProvider.getCredential(email!!, password)
                                FirebaseAuth.getInstance().signInWithCredential(manualCred)
                                    .addOnSuccessListener { result ->
                                        result.user?.linkWithCredential(googleCredential)
                                            ?.addOnSuccessListener {
                                                Toast.makeText(this, "Akun berhasil di-link!\nSekarang bisa login manual/Google!", Toast.LENGTH_LONG).show()
                                                val intent = Intent(this, MainActivity::class.java)
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                startActivity(intent)
                                                finish()
                                            }
                                            ?.addOnFailureListener { e ->
                                                Toast.makeText(this, "Gagal link: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "Password salah. Tidak bisa link akun. Coba ulangi.", Toast.LENGTH_LONG).show()
                                    }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    } else {
                        Toast.makeText(this, "❌ Google Sign-In failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    // ✅ NEW: Check and clear database if different user
    private suspend fun checkAndClearDatabaseIfNeeded(newUid: String) {
        withContext(Dispatchers.IO) {
            try {
                val sharedPref = getSharedPreferences("TaskFlowPrefs", Context.MODE_PRIVATE)
                val lastUid = sharedPref.getString("last_uid", null)

                Log.d("LoginActivity", "Last UID: $lastUid")
                Log.d("LoginActivity", "New UID: $newUid")

                if (lastUid != null && lastUid != newUid) {
                    // Different user! Clear local database
                    Log.d("LoginActivity", "🗑️ Different user detected! Clearing local database...")

                    val database = AppDatabase.getDatabase(this@LoginActivity)
                    val repository = TaskRepository(database.taskDao())
                    repository.deleteAllTasks()

                    Log.d("LoginActivity", "✅ Local database cleared for new user")
                } else {
                    Log.d("LoginActivity", "✅ Same user or first login, keeping database")
                }

                // Save new UID
                sharedPref.edit()
                    .putString("last_uid", newUid)
                    .apply()

            } catch (e: Exception) {
                Log.e("LoginActivity", "❌ Error checking/clearing database: ${e.message}", e)
            }
        }
    }
}
