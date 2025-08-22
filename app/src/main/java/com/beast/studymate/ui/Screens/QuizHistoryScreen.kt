package com.beast.studymate.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import com.beast.studymate.database.QuizHistoryEntity
import com.beast.studymate.database.StudyMateDatabase
import com.beast.studymate.repository.QuizHistoryRepository
import com.beast.studymate.auth.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizHistoryScreen(navController: NavController, authViewModel: AuthViewModel = AuthViewModel()) {
    val context = LocalContext.current
    val database = StudyMateDatabase.getDatabase(context)
    val repository = QuizHistoryRepository(database.quizHistoryDao())
    val scope = rememberCoroutineScope()

    // Get current user ID
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid ?: ""
    val userName = currentUser?.displayName ?: "User"

    // State for clear history dialog
    var showClearDialog by remember { mutableStateOf(false) }

    // Get quiz history for current user only
    val quizHistory by if (userId.isNotEmpty()) {
        repository.getQuizHistoryByUser(userId).collectAsState(initial = emptyList())
    } else {
        repository.getAllQuizHistory().collectAsState(initial = emptyList())
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp),
            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.navigateUp() }
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = "Quiz History",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$userName's Results",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                // Clear History Button
                if (quizHistory.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearDialog = true }
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
        
        // Content
        if (quizHistory.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Quiz,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No quiz history yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Take your first quiz to see your personal results here!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // Quiz History List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(quizHistory) { quiz ->
                    QuizHistoryCard(
                        quiz = quiz,
                        onClick = {
                            navController.navigate("quiz_detail/${quiz.id}")
                        },
                        onDelete = {
                            // TODO: Implement delete functionality
                        }
                    )
                }
            }
        }
    }

    // Clear History Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "Clear Quiz History",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to clear all your quiz history? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            if (userId.isNotEmpty()) {
                                repository.deleteQuizHistoryByUser(userId)
                            }
                            showClearDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun QuizHistoryCard(
    quiz: QuizHistoryEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    val date = dateFormat.format(Date(quiz.timestamp))
    val percentage = (quiz.score.toFloat() / quiz.totalQuestions * 100).toInt()
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quiz.topics,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Score Badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            percentage >= 80 -> MaterialTheme.colorScheme.primaryContainer
                            percentage >= 60 -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.errorContainer
                        }
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "${quiz.score}/${quiz.totalQuestions}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            percentage >= 80 -> MaterialTheme.colorScheme.onPrimaryContainer
                            percentage >= 60 -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress Bar
            LinearProgressIndicator(
                progress = quiz.score.toFloat() / quiz.totalQuestions,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = when {
                    percentage >= 80 -> MaterialTheme.colorScheme.primary
                    percentage >= 60 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.error
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "$percentage% correct",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
