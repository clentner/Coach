package com.chrislentner.coach.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class DailySurveyWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        NotificationHelper.createNotificationChannels(applicationContext)
        NotificationHelper.showSurveyNotification(applicationContext)
        return Result.success()
    }
}
