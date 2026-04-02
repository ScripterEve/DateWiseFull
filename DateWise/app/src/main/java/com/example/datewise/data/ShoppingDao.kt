package com.example.datewise.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_items ORDER BY isChecked ASC, addedDate DESC")
    fun getAll(): Flow<List<ShoppingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ShoppingItem): Long

    @Delete
    suspend fun delete(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("UPDATE shopping_items SET isChecked = NOT isChecked WHERE id = :id")
    suspend fun toggleChecked(id: Int)

    @Query("DELETE FROM shopping_items WHERE isChecked = 1")
    suspend fun clearChecked()
}
