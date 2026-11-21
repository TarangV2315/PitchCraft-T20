package com.example.data.local

import com.example.model.CareerState
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CareerRepository(private val careerDao: CareerDao) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(CareerState::class.java)

    val careerSummaries: Flow<List<CareerSummary>> = careerDao.observeCareerSummaries()

    fun observeCareer(careerId: String): Flow<CareerState?> =
        careerDao.observeCareerById(careerId).map { entity ->
            entity?.let { deserialize(it.payloadJson) }
        }

    suspend fun loadCareer(careerId: String): CareerState? = withContext(Dispatchers.IO) {
        careerDao.getCareerById(careerId)?.let { deserialize(it.payloadJson) }
    }

    suspend fun saveCareer(state: CareerState) = withContext(Dispatchers.IO) {
        val stamped = state.copy(updatedAtMillis = System.currentTimeMillis())
        val json = adapter.toJson(stamped)
        val entity = CareerEntity(
            careerId = stamped.careerId,
            clubName = stamped.clubName,
            clubShortCode = stamped.clubShortCode,
            managerName = stamped.managerName,
            season = stamped.season,
            week = stamped.week,
            divisionTier = stamped.divisionTier,
            bankBalance = stamped.bankBalance,
            trophiesWon = stamped.trophiesWon,
            updatedAtMillis = stamped.updatedAtMillis,
            payloadJson = json
        )
        careerDao.upsertCareer(entity)
    }

    suspend fun deleteCareer(careerId: String) = withContext(Dispatchers.IO) {
        careerDao.deleteCareerById(careerId)
    }

    private fun deserialize(json: String): CareerState? = runCatching {
        adapter.fromJson(json)
    }.getOrNull()
}
