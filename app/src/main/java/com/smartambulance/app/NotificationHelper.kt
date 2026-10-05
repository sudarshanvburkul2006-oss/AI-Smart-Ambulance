package com.smartambulance.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    // Unique channel ID so Android uses the new sound/vibration settings
    private const val CHANNEL_ID = "ambulance_alert_channel_v3"

    // Notification ID
    private const val NOTIFICATION_ID = 1001

    /**
     * Creates the notification channel.
     *
     * On Android 8.0 and above, sound/vibration are controlled
     * mainly by the notification channel.
     */
    fun createChannel(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val soundUri =
                RingtoneManager.getDefaultUri(
                    RingtoneManager.TYPE_ALARM
                )

            val audioAttributes =
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(
                        AudioAttributes.CONTENT_TYPE_SONIFICATION
                    )
                    .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ambulance Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {

                description =
                    "High priority emergency ambulance alerts"

                enableVibration(true)

                vibrationPattern = longArrayOf(
                    0,
                    1000,
                    500,
                    1000,
                    500,
                    1500
                )

                setSound(
                    soundUri,
                    audioAttributes
                )
            }

            val manager =
                context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Shows the ambulance approaching notification.
     *
     * Called when backend returns:
     *
     * alert = true
     */
    fun showAmbulanceAlert(context: Context) {

        // Make sure notification channel exists
        createChannel(context)

        // Android 13+ requires notification permission
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                context.checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        // Get alarm sound
        val soundUri =
            RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_ALARM
            )

        // Build notification
        val notification =
            NotificationCompat.Builder(
                context,
                CHANNEL_ID
            )
                .setSmallIcon(
                    android.R.drawable.ic_dialog_alert
                )
                .setContentTitle(
                    "🚨 AMBULANCE APPROACHING"
                )
                .setContentText(
                    "Emergency ambulance detected within 500 meters."
                )
                .setPriority(
                    NotificationCompat.PRIORITY_MAX
                )
                .setCategory(
                    NotificationCompat.CATEGORY_ALARM
                )
                .setAutoCancel(true)
                .setVibrate(
                    longArrayOf(
                        0,
                        1000,
                        500,
                        1000,
                        500,
                        1500
                    )
                )
                .setSound(
                    RingtoneManager.getDefaultUri(
                        RingtoneManager.TYPE_ALARM
                    )
                )
                .build()

        // Show notification
        NotificationManagerCompat
            .from(context)
            .notify(
                NOTIFICATION_ID,
                notification
            )
    }
}