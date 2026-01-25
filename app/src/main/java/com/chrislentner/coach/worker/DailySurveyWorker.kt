package com.chrislentner.coach.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chrislentner.coach.database.AppDatabase
import com.chrislentner.coach.database.ScheduleRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DailySurveyWorker(appContext: Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ScheduleRepository(database.scheduleDao())

        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val schedule = repository.getScheduleByDate(today)

        if (schedule != null && schedule.isRestDay) {
            return Result.success()
        }

        NotificationHelper.createNotificationChannels(applicationContext)
        NotificationHelper.showSurveyNotification(applicationContext)
        return Result.success()
    }
}
