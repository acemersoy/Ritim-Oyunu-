package com.rhythmgame.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

object WaveformExtractor {

    suspend fun extractAmplitudes(file: File, sampleCount: Int = 200): List<Float> =
        withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(file.absolutePath)

                // Find audio track
                var audioTrackIndex = -1
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("audio/")) {
                        audioTrackIndex = i
                        break
                    }
                }
                if (audioTrackIndex == -1) return@withContext emptyList()

                val format = extractor.getTrackFormat(audioTrackIndex)
                extractor.selectTrack(audioTrackIndex)

                val mime = format.getString(MediaFormat.KEY_MIME) ?: return@withContext emptyList()
                val codec = MediaCodec.createDecoderByType(mime)
                codec.configure(format, null, null, 0)
                codec.start()

                val allSamples = mutableListOf<Short>()
                val bufferInfo = MediaCodec.BufferInfo()
                var inputDone = false
                var outputDone = false

                while (!outputDone) {
                    // Feed input
                    if (!inputDone) {
                        val inputIndex = codec.dequeueInputBuffer(10_000)
                        if (inputIndex >= 0) {
                            val inputBuffer = codec.getInputBuffer(inputIndex) ?: continue
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }

                    // Read output
                    val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                    if (outputIndex >= 0) {
                        val outputBuffer = codec.getOutputBuffer(outputIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
                            val shortBuffer = outputBuffer.asShortBuffer()
                            val shortCount = bufferInfo.size / 2
                            for (i in 0 until shortCount) {
                                allSamples.add(shortBuffer.get(i))
                            }
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                }

                codec.stop()
                codec.release()

                if (allSamples.isEmpty()) return@withContext emptyList()

                // Downsample to sampleCount windows, compute RMS for each
                val windowSize = allSamples.size / sampleCount
                if (windowSize <= 0) return@withContext allSamples.map { it.toFloat() / Short.MAX_VALUE }

                val amplitudes = mutableListOf<Float>()
                for (i in 0 until sampleCount) {
                    val start = i * windowSize
                    val end = minOf(start + windowSize, allSamples.size)
                    var sum = 0.0
                    for (j in start until end) {
                        val s = allSamples[j].toDouble() / Short.MAX_VALUE
                        sum += s * s
                    }
                    val rms = sqrt(sum / (end - start)).toFloat()
                    amplitudes.add(rms)
                }

                // Normalize to 0..1
                val maxAmp = amplitudes.maxOrNull() ?: 1f
                if (maxAmp > 0f) {
                    amplitudes.map { it / maxAmp }
                } else {
                    amplitudes
                }
            } finally {
                extractor.release()
            }
        }

    suspend fun getDurationMs(file: File): Long = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    val durationUs = format.getLong(MediaFormat.KEY_DURATION)
                    return@withContext durationUs / 1000
                }
            }
            0L
        } finally {
            extractor.release()
        }
    }
}
