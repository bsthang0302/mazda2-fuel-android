package com.mazda2care.fuellog

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mazda2care.fuellog.worker.FuelPriceSyncWorker
import java.util.concurrent.TimeUnit

class FuelLogApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        scheduleAutomaticFuelPriceSync()
    }

    private fun scheduleAutomaticFuelPriceSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Tự động kiểm tra giá xăng mỗi 6 giờ (đặc biệt bắt kịp kỳ điều hành 15h00 chiều thứ Năm)
        val syncRequest = PeriodicWorkRequestBuilder<FuelPriceSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PetrolimexAutoPriceSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
