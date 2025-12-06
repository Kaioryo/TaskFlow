package com.taskflow.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText

class ProfileFragment : Fragment() {

    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var btnEditProfile: Button
    private lateinit var btnChangePassword: LinearLayout
    private lateinit var btnHelp: LinearLayout
    private lateinit var btnSettings: LinearLayout
    private lateinit var btnLogout: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        tvUsername = view.findViewById(R.id.tv_username)
        tvEmail = view.findViewById(R.id.tv_email)
        btnEditProfile = view.findViewById(R.id.btn_edit_profile)
        btnChangePassword = view.findViewById(R.id.btn_change_password)
        btnHelp = view.findViewById(R.id.btn_help)
        btnSettings = view.findViewById(R.id.btn_settings)
        btnLogout = view.findViewById(R.id.btn_logout)

        // Load user data
        loadUserData()

        // Setup click listeners
        btnEditProfile.setOnClickListener { showEditProfileDialog() }
        btnChangePassword.setOnClickListener { showChangePasswordDialog() }
        btnHelp.setOnClickListener { showHelpDialog() }
        btnSettings.setOnClickListener { showSettingsDialog() }
        btnLogout.setOnClickListener { logout() }
    }

    private fun loadUserData() {
        val sharedPref = requireContext().getSharedPreferences("TaskFlowPrefs", Context.MODE_PRIVATE)
        val username = sharedPref.getString("username", "User140810220000") ?: "User140810220000"
        val email = sharedPref.getString("email", "username@gmail.com") ?: "username@gmail.com"

        tvUsername.text = username
        tvEmail.text = email
    }

    private fun showEditProfileDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null)
        val etUsername = dialogView.findViewById<TextInputEditText>(R.id.et_username)
        val etEmail = dialogView.findViewById<TextInputEditText>(R.id.et_email)

        // Pre-fill current data
        val sharedPref = requireContext().getSharedPreferences("TaskFlowPrefs", Context.MODE_PRIVATE)
        etUsername.setText(sharedPref.getString("username", ""))
        etEmail.setText(sharedPref.getString("email", ""))

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val newUsername = etUsername.text.toString()
                val newEmail = etEmail.text.toString()

                if (newUsername.isNotEmpty() && newEmail.isNotEmpty()) {
                    // Save to SharedPreferences
                    sharedPref.edit()
                        .putString("username", newUsername)
                        .putString("email", newEmail)
                        .apply()

                    // Update UI
                    loadUserData()
                    Toast.makeText(requireContext(), "✅ Profile updated successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "❌ Please fill all fields", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showChangePasswordDialog() {
        val passwordFields = arrayOf("Current Password", "New Password", "Confirm Password")
        val inputs = arrayOfNulls<String>(3)

        AlertDialog.Builder(requireContext())
            .setTitle("Change Password")
            .setMessage("This feature will validate your password securely.")
            .setPositiveButton("Change") { dialog, _ ->
                Toast.makeText(requireContext(), "🔐 Password changed successfully!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showHelpDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("❓ Help & Support")
            .setMessage("""
                Need assistance with TaskFlow?
                
                📧 Email: support@taskflow.com
                📞 Phone: +62 123 4567 890
                🌐 Website: www.taskflow.com
                
                We're here to help 24/7!
            """.trimIndent())
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .setNegativeButton("Contact Us") { _, _ ->
                Toast.makeText(requireContext(), "Opening contact form...", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showSettingsDialog() {
        val options = arrayOf("🔔 Notifications", "🌐 Language", "🎨 Theme", "ℹ️ About")

        AlertDialog.Builder(requireContext())
            .setTitle("⚙️ Settings")
            .setItems(options) { dialog, which ->
                val selected = options[which]
                Toast.makeText(requireContext(), "Selected: $selected", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun logout() {
        AlertDialog.Builder(requireContext())
            .setTitle("🚪 Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes, Logout") { _, _ ->
                // Sign out remote (firebase) dan local (prefs)
                FirebaseManager.signOut()
                requireContext().getSharedPreferences("TaskFlowPrefs", Context.MODE_PRIVATE)
                    .edit().clear().apply()
                // Redirect ke Login, dan hapus seluruh history
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                // Optional: finish activity supaya tidak bisa 'back'
                requireActivity().finish()
                Toast.makeText(requireContext(), "👋 Logged out successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
