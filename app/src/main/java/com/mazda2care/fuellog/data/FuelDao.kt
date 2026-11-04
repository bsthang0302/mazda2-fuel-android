package com.mazda2care.fuellog.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mazda2care.fuellog.model.FuelPricePeriodEntity
import com.mazda2care.fuellog.model.FuelRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelDao {
    @Query("SELECT * FROM fuel_records ORDER BY epochMillis DESC, odo DESC")
    fun observeFuelRecords(): Flow<List<FuelRecordEntity>>

    @Query("SELECT * FROM fuel_records ORDER BY epochMillis DESC, odo DESC")
    suspend fun getAllFuelRecords(): List<FuelRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelRecord(record: FuelRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFuelRecords(records: List<FuelRecordEntity>)

    @Delete
    suspend fun deleteFuelRecord(record: FuelRecordEntity)

    @Query("DELETE FROM fuel_records")
    suspend fun clearAllFuelRecords()

    @Query("SELECT * FROM fuel_price_periods ORDER BY epochMillis DESC")
    fun observeFuelPricePeriods(): Flow<List<FuelPricePeriodEntity>>

    @Query("SELECT * FROM fuel_price_periods ORDER BY epochMillis DESC")
    suspend fun getAllFuelPricePeriods(): List<FuelPricePeriodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelPricePeriod(period: FuelPricePeriodEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFuelPricePeriods(periods: List<FuelPricePeriodEntity>)

    @Query("DELETE FROM fuel_price_periods")
    suspend fun clearAllFuelPricePeriods()
}
