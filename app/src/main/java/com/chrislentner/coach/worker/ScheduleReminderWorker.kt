package com.chrislentner.coach.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class ScheduleReminderWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        NotificationHelper.createNotificationChannels(applicationContext)
        NotificationHelper.showReminderNotification(applicationContext)
        return Result.success()
    }
}
