package com.example.datewisepos.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expiry_records",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productId")]
)
data class ExpiryRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val expiryDate: Long,
    val createdAt: Long = System.currentTimeMillis()
)
