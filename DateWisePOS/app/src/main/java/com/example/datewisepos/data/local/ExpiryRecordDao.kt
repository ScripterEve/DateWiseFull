package com.example.datewisepos.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpiryRecordDao {

    @Query("SELECT * FROM expiry_records WHERE productId = :productId ORDER BY expiryDate ASC")
    fun getForProduct(productId: Long): Flow<List<ExpiryRecord>>

    @Query("SELECT * FROM expiry_records WHERE productId = :productId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestForProduct(productId: Long): ExpiryRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ExpiryRecord): Long

    @Delete
    suspend fun delete(record: ExpiryRecord)

    @Query("DELETE FROM expiry_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM expiry_records ORDER BY expiryDate ASC")
    fun getAll(): Flow<List<ExpiryRecord>>
}
