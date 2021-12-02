package com.theleafapps.pro.starrun.repository

import com.theleafapps.pro.starrun.db.Run
import com.theleafapps.pro.starrun.db.RunDAO
import javax.inject.Inject

class MainRepository @Inject constructor(
    val runDAO: RunDAO
) {
    suspend fun insertRun(run: Run) = runDAO.insertRun(run)

    suspend fun deleteRun(run: Run) = runDAO.deleteRun(run)

    fun getAllRunsSortedByDate() = runDAO.getAllRunsSortedByDate()

    fun getAllRunsSortedByDistance() = runDAO.getAllRunsSortedByDistanceInMeters()

    fun getAllRunsSortedByTimeInMillis() = runDAO.getAllRunsSortedByTimeInMillis()

    fun getAllRunsSortedByAvgSpeed() = runDAO.getAllRunsSortedByAvgSpeedInKMH()

    fun getAllRunsSortedByCaloriesBurnt() = runDAO.getAllRunsSortedByCaloriesBurned()

    fun getTotalAvgSpeed() = runDAO.getTotalAvgSpeedInKMH()

    fun getTotalDistance() = runDAO.getTotalDistanceInMeters()

    fun getTotalCaloriesBurnt() = runDAO.getTotalCaloriesBurned()

    fun getTotalTimeInMillis() = runDAO.getTotalTimeInMillis()
}