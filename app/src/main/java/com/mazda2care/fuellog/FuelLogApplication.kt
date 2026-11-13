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

        // Tự động kiểm tra giá xăng mỗi 2 giờ để bắt kịp ngay các kỳ điều chỉnh chiều Thứ 2 và Thứ 5 (15h00)
        val syncRequest = PeriodicWorkRequestBuilder<FuelPriceSyncWorker>(2, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PetrolimexAutoPriceSync",
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
    }
}
