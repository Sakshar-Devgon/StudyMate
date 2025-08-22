package com.beast.studymate.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [QuizHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StudyMateDatabase : RoomDatabase() {
    
    abstract fun quizHistoryDao(): QuizHistoryDao
    
    companion object {
        @Volatile
        private var INSTANCE: StudyMateDatabase? = null
        
        fun getDatabase(context: Context): StudyMateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyMateDatabase::class.java,
                    "studymate_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
