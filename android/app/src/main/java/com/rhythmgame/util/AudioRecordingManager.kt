package com.rhythmgame.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecordingManager {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start(context: Context, outputFile: File) {
        this.outputFile = outputFile
        outputFile.parentFile?.mkdirs()

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
    }

    fun stop(): File? {
        return try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            outputFile
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            null
        }
    }

    fun cancel() {
        try {
            recorder?.stop()
        } catch (_: Exception) { }
        recorder?.release()
        recorder = null
        outputFile?.delete()
        outputFile = null
    }

    fun getMaxAmplitude(): Int {
        return try {
            recorder?.maxAmplitude ?: 0
        } catch (_: Exception) {
            0
        }
    }
}
