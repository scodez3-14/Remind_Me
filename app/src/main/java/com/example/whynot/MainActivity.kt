package com.example.whynot

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.ui.unit.dp


data class ReminderItem(
    val id: Int,
    val message: String,
    val time: String
)

class MainActivity : ComponentActivity() {

    // Permission launcher for Android 13+
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "Notification permission denied!", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Create the Notification Channel (Required for Android 8.0+)
        createNotificationChannel()

        // 2. Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 3. Show Compose UI
        setContent {
            ReminderScreen(this)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "ReminderChannel"
            val descriptionText = "Channel for Reminder App"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("REMINDER_ID", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

fun scheduleNotification(context: Context, time: Long, message: String, id: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra("message", message)
    }

    // Use the 'id' here so each PendingIntent is unique
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        id,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
}



fun saveReminders(context: Context, list: List<ReminderItem>) {
    val sharedPreferences = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    val json = Gson().toJson(list) // Converts list to String
    editor.putString("reminders", json)
    editor.apply()
}

fun loadReminders(context: Context): MutableList<ReminderItem> {
    val sharedPreferences = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
    val json = sharedPreferences.getString("reminders", null)
    val type = object : TypeToken<MutableList<ReminderItem>>() {}.type
    return if (json == null) mutableListOf() else Gson().fromJson(json, type)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(context: Context) {
    val reminderList = remember { mutableStateListOf<ReminderItem>().apply { addAll(loadReminders(context)) } }
    var showAddDialog by remember { mutableStateOf(false) }

    // Temp states for inputs
    var taskText by remember { mutableStateOf("") }
    val calendar = remember { Calendar.getInstance() }
    var selectedDate by remember { mutableStateOf("${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)+1}-${calendar.get(Calendar.DAY_OF_MONTH)}") }
    var selectedTime by remember { mutableStateOf(String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
        ) {
            Text("My Tasks", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(reminderList) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.message, style = MaterialTheme.typography.bodyLarge)
                                Text("On: ${item.time}, ${item.id}", color = Color.Gray) // you can store date too
                            }
                            IconButton(onClick = {
                                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                val intent = Intent(context, ReminderReceiver::class.java)
                                val pendingIntent = PendingIntent.getBroadcast(
                                    context,
                                    item.id,
                                    intent,
                                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                                )
                                pendingIntent?.let {
                                    alarmManager.cancel(it)
                                    it.cancel()
                                }
                                reminderList.remove(item)
                                saveReminders(context, reminderList)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }

        // ADD TASK DIALOG
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Task") },
                text = {
                    Column {
                        TextField(
                            value = taskText,
                            onValueChange = { taskText = it },
                            label = { Text("Task") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Date picker (simple text input for now)
                        TextField(
                            value = selectedDate,
                            onValueChange = { selectedDate = it },
                            label = { Text("Date (YYYY-MM-DD)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Time picker (simple text input for now)
                        TextField(
                            value = selectedTime,
                            onValueChange = { selectedTime = it },
                            label = { Text("Time (HH:MM)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val newId = (0..100000).random()
                        val fullTime = "$selectedDate $selectedTime"
                        reminderList.add(ReminderItem(newId, taskText, fullTime))
                        saveReminders(context, reminderList)

                        // Schedule notification (optional)
                        val cal = Calendar.getInstance().apply {
                            val parts = fullTime.split(" ", ":", ignoreCase = true)
                            set(Calendar.YEAR, selectedDate.split("-")[0].toInt())
                            set(Calendar.MONTH, selectedDate.split("-")[1].toInt() - 1)
                            set(Calendar.DAY_OF_MONTH, selectedDate.split("-")[2].toInt())
                            set(Calendar.HOUR_OF_DAY, selectedTime.split(":")[0].toInt())
                            set(Calendar.MINUTE, selectedTime.split(":")[1].toInt())
                            set(Calendar.SECOND, 0)
                        }
                        scheduleNotification(context, cal.timeInMillis, taskText, newId)

                        // Reset dialog
                        taskText = ""
                        showAddDialog = false
                    }) {
                        Text("Save")
                    }
                }
            )
        }
    }
}
