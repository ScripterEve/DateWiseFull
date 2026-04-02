package com.example.datewisepos.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Product?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun search(query: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long)
}
