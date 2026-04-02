package com.example.datewise.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY expiryDate ASC")
    fun getAll(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE category = :category ORDER BY expiryDate ASC")
    fun getByCategory(category: ProductCategory): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Int): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Int)
}
