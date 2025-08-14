package com.beast.studymate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var syllabusText by remember { mutableStateOf("") }
    var customInstruction by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to StudyMate!",
            fontSize = 24.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Enter your syllabus below:",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = syllabusText,
            onValueChange = { syllabusText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            placeholder = { Text("Type or paste your syllabus here...") },
            maxLines = 15,
            singleLine = false,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Custom Instructions (Optional):",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = customInstruction,
            onValueChange = { customInstruction = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            placeholder = { Text("e.g., 'Create fill-in-the-blank questions', 'Focus on definitions', 'Make it challenging'...") },
            maxLines = 5,
            singleLine = false,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (syllabusText.isBlank()) {
                    errorMessage = "Please enter some text!"
                    return@Button
                }

                isProcessing = true
                errorMessage = ""
                scope.launch {
                    // Here you can send syllabusText to your AI flashcard generator
                    // For now, we just simulate processing
                    kotlinx.coroutines.delay(500) // simulate network/API delay
                    isProcessing = false

                    // Navigate to FlashcardScreen and pass syllabus text and custom instruction
                    navController.currentBackStackEntry?.savedStateHandle?.set("syllabusText", syllabusText)
                    navController.currentBackStackEntry?.savedStateHandle?.set("customInstruction", customInstruction)
                    navController.navigate("flashcards")
                }
            },
            enabled = !isProcessing
        ) {
            Text(if (isProcessing) "Processing..." else "Generate Flashcards")
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }

        if (syllabusText.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Preview:\n${syllabusText.take(300)}...",
                fontSize = 14.sp
            )
        }
    }
}
