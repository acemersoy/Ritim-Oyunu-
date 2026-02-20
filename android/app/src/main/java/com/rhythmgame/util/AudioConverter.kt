package com.rhythmgame.util

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Converts audio files (MP3, M4A, OGG, etc.) to WAV format using Android MediaCodec.
 * This replaces pydub/ffmpeg for on-device audio conversion.
 */
object AudioConverter {

    /**
     * Convert an audio file to WAV format.
     * @param context Android context
     * @param inputUri URI of the input audio file
     * @param outputFile Destination WAV file
     * @return true if conversion succeeded
     */
    fun convertToWav(context: Context, inputUri: Uri, outputFile: File): Boolean {
        val extractor = MediaExtractor()
        try {
            // Set data source from URI
            context.contentResolver.openFileDescriptor(inputUri, "r")?.use { pfd ->
                extractor.setDataSource(pfd.fileDescriptor)
            } ?: return false

            // Find audio track
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex < 0 || audioFormat == null) return false

            extractor.selectTrack(audioTrackIndex)
            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: return false
            val sampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            // If already PCM/WAV, just copy with proper header
            if (mime == MediaFormat.MIMETYPE_AUDIO_RAW) {
                return writeRawToWav(context, inputUri, outputFile, sampleRate, channelCount)
            }

            // Create decoder
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(audioFormat, null, null, 0)
            codec.start()

            val pcmData = mutableListOf<ByteArray>()
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            val timeoutUs = 10_000L

            while (!outputDone) {
                // Feed input
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(timeoutUs)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex) ?: continue
                        val bytesRead = extractor.readSampleData(inputBuffer, 0)
                        if (bytesRead < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, bytesRead, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                // Drain output
                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                if (outputIndex >= 0) {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                    val outputBuffer = codec.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        val chunk = ByteArray(bufferInfo.size)
                        outputBuffer.get(chunk)
                        pcmData.add(chunk)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                }
            }

            codec.stop()
            codec.release()

            // Get actual output format (sample rate / channels may differ)
            val outSampleRate = sampleRate
            val outChannels = channelCount

            // Write WAV file
            writeWavFile(outputFile, pcmData, outSampleRate, outChannels)
            return true

        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            extractor.release()
        }
    }

    /**
     * Convert a local file path to WAV.
     */
    fun convertFileToWav(inputFile: File, outputFile: File, context: Context): Boolean {
        val uri = Uri.fromFile(inputFile)
        return convertToWav(context, uri, outputFile)
    }

    private fun writeRawToWav(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        sampleRate: Int,
        channelCount: Int,
    ): Boolean {
        val inputStream = context.contentResolver.openInputStream(inputUri) ?: return false
        val pcmBytes = inputStream.use { it.readBytes() }
        writeWavFile(outputFile, listOf(pcmBytes), sampleRate, channelCount)
        return true
    }

    private fun writeWavFile(
        file: File,
        pcmChunks: List<ByteArray>,
        sampleRate: Int,
        channelCount: Int,
        bitsPerSample: Int = 16,
    ) {
        val totalPcmSize = pcmChunks.sumOf { it.size }
        val byteRate = sampleRate * channelCount * bitsPerSample / 8
        val blockAlign = channelCount * bitsPerSample / 8

        FileOutputStream(file).use { fos ->
            val buffer = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

            // RIFF header
            buffer.put("RIFF".toByteArray())
            buffer.putInt(36 + totalPcmSize)
            buffer.put("WAVE".toByteArray())

            // fmt chunk
            buffer.put("fmt ".toByteArray())
            buffer.putInt(16) // chunk size
            buffer.putShort(1) // PCM format
            buffer.putShort(channelCount.toShort())
            buffer.putInt(sampleRate)
            buffer.putInt(byteRate)
            buffer.putShort(blockAlign.toShort())
            buffer.putShort(bitsPerSample.toShort())

            // data chunk
            buffer.put("data".toByteArray())
            buffer.putInt(totalPcmSize)

            fos.write(buffer.array())

            // Write PCM data
            for (chunk in pcmChunks) {
                fos.write(chunk)
            }
        }
    }
}
