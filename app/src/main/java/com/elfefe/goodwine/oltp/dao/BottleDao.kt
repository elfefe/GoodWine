package com.elfefe.goodwine.oltp.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.elfefe.goodwine.oltp.entities.Bottle
import kotlinx.coroutines.flow.Flow

@Dao
interface BottleDao {
    @Query("SELECT * FROM bottle")
    fun getAll(): Flow<List<Bottle>>

    @Query("SELECT * FROM bottle WHERE :id = id")
    fun getById(id: Int): Flow<List<Bottle>>

    @Query("SELECT * FROM bottle ORDER BY rating ASC")
    fun getByRatinAsc(): Flow<List<Bottle>>

    @Query("SELECT * FROM bottle ORDER BY rating DESC")
    fun getByRatingDesc(): Flow<List<Bottle>>

    @Query("SELECT * FROM bottle ORDER BY date ASC")
    fun getByDateAsc(): Flow<List<Bottle>>

    @Query("SELECT * FROM bottle ORDER BY date DESC")
    fun getByDateDesc(): Flow<List<Bottle>>

    @Insert
    fun insertAll(vararg users: Bottle)

    @Delete
    fun delete(user: Bottle)
}