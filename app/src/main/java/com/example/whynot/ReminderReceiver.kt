package com.example.whynot

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
// Source - https://stackoverflow.com/a
// Posted by Joffrey, modified by community. See post 'Timeline' for change history
// Retrieved 2026-01-03, License - CC BY-SA 4.0

import kotlinx.coroutines.*


class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync() // Tells the system to keep the receiver alive
        val originalTask = intent.getStringExtra("message") ?: "Task"

        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = "xxxxxxxxxxxxxxxxxxxxx"
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prompt = "The user has a task: '$originalTask'. Give me one short, high-energy motivational sentence with emojis, you can use famous quotes  of high achievers too."
                val response = generativeModel.generateContent(prompt)
                val aiMessage = response.text?.take(120) ?: "You've got this!"

                showNotification(context, originalTask, aiMessage)
            } catch (e: Exception) {
                android.util.Log.e("GEMINI_ERROR", "Full error: ", e)
                showNotification(context, originalTask, "Check Logcat for: ${e.message}")
            } finally {
                pendingResult.finish() // Must call this so the system can reclaim resources
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val builder = NotificationCompat.Builder(context, "REMINDER_ID")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}
