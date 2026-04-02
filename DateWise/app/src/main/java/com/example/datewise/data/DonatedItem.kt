package com.example.datewise.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "donated_items")
data class DonatedItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val barcode: String = "",
    val description: String = "",
    val category: ProductCategory = ProductCategory.FRIDGE,
    val donatedDate: LocalDate = LocalDate.now()
)
