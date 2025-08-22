package com.beast.studymate.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_history")
data class QuizHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val topics: String, // JSON string of topics/subject
    val questionsJson: String, // JSON string of questions and answers
    val score: Int,
    val totalQuestions: Int,
    val userId: String? = null // Optional user ID for multi-user support
)

data class QuizHistoryItem(
    val id: Long,
    val timestamp: Long,
    val topics: String,
    val score: Int,
    val totalQuestions: Int,
    val questions: List<QuizQuestionHistory>
)

data class QuizQuestionHistory(
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val userAnswerIndex: Int,
    val isCorrect: Boolean
)
