package com.taskflow.app

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis()
) {
    // Firestore requires no-arg constructor
    constructor() : this("", "", "", 0L, 0L)
}
