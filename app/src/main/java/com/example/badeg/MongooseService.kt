package com.example.badeg

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import java.io.RandomAccessFile

class MongooseService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val statsRunnable = object : Runnable {
        override fun run() {
            logSystemStats()
            handler.postDelayed(this, 10000) // Every 10 seconds
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val docRoot = intent?.getStringExtra("DOC_ROOT") ?: filesDir.absolutePath

        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("B.A.D.E.G Server")
            .setContentText("Mongoose Server is running in background")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

        startForeground(1, notification)

        ServerManager.startServer(docRoot)
        ServerManager.nativeLog("Service started with root: $docRoot")

        handler.post(statsRunnable)

        return START_STICKY
    }

    private fun logSystemStats() {
        try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val load = reader.readLine()
            reader.close()
            ServerManager.nativeLog("SYS: $load")
        } catch (e: Exception) {
            // proc/stat might not be accessible on newer Android versions without extra effort
            // but we try anyway for older ones or rooted environments
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(statsRunnable)
        ServerManager.nativeLog("Service stopping")
        ServerManager.stopServer()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Mongoose Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        const val CHANNEL_ID = "MongooseServiceChannel"
    }
}
