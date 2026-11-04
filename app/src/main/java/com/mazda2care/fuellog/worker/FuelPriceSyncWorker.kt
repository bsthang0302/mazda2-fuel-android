package com.mazda2care.fuellog.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mazda2care.fuellog.data.AppDatabase
import com.mazda2care.fuellog.data.FuelRepository
import com.mazda2care.fuellog.model.FormatUtils

class FuelPriceSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val repo = FuelRepository(applicationContext, db.fuelDao())

        val oldPrice = repo.getCarProfile().currentFuelPrice
        val fetchResult = repo.syncLiveFuelPrices()

        return if (fetchResult.success) {
            val newProfile = repo.getCarProfile()
            val newPrice = newProfile.currentFuelPrice
            if (kotlin.math.abs(newPrice - oldPrice) >= 10.0) {
                sendPriceChangedNotification(
                    fuelName = newProfile.selectedFuelProduct.shortName,
                    zoneName = newProfile.selectedPriceZone.shortLabel,
                    oldPrice = oldPrice,
                    newPrice = newPrice
                )
            }
            Result.success()
        } else {
            Result.retry()
        }
    }

    private fun sendPriceChangedNotification(
        fuelName: String,
        zoneName: String,
        oldPrice: Double,
        newPrice: Double
    ) {
        val channelId = "fuel_price_updates"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Cập nhật giá xăng tự động",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Thông báo khi Petrolimex điều chỉnh giá bán lẻ xăng dầu"
            }
            manager.createNotificationChannel(channel)
        }

        val diff = newPrice - oldPrice
        val trend = if (diff > 0) "tăng +${FormatUtils.formatVnd(diff)}" else "giảm ${FormatUtils.formatVnd(diff)}"

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Giá $fuelName ($zoneName) vừa điều chỉnh!")
            .setContentText("Giá mới: ${FormatUtils.formatVnd(newPrice)}/L ($trend so với kỳ trước)")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            manager.notify(1001, notification)
        } catch (_: SecurityException) {
            // Người dùng chưa cấp quyền POST_NOTIFICATIONS trên Android 13+
        }
    }
}
