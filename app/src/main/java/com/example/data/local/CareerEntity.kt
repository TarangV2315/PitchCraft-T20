package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "careers")
data class CareerEntity(
    @PrimaryKey val careerId: String,
    val clubName: String,
    val clubShortCode: String,
    val managerName: String,
    val season: Int,
    val week: Int,
    val divisionTier: Int,
    val bankBalance: Int,
    val trophiesWon: Int,
    val updatedAtMillis: Long,
    val payloadJson: String
)

data class CareerSummary(
    val careerId: String,
    val clubName: String,
    val clubShortCode: String,
    val managerName: String,
    val season: Int,
    val week: Int,
    val divisionTier: Int,
    val bankBalance: Int,
    val trophiesWon: Int,
    val updatedAtMillis: Long
)
