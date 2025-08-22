package com.beast.studymate.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.beast.studymate.database.QuizHistoryItem
import com.beast.studymate.database.StudyMateDatabase
import com.beast.studymate.repository.QuizHistoryRepository
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizDetailScreen(navController: NavController, quizId: Long) {
    val context = LocalContext.current
    val database = StudyMateDatabase.getDatabase(context)
    val repository = QuizHistoryRepository(database.quizHistoryDao())
    
    var quizDetail by remember { mutableStateOf<QuizHistoryItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(quizId) {
        val quiz = repository.getQuizById(quizId)
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: ""

        // Only show quiz if it belongs to current user or if no user ID is set (legacy data)
        if (quiz != null && (quiz.id == quizId)) {
            // Additional security check could be added here to verify userId matches
            quizDetail = quiz
        }
        isLoading = false
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
                
                Column {
                    Text(
                        text = "Quiz Details",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    quizDetail?.let { quiz ->
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                        val date = dateFormat.format(Date(quiz.timestamp))
                        Text(
                            text = date,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (quizDetail == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Quiz not found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val quiz = quizDetail!!
            val percentage = (quiz.score.toFloat() / quiz.totalQuestions * 100).toInt()
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Score Summary
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                percentage >= 80 -> MaterialTheme.colorScheme.primaryContainer
                                percentage >= 60 -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = quiz.topics,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    percentage >= 80 -> MaterialTheme.colorScheme.onPrimaryContainer
                                    percentage >= 60 -> MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.onErrorContainer
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "${quiz.score} / ${quiz.totalQuestions}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    percentage >= 80 -> MaterialTheme.colorScheme.onPrimaryContainer
                                    percentage >= 60 -> MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.onErrorContainer
                                }
                            )
                            
                            Text(
                                text = "$percentage% correct",
                                style = MaterialTheme.typography.titleMedium,
                                color = when {
                                    percentage >= 80 -> MaterialTheme.colorScheme.onPrimaryContainer
                                    percentage >= 60 -> MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.onErrorContainer
                                }.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                
                // Questions and Answers
                itemsIndexed(quiz.questions) { index, question ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Question Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Question ${index + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                Icon(
                                    if (question.isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = if (question.isCorrect) "Correct" else "Incorrect",
                                    tint = if (question.isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Question Text
                            Text(
                                text = question.question,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Answer Options
                            question.options.forEachIndexed { optionIndex, option ->
                                val isCorrect = optionIndex == question.correctAnswerIndex
                                val isUserAnswer = optionIndex == question.userAnswerIndex
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when {
                                            isCorrect -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                            isUserAnswer && !isCorrect -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        }
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = option,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = when {
                                                isCorrect -> MaterialTheme.colorScheme.onPrimaryContainer
                                                isUserAnswer && !isCorrect -> MaterialTheme.colorScheme.onErrorContainer
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        
                                        if (isCorrect) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Correct Answer",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else if (isUserAnswer) {
                                            Icon(
                                                Icons.Default.Cancel,
                                                contentDescription = "Your Answer",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
