package com.beast.studymate.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException
import com.beast.studymate.database.StudyMateDatabase
import com.beast.studymate.database.QuizQuestionHistory
import com.beast.studymate.repository.QuizHistoryRepository
import com.beast.studymate.auth.AuthViewModel
import com.beast.studymate.R

data class Flashcard(val question: String, val answer: String)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int
)

@Composable
fun FlashcardScreen(navController: NavController, authViewModel: AuthViewModel = AuthViewModel()) {
    val context = LocalContext.current
    val database = StudyMateDatabase.getDatabase(context)
    val repository = QuizHistoryRepository(database.quizHistoryDao())

    val syllabusText = navController.previousBackStackEntry
        ?.savedStateHandle?.get<String>("syllabusText") ?: ""
    val customInstruction = navController.previousBackStackEntry?.savedStateHandle?.get<String>("customInstruction") ?: ""

    var flashcards by remember { mutableStateOf<List<Flashcard>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isQuizMode by remember { mutableStateOf(false) }
    var quizQuestions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var userAnswers by remember { mutableStateOf<List<Int>>(emptyList()) }
    var showScore by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(syllabusText, customInstruction) {
        if (syllabusText.isNotBlank()) {
            scope.launch {
                try {
                    // Check network connectivity first
                    if (!isNetworkAvailable(context)) {
                        errorMessage = "No internet connection. Please check your network and try again."
                        isLoading = false
                        return@launch
                    }

                    val response = generateFlashcards(syllabusText, customInstruction, context)
                    if (response != null) {
                        flashcards = parseFlashcards(response)
                        if (flashcards.isEmpty()) {
                            errorMessage = "No flashcards could be generated from the provided text. Please try with different content."
                        }
                    } else {
                        errorMessage = "Failed to generate flashcards. Please check your internet connection and try again."
                    }
                } catch (e: Exception) {
                    errorMessage = when {
                        e.message?.contains("timeout", ignoreCase = true) == true ->
                            "Request timed out. Please check your internet connection and try again."
                        e.message?.contains("UnknownHost", ignoreCase = true) == true ->
                            "Cannot connect to AI service. Please check your internet connection."
                        else -> "Error: ${e.message}"
                    }
                } finally {
                    isLoading = false
                }
            }
        } else {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
            .padding(16.dp)
    ) {
            // Beautiful Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "StudyMate Flashcards",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "AI-Generated Study Material",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (customInstruction.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Custom Instructions",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = customInstruction,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

        Spacer(modifier = Modifier.height(8.dp))

            // Compact Mode Toggle Buttons
            if (!isLoading && flashcards.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Flashcard Mode Button
                    Button(
                        onClick = {
                            isQuizMode = false
                            showScore = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isQuizMode)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Style,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (!isQuizMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Flashcards",
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = if (!isQuizMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Quiz Mode Button
                    Button(
                        onClick = {
                            isQuizMode = true
                            showScore = false
                            quizQuestions = generateQuizQuestions(flashcards)
                            userAnswers = List(quizQuestions.size) { -1 }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isQuizMode)
                                MaterialTheme.colorScheme.secondary
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Quiz,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isQuizMode) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Quiz Mode",
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = if (isQuizMode) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

        if (isLoading) {
            // Beautiful loading state
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "🧠 AI is creating your flashcards...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This may take a few moments",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            errorMessage?.let { error ->
                // Beautiful error state
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Oops! Something went wrong",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when {
                                error.contains("timeout", ignoreCase = true) ->
                                    "Network timeout. Please check your internet connection and try again."
                                error.contains("Failed to generate", ignoreCase = true) ->
                                    "AI service is temporarily unavailable. Please try again in a moment."
                                else -> error
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                scope.launch {
                                    try {
                                        // Check network connectivity first
                                        if (!isNetworkAvailable(context)) {
                                            errorMessage = "No internet connection. Please check your network and try again."
                                            isLoading = false
                                            return@launch
                                        }

                                        val response = generateFlashcards(syllabusText, customInstruction, context)
                                        if (response != null) {
                                            flashcards = parseFlashcards(response)
                                            if (flashcards.isEmpty()) {
                                                errorMessage = "No flashcards could be generated from the provided text. Please try with different content."
                                            }
                                        } else {
                                            errorMessage = "Failed to generate flashcards. Please check your internet connection and try again."
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = when {
                                            e.message?.contains("timeout", ignoreCase = true) == true ->
                                                "Request timed out. Please check your internet connection and try again."
                                            e.message?.contains("UnknownHost", ignoreCase = true) == true ->
                                                "Cannot connect to AI service. Please check your internet connection."
                                            else -> "Error: ${e.message}"
                                        }
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Try Again")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Debug information
            Text("Number of flashcards: ${flashcards.size}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))

            if (flashcards.isEmpty() && errorMessage == null) {
                Text(
                    text = "No flashcards generated. Please try again with different text.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (isQuizMode) {
                    QuizModeContent(
                        quizQuestions = quizQuestions,
                        userAnswers = userAnswers,
                        onAnswerSelected = { questionIndex, answerIndex ->
                            userAnswers = userAnswers.toMutableList().apply {
                                set(questionIndex, answerIndex)
                            }
                        },
                        showScore = showScore,
                        onSubmit = {
                            showScore = true
                            // Save quiz history for current user
                            scope.launch {
                                try {
                                    val currentUser = authViewModel.getCurrentUser()
                                    val userId = currentUser?.uid ?: ""

                                    val quizHistoryQuestions = quizQuestions.mapIndexed { index, question ->
                                        val userAnswer = userAnswers.getOrNull(index) ?: -1
                                        QuizQuestionHistory(
                                            question = question.question,
                                            options = question.options,
                                            correctAnswerIndex = question.correctAnswerIndex,
                                            userAnswerIndex = userAnswer,
                                            isCorrect = userAnswer == question.correctAnswerIndex
                                        )
                                    }

                                    val correctAnswers = quizHistoryQuestions.count { it.isCorrect }
                                    val topics = syllabusText.take(100) + if (syllabusText.length > 100) "..." else ""

                                    // Save with user ID to ensure each user has their own history
                                    repository.insertQuiz(
                                        topics = topics,
                                        questions = quizHistoryQuestions,
                                        score = correctAnswers,
                                        totalQuestions = quizQuestions.size,
                                        userId = userId
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("FlashcardScreen", "Failed to save quiz history", e)
                                }
                            }
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(flashcards) { card ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(8.dp, RoundedCornerShape(20.dp))
                                    .animateContentSize(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Question Section - Beautiful Blue Theme
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shadow(3.dp, RoundedCornerShape(16.dp)),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(20.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(bottom = 12.dp)
                                            ) {
                                                Card(
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.primary
                                                    ),
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Quiz,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onPrimary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    "Question",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Text(
                                                card.question,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 26.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Answer Section - Beautiful Grey Theme
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shadow(3.dp, RoundedCornerShape(16.dp)),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    ) {
                                        Column(modifier = Modifier.padding(20.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(bottom = 12.dp)
                                            ) {
                                                Card(
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                                                    ),
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Lightbulb,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.surface,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    "Answer",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                card.answer,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 26.sp
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

suspend fun generateFlashcards(syllabusText: String, customInstruction: String = "", context: android.content.Context): String? {
    val apiKey = context.getString(R.string.gemini_api_key)
    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=$apiKey"

    // Configure OkHttpClient with longer timeouts and retry logic
    val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // Correct Gemini API request structure
    val jsonBody = JSONObject().apply {
        put("contents", org.json.JSONArray().apply {
            put(JSONObject().apply {
                put("parts", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        val basePrompt = """
                            Generate flashcards from the following text. Format your response as JSON with this structure:
                            {
                              "flashcards": [
                                {"question": "Question 1", "answer": "Answer 1"},
                                {"question": "Question 2", "answer": "Answer 2"}
                              ]
                            }

                            ${if (customInstruction.isNotBlank()) {
                                "CUSTOM INSTRUCTIONS: $customInstruction\n\nPlease follow the above instructions when creating the flashcards.\n\n"
                            } else {
                                "Create standard Q&A flashcards covering the key concepts, definitions, and important points.\n\n"
                            }}

                            Text to generate flashcards from:
                            $syllabusText
                        """.trimIndent()

                        put("text", basePrompt)
                    })
                })
            })
        })
        put("generationConfig", JSONObject().apply {
            put("temperature", 0.7)
            put("candidateCount", 1)
            put("maxOutputTokens", 2048)
        })
    }

    val requestBody = RequestBody.create("application/json".toMediaType(), jsonBody.toString())

    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .addHeader("Content-Type", "application/json")
        .build()

    return withContext(Dispatchers.IO) {
        var lastException: Exception? = null

        // Retry logic with exponential backoff
        repeat(3) { attempt ->
            try {
                Log.d("FlashcardScreen", "API request attempt ${attempt + 1}")

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    Log.d("FlashcardScreen", "API Response successful on attempt ${attempt + 1}")
                    return@withContext responseBody
                } else {
                    Log.e("FlashcardScreen", "API Error on attempt ${attempt + 1}: ${response.code} ${response.message}")
                    Log.e("FlashcardScreen", "Response body: $responseBody")

                    // If it's a client error (4xx), don't retry
                    if (response.code in 400..499) {
                        return@withContext null
                    }
                }
            } catch (e: Exception) {
                lastException = e
                Log.e("FlashcardScreen", "API request failed on attempt ${attempt + 1}", e)

                // Wait before retrying (exponential backoff)
                if (attempt < 2) {
                    try {
                        kotlinx.coroutines.delay((1000L * (attempt + 1) * (attempt + 1))) // 1s, 4s, 9s
                    } catch (ignored: Exception) {}
                }
            }
        }

        Log.e("FlashcardScreen", "All API retry attempts failed", lastException)
        null
    }
}

fun parseFlashcards(response: String): List<Flashcard> {
    val flashcards = mutableListOf<Flashcard>()

    try {
        val jsonResponse = JSONObject(response)
        val candidates = jsonResponse.getJSONArray("candidates")

        if (candidates.length() > 0) {
            val candidate = candidates.getJSONObject(0)
            val content = candidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")

            if (parts.length() > 0) {
                val text = parts.getJSONObject(0).getString("text")
                Log.d("FlashcardScreen", "Generated text: $text")

                // Try to parse as JSON first
                try {
                    // Clean the text - remove markdown code blocks if present
                    val cleanedText = text.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()

                    Log.d("FlashcardScreen", "Cleaned text: $cleanedText")

                    val flashcardJson = JSONObject(cleanedText)
                    val flashcardArray = flashcardJson.getJSONArray("flashcards")

                    for (i in 0 until flashcardArray.length()) {
                        val flashcard = flashcardArray.getJSONObject(i)
                        val question = flashcard.getString("question")
                        val answer = flashcard.getString("answer")
                        flashcards.add(Flashcard(question, answer))
                    }

                    Log.d("FlashcardScreen", "Successfully parsed ${flashcards.size} flashcards")
                } catch (jsonException: Exception) {
                    // Fallback: parse as plain text format
                    Log.d("FlashcardScreen", "JSON parsing failed: ${jsonException.message}, trying text parsing")
                    parseTextFormat(text, flashcards)
                }
            }
        }
    } catch (e: Exception) {
        Log.e("FlashcardScreen", "Failed to parse flashcards", e)
    }

    return flashcards
}

private fun parseTextFormat(text: String, flashcards: MutableList<Flashcard>) {
    // Handle various text formats
    val lines = text.split("\n")
    var currentQuestion = ""
    var currentAnswer = ""
    var isAnswer = false

    for (line in lines) {
        val trimmedLine = line.trim()
        if (trimmedLine.isEmpty()) continue

        when {
            trimmedLine.startsWith("Q:", ignoreCase = true) ||
            trimmedLine.startsWith("Question:", ignoreCase = true) -> {
                // Save previous flashcard if exists
                if (currentQuestion.isNotEmpty() && currentAnswer.isNotEmpty()) {
                    flashcards.add(Flashcard(currentQuestion, currentAnswer))
                }
                currentQuestion = trimmedLine.removePrefix("Q:").removePrefix("Question:").trim()
                currentAnswer = ""
                isAnswer = false
            }
            trimmedLine.startsWith("A:", ignoreCase = true) ||
            trimmedLine.startsWith("Answer:", ignoreCase = true) -> {
                currentAnswer = trimmedLine.removePrefix("A:").removePrefix("Answer:").trim()
                isAnswer = true
            }
            isAnswer && currentAnswer.isNotEmpty() -> {
                currentAnswer += " " + trimmedLine
            }
            currentQuestion.isNotEmpty() && currentAnswer.isEmpty() -> {
                currentAnswer = trimmedLine
                isAnswer = true
            }
        }
    }

    // Add the last flashcard
    if (currentQuestion.isNotEmpty() && currentAnswer.isNotEmpty()) {
        flashcards.add(Flashcard(currentQuestion, currentAnswer))
    }
}

fun generateQuizQuestions(flashcards: List<Flashcard>): List<QuizQuestion> {
    if (flashcards.size < 4) return emptyList() // Need at least 4 flashcards for multiple choice

    val selectedFlashcards = flashcards.shuffled().take(minOf(10, flashcards.size))
    val allAnswers = flashcards.map { it.answer }

    return selectedFlashcards.map { flashcard ->
        val correctAnswer = flashcard.answer
        val incorrectAnswers = allAnswers.filter { it != correctAnswer }.shuffled().take(3)
        val allOptions = (listOf(correctAnswer) + incorrectAnswers).shuffled()
        val correctIndex = allOptions.indexOf(correctAnswer)

        QuizQuestion(
            question = flashcard.question,
            options = allOptions,
            correctAnswerIndex = correctIndex
        )
    }
}

@Composable
fun QuizModeContent(
    quizQuestions: List<QuizQuestion>,
    userAnswers: List<Int>,
    onAnswerSelected: (Int, Int) -> Unit,
    showScore: Boolean,
    onSubmit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (showScore) {
            val correctAnswers = quizQuestions.indices.count { index ->
                userAnswers.getOrNull(index) == quizQuestions[index].correctAnswerIndex
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .shadow(16.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        correctAnswers.toFloat() / quizQuestions.size >= 0.8f ->
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                        correctAnswers.toFloat() / quizQuestions.size >= 0.6f ->
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                        else ->
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val percentage = (correctAnswers.toFloat() / quizQuestions.size * 100).toInt()

                    Icon(
                        when {
                            percentage >= 80 -> Icons.Default.EmojiEvents
                            percentage >= 60 -> Icons.Default.ThumbUp
                            else -> Icons.Default.Refresh
                        },
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = when {
                            percentage >= 80 -> MaterialTheme.colorScheme.primary
                            percentage >= 60 -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = when {
                            percentage >= 80 -> "Excellent! 🎉"
                            percentage >= 60 -> "Good Job! 👍"
                            else -> "Keep Practicing! 💪"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            percentage >= 80 -> MaterialTheme.colorScheme.onPrimaryContainer
                            percentage >= 60 -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onErrorContainer
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "$correctAnswers / ${quizQuestions.size}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            percentage >= 80 -> MaterialTheme.colorScheme.onPrimaryContainer
                            percentage >= 60 -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onErrorContainer
                        }
                    )

                    Text(
                        text = "$percentage% Correct",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            percentage >= 80 -> MaterialTheme.colorScheme.onPrimaryContainer
                            percentage >= 60 -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onErrorContainer
                        }.copy(alpha = 0.8f)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
            items(quizQuestions.size) { questionIndex ->
                val question = quizQuestions[questionIndex]
                val selectedAnswer = userAnswers.getOrNull(questionIndex) ?: -1

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Question Header
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Quiz,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Question ${questionIndex + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = question.question,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 24.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        question.options.forEachIndexed { optionIndex, option ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .selectable(
                                        selected = selectedAnswer == optionIndex,
                                        onClick = {
                                            if (!showScore) {
                                                onAnswerSelected(questionIndex, optionIndex)
                                            }
                                        },
                                        role = Role.RadioButton
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        showScore && optionIndex == question.correctAnswerIndex ->
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                        showScore && selectedAnswer == optionIndex && optionIndex != question.correctAnswerIndex ->
                                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                        selectedAnswer == optionIndex ->
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                        else ->
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    }
                                ),
                                border = if (selectedAnswer == optionIndex) {
                                    BorderStroke(
                                        2.dp,
                                        if (showScore && optionIndex == question.correctAnswerIndex)
                                            MaterialTheme.colorScheme.primary
                                        else if (showScore && optionIndex != question.correctAnswerIndex)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.secondary
                                    )
                                } else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedAnswer == optionIndex,
                                        onClick = null,
                                        enabled = !showScore,
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = when {
                                                showScore && optionIndex == question.correctAnswerIndex -> MaterialTheme.colorScheme.primary
                                                showScore && selectedAnswer == optionIndex && optionIndex != question.correctAnswerIndex -> MaterialTheme.colorScheme.error
                                                else -> MaterialTheme.colorScheme.secondary
                                            }
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = when {
                                            showScore && optionIndex == question.correctAnswerIndex -> MaterialTheme.colorScheme.primary
                                            showScore && selectedAnswer == optionIndex && optionIndex != question.correctAnswerIndex -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                        lineHeight = 20.sp
                                    )

                                    Spacer(modifier = Modifier.weight(1f))

                                    // Show icons for correct/incorrect answers when quiz is complete
                                    if (showScore) {
                                        when {
                                            optionIndex == question.correctAnswerIndex -> {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Correct",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            selectedAnswer == optionIndex && optionIndex != question.correctAnswerIndex -> {
                                                Icon(
                                                    Icons.Default.Cancel,
                                                    contentDescription = "Incorrect",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
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

        if (!showScore && quizQuestions.isNotEmpty()) {
            val allAnswered = userAnswers.size == quizQuestions.size && userAnswers.all { it != -1 }

            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                enabled = allAnswered
            ) {
                Text("Submit Quiz")
            }
        }
    }
}

// Network connectivity check function
fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

    return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
           networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
