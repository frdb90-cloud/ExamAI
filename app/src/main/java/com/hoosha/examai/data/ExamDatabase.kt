package com.hoosha.examai.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StudySourceEntity::class,
        SourceChunkEntity::class,
        ExamSessionEntity::class,
        ExamAnswerEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class ExamDatabase : RoomDatabase() {

    abstract fun examDao(): ExamDao

    companion object {
        @Volatile
        private var instance: ExamDatabase? = null

        fun getInstance(context: Context): ExamDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ExamDatabase::class.java,
                    "exam_ai_offline.db"
                ).build().also {
                    instance = it
                }
            }
        }
    }
}