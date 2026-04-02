package com.example.datewise.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DonatedItemDao {
    @Query("SELECT * FROM donated_items ORDER BY donatedDate DESC")
    fun getAll(): Flow<List<DonatedItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DonatedItem)

    @Query("DELETE FROM donated_items WHERE id = :id")
    suspend fun deleteById(id: Int)
}
