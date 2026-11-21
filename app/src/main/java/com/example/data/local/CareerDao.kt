package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CareerDao {
    @Query(
        "SELECT careerId, clubName, clubShortCode, managerName, season, week, divisionTier, bankBalance, trophiesWon, updatedAtMillis " +
            "FROM careers ORDER BY updatedAtMillis DESC"
    )
    fun observeCareerSummaries(): Flow<List<CareerSummary>>

    @Query("SELECT * FROM careers WHERE careerId = :careerId LIMIT 1")
    fun observeCareerById(careerId: String): Flow<CareerEntity?>

    @Query("SELECT * FROM careers WHERE careerId = :careerId LIMIT 1")
    suspend fun getCareerById(careerId: String): CareerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCareer(career: CareerEntity)

    @Query("DELETE FROM careers WHERE careerId = :careerId")
    suspend fun deleteCareerById(careerId: String)
}
