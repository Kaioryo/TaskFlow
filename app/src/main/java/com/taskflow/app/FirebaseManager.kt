package com.taskflow.app

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseManager {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Get current user
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    // Check if user is logged in
    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    // ✅ SIGN UP
    suspend fun signUp(email: String, password: String, username: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User creation failed")

            // Save user data to Firestore
            val userData = User(
                uid = user.uid,
                email = email,
                username = username
            )
            saveUserToFirestore(userData)

            Log.d("FirebaseManager", "✅ Sign up successful: ${user.email}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ Sign up failed: ${e.message}")
            Result.failure(e)
        }
    }

    // ✅ SIGN IN
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Sign in failed")

            // Update last login
            updateLastLogin(user.uid)

            Log.d("FirebaseManager", "✅ Sign in successful: ${user.email}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ Sign in failed: ${e.message}")
            Result.failure(e)
        }
    }

    // ✅ SIGN OUT
    fun signOut() {
        auth.signOut()
        Log.d("FirebaseManager", "🚪 User signed out")
    }

    // ✅ SAVE USER TO FIRESTORE
    private suspend fun saveUserToFirestore(user: User) {
        try {
            firestore.collection("users")
                .document(user.uid)
                .set(user)
                .await()
            Log.d("FirebaseManager", "✅ User saved to Firestore")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ Error saving user: ${e.message}")
        }
    }

    // ✅ UPDATE LAST LOGIN
    private suspend fun updateLastLogin(uid: String) {
        try {
            firestore.collection("users")
                .document(uid)
                .update("lastLogin", System.currentTimeMillis())
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ Error updating last login: ${e.message}")
        }
    }

    // ✅ GET USER DATA FROM FIRESTORE
    suspend fun getUserData(uid: String): User? {
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ Error getting user: ${e.message}")
            null
        }
    }

    // ✅ SYNC TASK TO FIRESTORE (FIXED - Use Firestore Auto ID)
    suspend fun syncTaskToCloud(task: Task) {
        try {
            val userId = getCurrentUser()?.uid
            if (userId == null) {
                Log.e("FirebaseManager", "❌ User not logged in, cannot sync")
                return
            }

            Log.d("FirebaseManager", "🔄 Syncing task: ${task.title}")

            // ✅ GUNAKAN FIRESTORE AUTO-GENERATED ID atau roomId sebagai string
            val taskData = hashMapOf(
                "id" to task.id,
                "title" to task.title,
                "description" to task.description,
                "location" to task.location,
                "dueDate" to task.dueDate,
                "dueTime" to task.dueTime,
                "priority" to task.priority,
                "isCompleted" to task.isCompleted,
                "createdAt" to task.createdAt,
                "reminderTime" to task.reminderTime,
                "reminderSet" to task.reminderSet
            )

            val docRef = firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .document("task_${task.id}") // ✅ Prefix dengan "task_"

            // Set with merge to avoid overwriting
            docRef.set(taskData)
                .await()

            Log.d("FirebaseManager", "✅ Task synced: ${task.title} (ID: task_${task.id})")

            // ✅ VERIFY: Read back to confirm
            val verifyDoc = docRef.get().await()
            if (verifyDoc.exists()) {
                Log.d("FirebaseManager", "✅ VERIFIED: Task exists in Firestore")
            } else {
                Log.e("FirebaseManager", "❌ VERIFICATION FAILED: Task not found in Firestore")
            }

        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ Sync failed for ${task.title}: ${e.message}", e)
        }
    }

    // ✅ GET ALL TASKS FROM CLOUD (FIXED)
    suspend fun getTasksFromCloud(): List<Task> {
        return try {
            val userId = getCurrentUser()?.uid
            if (userId == null) {
                Log.e("FirebaseManager", "❌ User not logged in, cannot fetch tasks")
                return emptyList()
            }

            Log.d("FirebaseManager", "🔄 Fetching tasks from cloud for user: $userId")

            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .get()
                .await()

            Log.d("FirebaseManager", "📊 Found ${snapshot.documents.size} documents in Firestore")

            val tasks = snapshot.documents.mapNotNull { doc ->
                try {
                    val task = Task(
                        id = doc.getLong("id")?.toInt() ?: 0,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        location = doc.getString("location") ?: "",
                        dueDate = doc.getString("dueDate") ?: "",
                        dueTime = doc.getString("dueTime") ?: "",
                        priority = doc.getString("priority") ?: "medium",
                        isCompleted = doc.getBoolean("isCompleted") ?: false,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        reminderTime = doc.getLong("reminderTime") ?: 0,
                        reminderSet = doc.getBoolean("reminderSet") ?: false
                    )
                    Log.d("FirebaseManager", "✅ Parsed task: ${task.title}")
                    task
                } catch (e: Exception) {
                    Log.e("FirebaseManager", "❌ Error parsing task: ${e.message}")
                    null
                }
            }

            Log.d("FirebaseManager", "✅ Fetched ${tasks.size} tasks from cloud")
            tasks

        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ Error fetching tasks: ${e.message}", e)
            emptyList()
        }
    }

    // ✅ DELETE TASK FROM CLOUD (FIXED)
    suspend fun deleteTaskFromCloud(taskId: Int) {
        try {
            val userId = getCurrentUser()?.uid ?: return

            firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .document("task_$taskId") // ✅ Same prefix
                .delete()
                .await()

            Log.d("FirebaseManager", "✅ Task deleted from cloud: $taskId")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ Delete failed: ${e.message}")
        }
    }
}
