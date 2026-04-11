package com.rhythmgame.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class UploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationId = 101
    private val channelId = "upload_channel"

    override suspend fun doWork(): Result {
        val filePath = inputData.getString("FILE_PATH") ?: return Result.failure()
        val serverUrl = inputData.getString("SERVER_URL") ?: "http://10.0.2.2:8000/api/upload"
        
        val file = File(filePath)
        if (!file.exists()) return Result.failure()

        createNotificationChannel()
        setForeground(createForegroundInfo(0))

        return try {
            uploadFileStreaming(file, serverUrl)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun uploadFileStreaming(file: File, urlString: String) {
        val boundary = "Boundary-${UUID.randomUUID()}"
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        
        connection.apply {
            doOutput = true
            requestMethod = "POST"
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setChunkedStreamingMode(1024 * 1024) // 1MB chunks
        }

        val outputStream = connection.outputStream
        val writer = outputStream.bufferedWriter()
        
        // Multi-part header
        writer.write("--$boundary\r\n")
        writer.write("Content-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"\r\n")
        writer.write("Content-Type: audio/mpeg\r\n\r\n")
        writer.flush()

        val inputStream = FileInputStream(file)
        val buffer = ByteArray(1024 * 64) // 64KB buffer for reading
        var totalBytesRead = 0L
        val fileSize = file.length()
        var lastUpdate = 0L

        inputStream.use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                
                // Update progress every 500ms to avoid UI flickering
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdate > 500) {
                    val progress = ((totalBytesRead * 100) / fileSize).toInt()
                    updateProgress(progress)
                    lastUpdate = currentTime
                }
            }
        }
        
        writer.write("\r\n--$boundary--\r\n")
        writer.flush()
        writer.close()
        outputStream.close()

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw Exception("Server returned code $responseCode")
        }
    }

    private fun updateProgress(progress: Int) {
        notificationManager.notify(notificationId, createNotification(progress))
        setProgressAsync(workDataOf("PROGRESS" to progress))
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        return ForegroundInfo(notificationId, createNotification(progress))
    }

    private fun createNotification(progress: Int) = NotificationCompat.Builder(applicationContext, channelId)
        .setContentTitle("Şarkı Analiz Ediliyor...")
        .setSmallIcon(android.R.drawable.stat_sys_upload)
        .setOngoing(true)
        .setProgress(100, progress, false)
        .setSubText("$progress%")
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Upload Status",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}
