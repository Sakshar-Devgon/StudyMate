package com.beast.studymate.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.beast.studymate.ui.screens.FlashcardScreen
import com.beast.studymate.ui.screens.LoginScreen
import com.beast.studymate.ui.screens.SignupScreen
import com.beast.studymate.ui.screens.SplashScreen
import com.beast.studymate.ui.screens.HomeScreen
import com.beast.studymate.ui.screens.QuizHistoryScreen
import com.beast.studymate.ui.screens.QuizDetailScreen
import com.beast.studymate.auth.AuthViewModel

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    onThemeChange: (Boolean) -> Unit = {}
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = modifier
    ) {
        composable("splash") { SplashScreen(navController, authViewModel) }
        composable("login") { LoginScreen(navController, authViewModel) }
        composable("signup") { SignupScreen(navController, authViewModel) }
        composable("home") {
            HomeScreen(
                navController = navController,
                authViewModel = authViewModel,
                isDarkTheme = isDarkTheme,
                onThemeChange = onThemeChange
            )
        }
        composable("flashcards") { FlashcardScreen(navController, authViewModel) }
        composable("quiz_history") { QuizHistoryScreen(navController, authViewModel) }
        composable(
            "quiz_detail/{quizId}",
            arguments = listOf(navArgument("quizId") { type = NavType.LongType })
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getLong("quizId") ?: 0L
            QuizDetailScreen(navController, quizId)
        }

    }
}
