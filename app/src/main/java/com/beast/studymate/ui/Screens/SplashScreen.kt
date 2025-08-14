package com.beast.studymate.ui.screens

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun SplashScreen(navController: NavController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Flashcard App", fontSize = 32.sp)
    }

    // Navigate to Login after 2 seconds
    Handler(Looper.getMainLooper()).postDelayed({
        navController.navigate("login") {
            popUpTo("splash") { inclusive = true }
        }
    }, 2000)
}
