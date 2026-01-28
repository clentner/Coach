package com.chrislentner.coach.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
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
            val now = ZonedDateTime.now()
            val dueTime = LocalTime.of(9, 0)
            var dueDateTime = now.with(dueTime)
            if (dueDateTime.isBefore(now)) {
                dueDateTime = dueDateTime.plusDays(1)
            }

            val timeDiff = Duration.between(now, dueDateTime).toMillis()

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
