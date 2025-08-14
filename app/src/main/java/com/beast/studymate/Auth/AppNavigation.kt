package com.beast.studymate.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.beast.studymate.ui.screens.FlashcardScreen
import com.beast.studymate.ui.screens.LoginScreen
import com.beast.studymate.ui.screens.SignupScreen
import com.beast.studymate.ui.screens.SplashScreen
import com.beast.studymate.ui.screens.HomeScreen

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = modifier
    ) {
        composable("splash") { SplashScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("signup") { SignupScreen(navController) }
        composable("home") { HomeScreen(navController) } // Added HomeScreen route
        composable("flashcards") { FlashcardScreen(navController) }

    }
}
