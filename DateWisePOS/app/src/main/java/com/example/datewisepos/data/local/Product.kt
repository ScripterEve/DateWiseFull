package com.example.datewisepos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    val name: String,
    val brand: String = "",
    val quantity: String = "",
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
