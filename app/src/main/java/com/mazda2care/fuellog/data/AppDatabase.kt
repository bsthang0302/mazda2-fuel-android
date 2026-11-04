package com.mazda2care.fuellog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mazda2care.fuellog.model.FuelPricePeriodEntity
import com.mazda2care.fuellog.model.FuelRecordEntity

@Database(
    entities = [FuelRecordEntity::class, FuelPricePeriodEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fuelDao(): FuelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mazda2_fuel_log.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
