package com.example.datewise.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.datewise.data.DateWiseDatabase
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class DailyExpiryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = DateWiseDatabase.getDatabase(applicationContext)
        val productDao = database.productDao()

        // Fetch all products (synchronous one-shot operation using first() on Flow)
        val products = try {
            productDao.getAll().first()
        } catch (e: Exception) {
            return Result.retry()
        }

        val today = LocalDate.now()

        products.forEach { product ->
            val daysLeft = ChronoUnit.DAYS.between(today, product.expiryDate)

            // Trigger notification only if exactly 7, 3, or 1 days left
            if (daysLeft == 7L || daysLeft == 3L || daysLeft == 1L) {
                NotificationHelper.sendExpiryNotification(applicationContext, product, daysLeft)
            }
        }

        return Result.success()
    }
}
