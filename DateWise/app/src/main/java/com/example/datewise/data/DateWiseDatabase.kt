package com.example.datewise.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Product::class, ShoppingItem::class, DonatedItem::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DateWiseDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun donatedItemDao(): DonatedItemDao

    companion object {
        @Volatile
        private var INSTANCE: DateWiseDatabase? = null

        fun getDatabase(context: Context): DateWiseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DateWiseDatabase::class.java,
                    "datewise_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
