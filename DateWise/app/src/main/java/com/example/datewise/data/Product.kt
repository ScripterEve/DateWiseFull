package com.example.datewise.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class ProductCategory {
    FRIDGE, PANTRY, FREEZER
}

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val expiryDate: LocalDate,
    val category: ProductCategory = ProductCategory.FRIDGE,
    val barcode: String = "",
    val description: String = "",
    val isOpened: Boolean = false,
    val useWithinDays: Int? = null,
    val addedDate: LocalDate = LocalDate.now()
)