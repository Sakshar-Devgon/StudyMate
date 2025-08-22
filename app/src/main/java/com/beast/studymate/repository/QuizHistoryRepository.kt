package com.beast.studymate.repository

import com.beast.studymate.database.QuizHistoryDao
import com.beast.studymate.database.QuizHistoryEntity
import com.beast.studymate.database.QuizHistoryItem
import com.beast.studymate.database.QuizQuestionHistory
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class QuizHistoryRepository(private val quizHistoryDao: QuizHistoryDao) {
    
    fun getAllQuizHistory(): Flow<List<QuizHistoryEntity>> = quizHistoryDao.getAllQuizHistory()
    
    fun getQuizHistoryByUser(userId: String): Flow<List<QuizHistoryEntity>> = 
        quizHistoryDao.getQuizHistoryByUser(userId)
    
    suspend fun getQuizById(id: Long): QuizHistoryItem? {
        val entity = quizHistoryDao.getQuizById(id) ?: return null
        return entity.toQuizHistoryItem()
    }
    
    suspend fun insertQuiz(
        topics: String,
        questions: List<QuizQuestionHistory>,
        score: Int,
        totalQuestions: Int,
        userId: String? = null
    ): Long {
        val questionsJson = questionsToJson(questions)
        val entity = QuizHistoryEntity(
            timestamp = System.currentTimeMillis(),
            topics = topics,
            questionsJson = questionsJson,
            score = score,
            totalQuestions = totalQuestions,
            userId = userId
        )
        return quizHistoryDao.insertQuiz(entity)
    }
    
    suspend fun deleteQuiz(id: Long) {
        quizHistoryDao.deleteQuizById(id)
    }
    
    suspend fun deleteAllQuizHistory() {
        quizHistoryDao.deleteAllQuizHistory()
    }

    suspend fun deleteQuizHistoryByUser(userId: String) {
        quizHistoryDao.deleteQuizHistoryByUser(userId)
    }
    
    private fun questionsToJson(questions: List<QuizQuestionHistory>): String {
        val jsonArray = JSONArray()
        questions.forEach { question ->
            val questionObj = JSONObject().apply {
                put("question", question.question)
                put("options", JSONArray(question.options))
                put("correctAnswerIndex", question.correctAnswerIndex)
                put("userAnswerIndex", question.userAnswerIndex)
                put("isCorrect", question.isCorrect)
            }
            jsonArray.put(questionObj)
        }
        return jsonArray.toString()
    }
    
    private fun QuizHistoryEntity.toQuizHistoryItem(): QuizHistoryItem {
        val questions = jsonToQuestions(this.questionsJson)
        return QuizHistoryItem(
            id = this.id,
            timestamp = this.timestamp,
            topics = this.topics,
            score = this.score,
            totalQuestions = this.totalQuestions,
            questions = questions
        )
    }
    
    private fun jsonToQuestions(json: String): List<QuizQuestionHistory> {
        val questions = mutableListOf<QuizQuestionHistory>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val questionObj = jsonArray.getJSONObject(i)
                val optionsArray = questionObj.getJSONArray("options")
                val options = mutableListOf<String>()
                for (j in 0 until optionsArray.length()) {
                    options.add(optionsArray.getString(j))
                }
                
                questions.add(
                    QuizQuestionHistory(
                        question = questionObj.getString("question"),
                        options = options,
                        correctAnswerIndex = questionObj.getInt("correctAnswerIndex"),
                        userAnswerIndex = questionObj.getInt("userAnswerIndex"),
                        isCorrect = questionObj.getBoolean("isCorrect")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return questions
    }
}
