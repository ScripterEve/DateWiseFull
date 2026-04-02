package com.example.datewise.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val isChecked: Boolean = false,
    val addedDate: LocalDate = LocalDate.now()
)
