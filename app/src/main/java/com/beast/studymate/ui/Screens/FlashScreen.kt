package com.beast.studymate.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

data class Flashcard(val question: String, val answer: String)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int
)

@Composable
fun FlashcardScreen(navController: NavController) {
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
                    val response = generateFlashcards(syllabusText, customInstruction)
                    if (response != null) {
                        flashcards = parseFlashcards(response)
                    } else {
                        errorMessage = "Failed to generate flashcards."
                    }
                } catch (e: Exception) {
                    errorMessage = "Error: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        } else {
            isLoading = false
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text("Flashcards", style = MaterialTheme.typography.headlineMedium)

        if (customInstruction.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Custom Instructions:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = customInstruction,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mode toggle buttons
        if (!isLoading && flashcards.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        isQuizMode = false
                        showScore = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = if (!isQuizMode) ButtonDefaults.buttonColors()
                            else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("Flashcard Mode")
                }

                Button(
                    onClick = {
                        isQuizMode = true
                        showScore = false
                        quizQuestions = generateQuizQuestions(flashcards)
                        userAnswers = List(quizQuestions.size) { -1 }
                    },
                    modifier = Modifier.weight(1f),
                    colors = if (isQuizMode) ButtonDefaults.buttonColors()
                            else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("Quiz Mode")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isLoading) {
            CircularProgressIndicator()
            Text("Generating flashcards...", style = MaterialTheme.typography.bodyMedium)
        } else {
            errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
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
                        onSubmit = { showScore = true }
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(flashcards) { card ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Q: ${card.question}", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("A: ${card.answer}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

suspend fun generateFlashcards(syllabusText: String, customInstruction: String = ""): String? {
    val apiKey = "AIzaSyAQqRuPaHIr_v7L9X5TJM16N2jyEpwdWHs" // Replace with your Gemini API key
    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=$apiKey"

    val client = OkHttpClient()

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
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                Log.d("FlashcardScreen", "API Response: $responseBody")
                responseBody
            } else {
                Log.e("FlashcardScreen", "API Error: ${response.code} ${response.message}")
                Log.e("FlashcardScreen", "Response body: $responseBody")
                null
            }
        } catch (e: IOException) {
            Log.e("FlashcardScreen", "API request failed", e)
            null
        }
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
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Quiz Complete!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your Score: $correctAnswers / ${quizQuestions.size}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    val percentage = (correctAnswers.toFloat() / quizQuestions.size * 100).toInt()
                    Text(
                        text = "($percentage%)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(quizQuestions.size) { questionIndex ->
                val question = quizQuestions[questionIndex]
                val selectedAnswer = userAnswers.getOrNull(questionIndex) ?: -1

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Question ${questionIndex + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = question.question,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        question.options.forEachIndexed { optionIndex, option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = selectedAnswer == optionIndex,
                                        onClick = {
                                            if (!showScore) {
                                                onAnswerSelected(questionIndex, optionIndex)
                                            }
                                        },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedAnswer == optionIndex,
                                    onClick = null,
                                    enabled = !showScore
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (showScore) {
                                        when {
                                            optionIndex == question.correctAnswerIndex -> MaterialTheme.colorScheme.primary
                                            selectedAnswer == optionIndex && optionIndex != question.correctAnswerIndex -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
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
