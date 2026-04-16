package com.rhythmgame.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

object AudioTrimmer {

    suspend fun trim(inputFile: File, outputFile: File, startMs: Long, endMs: Long): Boolean =
        withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            var muxer: MediaMuxer? = null
            try {
                extractor.setDataSource(inputFile.absolutePath)

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
                if (audioTrackIndex == -1 || audioFormat == null) return@withContext false

                extractor.selectTrack(audioTrackIndex)

                outputFile.parentFile?.mkdirs()
                muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val muxerTrackIndex = muxer.addTrack(audioFormat)
                muxer.start()

                val startUs = startMs * 1000
                val endUs = endMs * 1000
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                val bufferSize = 1024 * 1024 // 1MB
                val buffer = ByteBuffer.allocate(bufferSize)
                val bufferInfo = MediaCodec.BufferInfo()

                while (true) {
                    buffer.clear()
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    val sampleTime = extractor.sampleTime
                    if (sampleTime > endUs) break

                    if (sampleTime >= startUs) {
                        bufferInfo.offset = 0
                        bufferInfo.size = sampleSize
                        bufferInfo.presentationTimeUs = sampleTime - startUs
                        bufferInfo.flags = extractor.sampleFlags
                        muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                    }

                    extractor.advance()
                }

                muxer.stop()
                true
            } catch (e: Exception) {
                outputFile.delete()
                false
            } finally {
                extractor.release()
                try { muxer?.release() } catch (_: Exception) { }
            }
        }
}
