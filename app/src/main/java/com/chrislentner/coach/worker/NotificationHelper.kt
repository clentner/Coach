package com.chrislentner.coach.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.chrislentner.coach.MainActivity
import com.chrislentner.coach.R

object NotificationHelper {

    const val SURVEY_CHANNEL_ID = "survey_channel"
    const val REMINDER_CHANNEL_ID = "reminder_channel"
    const val SURVEY_NOTIFICATION_ID = 1001
    const val REMINDER_NOTIFICATION_ID = 1002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val surveyChannel = NotificationChannel(
                SURVEY_CHANNEL_ID,
                "Daily Survey",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily morning survey to plan your workout"
            }

            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Workout Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for your scheduled workouts"
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(surveyChannel)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    fun showSurveyNotification(context: Context) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "survey")
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, SURVEY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use default icon
            .setContentTitle("Plan your workout")
            .setContentText("Good morning! Click here to plan your workout for today.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(SURVEY_NOTIFICATION_ID, builder.build())
        }
    }

    fun showReminderNotification(context: Context) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Workout Time!")
            .setContentText("Your workout starts in 10 minutes. Get ready!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(REMINDER_NOTIFICATION_ID, builder.build())
        }
    }
}
