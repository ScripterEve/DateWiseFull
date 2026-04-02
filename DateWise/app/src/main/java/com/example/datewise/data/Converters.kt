package com.example.datewise.data

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }

    @TypeConverter
    fun toLocalDate(epochDay: Long?): LocalDate? {
        return epochDay?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun fromProductCategory(category: ProductCategory?): String? {
        return category?.name
    }

    @TypeConverter
    fun toProductCategory(name: String?): ProductCategory? {
        return name?.let { ProductCategory.valueOf(it) }
    }
}
