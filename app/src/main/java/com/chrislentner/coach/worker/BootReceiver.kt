package com.chrislentner.coach.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleDailySurvey(context)
        }
    }

    companion object {
        fun scheduleDailySurvey(context: Context) {
            val workManager = WorkManager.getInstance(context)

            // Calculate initial delay for 9 AM
            val currentDate = Calendar.getInstance()
            val dueDate = Calendar.getInstance()
            dueDate.set(Calendar.HOUR_OF_DAY, 9)
            dueDate.set(Calendar.MINUTE, 0)
            dueDate.set(Calendar.SECOND, 0)

            if (dueDate.before(currentDate)) {
                dueDate.add(Calendar.HOUR_OF_DAY, 24)
            }

            val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

            val dailyWorkRequest = PeriodicWorkRequestBuilder<DailySurveyWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                "DailySurveyWork",
                ExistingPeriodicWorkPolicy.UPDATE, // Update ensures timing is reset/kept correct
                dailyWorkRequest
            )
        }
    }
}
