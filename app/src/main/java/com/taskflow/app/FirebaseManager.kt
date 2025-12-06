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

    // ✅ SYNC TASK TO FIRESTORE
    suspend fun syncTaskToCloud(task: Task) {
        try {
            val userId = getCurrentUser()?.uid ?: return

            firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .document(task.id.toString())
                .set(task)
                .await()

            Log.d("FirebaseManager", "✅ Task synced: ${task.title}")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ Sync failed: ${e.message}")
        }
    }

    // ✅ GET ALL TASKS FROM CLOUD
    suspend fun getTasksFromCloud(): List<Task> {
        return try {
            val userId = getCurrentUser()?.uid ?: return emptyList()

            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .get()
                .await()

            snapshot.documents.mapNotNull { it.toObject(Task::class.java) }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ Error fetching tasks: ${e.message}")
            emptyList()
        }
    }

    // ✅ DELETE TASK FROM CLOUD
    suspend fun deleteTaskFromCloud(taskId: Int) {
        try {
            val userId = getCurrentUser()?.uid ?: return

            firestore.collection("users")
                .document(userId)
                .collection("tasks")
                .document(taskId.toString())
                .delete()
                .await()

            Log.d("FirebaseManager", "✅ Task deleted from cloud: $taskId")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "❌ Delete failed: ${e.message}")
        }
    }
}
