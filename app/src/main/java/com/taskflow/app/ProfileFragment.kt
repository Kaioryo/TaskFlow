package com.taskflow.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileFragment : Fragment() {

    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var btnEditProfile: Button
    private lateinit var btnChangePassword: LinearLayout
    private lateinit var btnLogout: LinearLayout

    // ✅ Task Statistics Views
    private lateinit var tvTotalTasks: TextView
    private lateinit var tvCompletedTasks: TextView
    private lateinit var tvPendingTasks: TextView
    private lateinit var tvOverdueTasks: TextView

    private lateinit var repository: TaskRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize repository
        val database = AppDatabase.getDatabase(requireContext())
        repository = TaskRepository(database.taskDao())

        // Initialize views
        tvUsername = view.findViewById(R.id.tv_username)
        tvEmail = view.findViewById(R.id.tv_email)
        btnEditProfile = view.findViewById(R.id.btn_edit_profile)
        btnChangePassword = view.findViewById(R.id.btn_change_password)
        btnLogout = view.findViewById(R.id.btn_logout)

        // ✅ Initialize statistics views
        tvTotalTasks = view.findViewById(R.id.tv_total_tasks)
        tvCompletedTasks = view.findViewById(R.id.tv_completed_tasks)
        tvPendingTasks = view.findViewById(R.id.tv_pending_tasks)
        tvOverdueTasks = view.findViewById(R.id.tv_overdue_tasks)

        // ✅ Hide Edit Profile button
        btnEditProfile.visibility = View.GONE

        // Load user data
        loadUserData()

        // ✅ Load task statistics
        loadTaskStatistics()

        // ✅ Check if user signed in with Google
        checkAuthProvider()

        // Setup click listeners
        btnChangePassword.setOnClickListener { showChangePasswordDialog() }
        btnLogout.setOnClickListener { logout() }
    }

    private fun loadUserData() {
        val sharedPref = requireContext().getSharedPreferences("TaskFlowPrefs", Context.MODE_PRIVATE)
        val username = sharedPref.getString("username", "User") ?: "User"
        val email = sharedPref.getString("email", "user@example.com") ?: "user@example.com"

        tvUsername.text = username
        tvEmail.text = email
    }

    // ✅ Load Task Statistics
    private fun loadTaskStatistics() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.allTasks.collect { tasks ->
                val total = tasks.size
                val completed = tasks.count { it.isCompleted }
                val pending = tasks.count { !it.isCompleted }
                val overdue = tasks.count {
                    !it.isCompleted && DateTimeHelper.isOverdue(it.dueDate, it.dueTime)
                }

                tvTotalTasks.text = total.toString()
                tvCompletedTasks.text = completed.toString()
                tvPendingTasks.text = pending.toString()
                tvOverdueTasks.text = overdue.toString()
            }
        }
    }

    // ✅ Check if user logged in with Google
// ✅ Check if user logged in with Google
    private fun checkAuthProvider() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val providerData = currentUser.providerData

            // Check if user signed in with Google
            val isGoogleSignIn = providerData.any {
                it.providerId == "google.com"
            }

            if (isGoogleSignIn) {
                // ✅ Hide ENTIRE CARD for Google users
                view?.findViewById<androidx.cardview.widget.CardView>(R.id.card_menu_options)?.visibility = View.GONE
                Log.d("ProfileFragment", "Google sign-in detected, hiding Change Password card")
            } else {
                // ✅ Show card for Email/Password users
                view?.findViewById<androidx.cardview.widget.CardView>(R.id.card_menu_options)?.visibility = View.VISIBLE
                Log.d("ProfileFragment", "Email sign-in detected, showing Change Password card")
            }
        }
    }

    // ✅ SIMPLE Change Password Dialog (No inline validation)
    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_change_password, null)

        val etCurrentPassword = dialogView.findViewById<TextInputEditText>(R.id.et_current_password)
        val etNewPassword = dialogView.findViewById<TextInputEditText>(R.id.et_new_password)
        val etConfirmPassword = dialogView.findViewById<TextInputEditText>(R.id.et_confirm_password)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Change Password") { dialog, _ ->
                val currentPassword = etCurrentPassword.text.toString()
                val newPassword = etNewPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()

                // Validate inputs
                when {
                    currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty() -> {
                        Toast.makeText(requireContext(), "❌ Please fill all fields", Toast.LENGTH_SHORT).show()
                    }
                    newPassword.length < 6 -> {
                        Toast.makeText(requireContext(), "❌ Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    }
                    newPassword != confirmPassword -> {
                        Toast.makeText(requireContext(), "❌ Passwords don't match", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        changePasswordInFirebase(currentPassword, newPassword)
                    }
                }

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // ✅ Change Password in Firebase
    private fun changePasswordInFirebase(currentPassword: String, newPassword: String) {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null || user.email == null) {
            Toast.makeText(requireContext(), "❌ User not found", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Re-authenticate user with current password
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
                user.reauthenticate(credential).await()

                // Update password
                user.updatePassword(newPassword).await()

                Toast.makeText(
                    requireContext(),
                    "✅ Password changed successfully!",
                    Toast.LENGTH_SHORT
                ).show()

                Log.d("ProfileFragment", "Password changed successfully")

            } catch (e: Exception) {
                Log.e("ProfileFragment", "Password change failed: ${e.message}", e)

                val errorMessage = when {
                    e.message?.contains("password is invalid") == true ->
                        "❌ Current password is incorrect"
                    e.message?.contains("network") == true ->
                        "❌ Network error, please try again"
                    else ->
                        "❌ Error: ${e.message}"
                }

                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun logout() {
        AlertDialog.Builder(requireContext())
            .setTitle("🚪 Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes, Logout") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        // ✅ NO NEED TO CLEAR HERE - Will be cleared on next login if different user
                        // Just sign out
                        FirebaseManager.signOut()

                        // Clear SharedPreferences EXCEPT last_uid
                        val sharedPref = requireContext().getSharedPreferences("TaskFlowPrefs", Context.MODE_PRIVATE)
                        val lastUid = sharedPref.getString("last_uid", null)

                        sharedPref.edit()
                            .clear()
                            .putString("last_uid", lastUid) // Keep last_uid for comparison
                            .apply()

                        // Redirect to Login
                        val intent = Intent(requireContext(), LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        requireActivity().finish()

                        Toast.makeText(requireContext(), "👋 Logged out successfully", Toast.LENGTH_SHORT).show()

                    } catch (e: Exception) {
                        Log.e("ProfileFragment", "❌ Logout error: ${e.message}", e)
                        Toast.makeText(requireContext(), "❌ Logout failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
