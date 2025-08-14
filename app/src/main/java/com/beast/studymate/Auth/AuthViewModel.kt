package com.beast.studymate.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        Log.d("AuthViewModel", "Login attempt for email: $email")

        if (email.isBlank() || password.isBlank()) {
            Log.d("AuthViewModel", "Login failed: Empty email or password")
            onResult(false, "Email and password cannot be empty")
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AuthViewModel", "Login successful for user: ${auth.currentUser?.uid}")
                    onResult(true, null)
                } else {
                    val exception = task.exception
                    Log.e("AuthViewModel", "Login failed", exception)

                    val errorMessage = when (exception) {
                        is FirebaseAuthException -> {
                            when (exception.errorCode) {
                                "ERROR_INVALID_EMAIL" -> "Invalid email format"
                                "ERROR_WRONG_PASSWORD" -> "Incorrect password"
                                "ERROR_USER_NOT_FOUND" -> "No account found with this email"
                                "ERROR_USER_DISABLED" -> "This account has been disabled"
                                "ERROR_TOO_MANY_REQUESTS" -> "Too many failed attempts. Try again later"
                                else -> "Login failed: ${exception.message}"
                            }
                        }
                        else -> "Login failed: ${exception?.message ?: "Unknown error"}"
                    }
                    onResult(false, errorMessage)
                }
            }
    }

    fun signup(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        Log.d("AuthViewModel", "Signup attempt for email: $email")

        if (email.isBlank() || password.isBlank()) {
            Log.d("AuthViewModel", "Signup failed: Empty email or password")
            onResult(false, "Email and password cannot be empty")
            return
        }

        if (password.length < 6) {
            Log.d("AuthViewModel", "Signup failed: Password too short")
            onResult(false, "Password must be at least 6 characters")
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AuthViewModel", "Signup successful for user: ${auth.currentUser?.uid}")
                    onResult(true, null)
                } else {
                    val exception = task.exception
                    Log.e("AuthViewModel", "Signup failed", exception)

                    val errorMessage = when (exception) {
                        is FirebaseAuthException -> {
                            when (exception.errorCode) {
                                "ERROR_INVALID_EMAIL" -> "Invalid email format"
                                "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists"
                                "ERROR_WEAK_PASSWORD" -> "Password is too weak"
                                "ERROR_TOO_MANY_REQUESTS" -> "Too many requests. Try again later"
                                else -> "Signup failed: ${exception.message}"
                            }
                        }
                        else -> "Signup failed: ${exception?.message ?: "Unknown error"}"
                    }
                    onResult(false, errorMessage)
                }
            }
    }
}
