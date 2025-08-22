package com.beast.studymate.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizHistoryDao {
    
    @Query("SELECT * FROM quiz_history ORDER BY timestamp DESC")
    fun getAllQuizHistory(): Flow<List<QuizHistoryEntity>>
    
    @Query("SELECT * FROM quiz_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getQuizHistoryByUser(userId: String): Flow<List<QuizHistoryEntity>>
    
    @Query("SELECT * FROM quiz_history WHERE id = :id")
    suspend fun getQuizById(id: Long): QuizHistoryEntity?
    
    @Insert
    suspend fun insertQuiz(quiz: QuizHistoryEntity): Long
    
    @Delete
    suspend fun deleteQuiz(quiz: QuizHistoryEntity)
    
    @Query("DELETE FROM quiz_history WHERE id = :id")
    suspend fun deleteQuizById(id: Long)
    
    @Query("DELETE FROM quiz_history")
    suspend fun deleteAllQuizHistory()

    @Query("DELETE FROM quiz_history WHERE userId = :userId")
    suspend fun deleteQuizHistoryByUser(userId: String)
}
