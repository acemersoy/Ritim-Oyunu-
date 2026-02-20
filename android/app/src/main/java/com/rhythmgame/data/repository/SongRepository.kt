package com.rhythmgame.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.rhythmgame.data.local.ChartDao
import com.rhythmgame.data.local.ChartEntity
import com.rhythmgame.data.local.SongDao
import com.rhythmgame.data.model.Chart
import com.rhythmgame.data.model.Song
import com.rhythmgame.data.remote.RhythmGameApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepository @Inject constructor(
    private val songDao: SongDao,
    private val chartDao: ChartDao,
    private val api: RhythmGameApi,
) {
    fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()

    suspend fun getSongById(songId: String): Song? = songDao.getSongById(songId)

    /**
     * Import a song:
     * 1. Copy file to app storage (for playback)
     * 2. Upload to Server for analysis
     * 3. Poll for status until Ready
     * 4. Download and save charts
     */
    suspend fun importSong(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val tempId = UUID.randomUUID().toString() // Temporary local ID until server responds
        val filename = getFileName(context, uri)

        // Create app-private directories
        val songsDir = File(context.filesDir, "songs").also { it.mkdirs() }
        val songDir = File(songsDir, tempId).also { it.mkdirs() }

        // Copy original file for local playback
        val localAudioFile = File(songDir, filename)
        context.contentResolver.openInputStream(uri)?.use { input ->
            localAudioFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw Exception("Dosya acilamadi")

        // Initial DB insert (Processing)
        val initialSong = Song(
            songId = tempId,
            filename = filename,
            status = "uploading",
            localAudioPath = localAudioFile.absolutePath,
        )
        songDao.insertSong(initialSong)

        try {
            // Upload to Server
            val requestFile = localAudioFile.asRequestBody("audio/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", filename, requestFile)
            
            val uploadResponse = api.uploadSong(body)
            val serverSongId = uploadResponse.song_id

            // Update local ID to match Server ID (Critical for consistency)
            // Note: Since Room doesn't support changing PrimaryKey easily, we might need to delete and re-insert
            // OR we just use the server ID from the start if we could. 
            // Better approach: Update the existing record with the new ID? No, PK is immutable.
            // Strategy: Delete temp record, Insert new record with Server ID.
            
            // Rename local directory to match server ID
            val newSongDir = File(songsDir, serverSongId)
            if (songDir.renameTo(newSongDir)) {
                // Update file path reference
                val newAudioPath = File(newSongDir, filename).absolutePath
                
                songDao.deleteSong(initialSong)
                
                val processingSong = initialSong.copy(
                    songId = serverSongId,
                    status = "analyzing",
                    localAudioPath = newAudioPath
                )
                songDao.insertSong(processingSong)
                
                return@withContext serverSongId
            } else {
                throw Exception("Failed to rename song directory")
            }

        } catch (e: Exception) {
            songDao.updateSong(initialSong.copy(status = "error", errorMessage = e.message))
            throw e
        }
    }

    /**
     * Poll status and download charts when ready.
     */
    suspend fun analyzeSong(context: Context, songId: String): Unit = withContext(Dispatchers.IO) {
        var retries = 0
        val maxRetries = 60 // 60 * 2s = 2 minutes timeout
        
        while (retries < maxRetries) {
            try {
                val status = api.getStatus(songId)
                
                when (status.status) {
                    "ready" -> {
                        // Download charts for all difficulties
                        val gson = Gson()
                        val difficulties = listOf("easy", "medium", "hard")
                        
                        for (diff in difficulties) {
                            try {
                                val chartResponse = api.getChart(songId, diff)
                                // Convert remote response to local model
                                // We can reuse the same JSON structure or map it
                                // Ideally, we map it. But ChartEntity stores JSON string.
                                // Let's just store the response as JSON since it matches our expectations
                                val chartJson = gson.toJson(chartResponse)
                                
                                chartDao.insertChart(
                                    ChartEntity(
                                        songId = songId,
                                        difficulty = diff,
                                        chartJson = chartJson
                                    )
                                )
                                
                                // Update song metadata from the first chart we get
                                if (diff == "medium") {
                                    val song = songDao.getSongById(songId)
                                    if (song != null) {
                                        songDao.updateSong(
                                            song.copy(
                                                status = "ready",
                                                bpm = chartResponse.bpm,
                                                durationMs = chartResponse.duration_ms
                                            )
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                // Ignore missing difficulty if any
                            }
                        }
                        return@withContext
                    }
                    "error" -> {
                        throw Exception(status.message ?: "Server analysis failed")
                    }
                    else -> {
                        // Still processing
                        delay(2000)
                        retries++
                    }
                }
            } catch (e: Exception) {
                // Network error, maybe retry a few times but count as wait
                delay(2000)
                retries++
            }
        }
        
        // Timeout
        val song = songDao.getSongById(songId)
        song?.let { 
            songDao.updateSong(it.copy(status = "error", errorMessage = "Analysis timed out")) 
        }
        throw Exception("Analysis timed out")
    }

    /**
     * Get chart for a song and difficulty from local DB.
     */
    suspend fun getChart(songId: String, difficulty: String = "medium"): Chart {
        val entity = chartDao.getChart(songId, difficulty)
            ?: throw Exception("Chart bulunamadi: $songId / $difficulty")
        return Gson().fromJson(entity.chartJson, Chart::class.java)
    }

    /**
     * Get the local audio file URI for playback.
     */
    suspend fun getAudioUri(songId: String): Uri {
        val song = songDao.getSongById(songId)
            ?: throw Exception("Sarki bulunamadi")
        val audioPath = song.localAudioPath
            ?: throw Exception("Audio dosya yolu bulunamadi")
        return Uri.fromFile(File(audioPath))
    }

    suspend fun updateHighScore(songId: String, difficulty: String, score: Int) {
        when (difficulty) {
            "easy" -> songDao.updateHighScoreEasy(songId, score)
            "medium" -> songDao.updateHighScoreMedium(songId, score)
            "hard" -> songDao.updateHighScoreHard(songId, score)
        }
    }

    /**
     * Delete song: remove DB records + local files.
     */
    suspend fun deleteSong(context: Context, songId: String) {
        // Delete charts from DB
        chartDao.deleteChartsForSong(songId)

        // Delete song from DB
        val song = songDao.getSongById(songId)
        if (song != null) {
            songDao.deleteSong(song)
        }

        // Delete local files
        val songDir = File(context.filesDir, "songs/$songId")
        if (songDir.exists()) {
            songDir.deleteRecursively()
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "audio.mp3"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
}
