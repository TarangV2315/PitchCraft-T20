package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CareerEntity::class], version = 1, exportSchema = false)
abstract class PitchCraftDatabase : RoomDatabase() {
    abstract fun careerDao(): CareerDao

    companion object {
        @Volatile
        private var INSTANCE: PitchCraftDatabase? = null

        fun getInstance(context: Context): PitchCraftDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PitchCraftDatabase::class.java,
                    "pitchcraft_t20_careers.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
