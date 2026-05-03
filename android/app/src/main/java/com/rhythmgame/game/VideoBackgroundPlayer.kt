package com.rhythmgame.game

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.*
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.ByteArrayOutputStream

/**
 * Decodes an MP4 video in a background thread and exposes the current frame
 * as a [Bitmap] that renderers can blit onto the Canvas each frame.
 *
 * The video loops automatically. Decoding runs at the video's native frame
 * rate (throttled via presentation timestamps) on a low-priority daemon thread
 * so it never starves the game-loop thread.
 */
class VideoBackgroundPlayer(
    context: Context,
    assetFileName: String,
    private val targetWidth: Int,
    private val targetHeight: Int,
) {
    /** The latest decoded frame — read from the render thread. */
    @Volatile
    var currentFrame: Bitmap? = null
        private set

    private var isRunning = true
    private val decoderThread: Thread

    // Pre-allocated conversion buffers (reused every frame to avoid GC)
    private var nv21Buffer: ByteArray? = null
    private val jpegStream = ByteArrayOutputStream(targetWidth * targetHeight)
    private val decodeRect = Rect()
    private val bitmapOptions = BitmapFactory.Options().apply {
        inMutable = true
        inPreferredConfig = Bitmap.Config.RGB_565 // smaller footprint for background
    }

    // Keep a reference for cleanup
    private var assetFd: AssetFileDescriptor? = null

    init {
        assetFd = context.assets.openFd(assetFileName)
        decoderThread = Thread({
            try {
                decodeLoop()
            } catch (_: InterruptedException) {
                // Normal shutdown
            } catch (e: Exception) {
                android.util.Log.w("VideoBgPlayer", "Decode error", e)
            }
        }, "VideoBg").apply {
            priority = Thread.NORM_PRIORITY - 2
            isDaemon = true
            start()
        }
    }

    fun release() {
        isRunning = false
        decoderThread.interrupt()
        try { decoderThread.join(300) } catch (_: Exception) {}
        currentFrame?.recycle()
        currentFrame = null
        try { assetFd?.close() } catch (_: Exception) {}
    }

    // ── Decode loop ────────────────────────────────────────────────────────────

    private fun decodeLoop() {
        val afd = assetFd ?: return
        val extractor = MediaExtractor()
        extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)

        // Find video track
        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val fmt = extractor.getTrackFormat(i)
            if (fmt.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
                trackIndex = i
                format = fmt
                break
            }
        }
        if (trackIndex < 0 || format == null) return
        extractor.selectTrack(trackIndex)

        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val videoW = format.getInteger(MediaFormat.KEY_WIDTH)
        val videoH = format.getInteger(MediaFormat.KEY_HEIGHT)
        decodeRect.set(0, 0, videoW, videoH)

        // Calculate inSampleSize for downscaling during JPEG decode
        bitmapOptions.inSampleSize = maxOf(videoW / targetWidth, videoH / targetHeight).coerceAtLeast(1)

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0) // ByteBuffer mode (no Surface)
        codec.start()

        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var startTimeNs = System.nanoTime()

        try {
            while (isRunning) {
                // ── Feed input ──
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(5_000)
                    if (inIdx >= 0) {
                        val buf = codec.getInputBuffer(inIdx)!!
                        val size = extractor.readSampleData(buf, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                // ── Read output ──
                val outIdx = codec.dequeueOutputBuffer(info, 5_000)
                if (outIdx >= 0) {
                    // Throttle to video PTS so we don't decode faster than needed
                    val ptsDeltaNs = info.presentationTimeUs * 1000
                    val elapsedNs = System.nanoTime() - startTimeNs
                    val sleepMs = (ptsDeltaNs - elapsedNs) / 1_000_000
                    if (sleepMs > 2) {
                        try { Thread.sleep(sleepMs) } catch (_: InterruptedException) {
                            if (!isRunning) break
                        }
                    }

                    val image = codec.getOutputImage(outIdx)
                    if (image != null) {
                        convertYuvToBitmap(image, videoW, videoH)
                        image.close()
                    }
                    codec.releaseOutputBuffer(outIdx, false)

                    // End of stream → loop from beginning
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        codec.flush()
                        extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                        inputDone = false
                        startTimeNs = System.nanoTime()
                    }
                }
            }
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }
    }

    // ── YUV → Bitmap via YuvImage JPEG roundtrip ───────────────────────────────
    // Simple and reliable across all devices; JPEG quality=70 is invisible for a
    // dimmed background and keeps the intermediate buffer small.

    private fun convertYuvToBitmap(image: android.media.Image, w: Int, h: Int) {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuf = yPlane.buffer
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        val nv21Size = w * h * 3 / 2
        if (nv21Buffer == null || nv21Buffer!!.size < nv21Size) {
            nv21Buffer = ByteArray(nv21Size)
        }
        val nv21 = nv21Buffer!!

        // Copy Y plane
        if (yRowStride == w) {
            yBuf.position(0)
            yBuf.get(nv21, 0, w * h)
        } else {
            for (row in 0 until h) {
                yBuf.position(row * yRowStride)
                yBuf.get(nv21, row * w, w)
            }
        }

        // Copy UV interleaved as NV21 (V first, then U)
        val uvH = h / 2
        if (uvPixelStride == 2 && uvRowStride == w) {
            // Fast path: already semi-planar, just copy V+U interleaved
            vBuf.position(0)
            vBuf.get(nv21, w * h, minOf(vBuf.remaining(), w * uvH))
        } else {
            // Generic path: handle arbitrary strides
            for (row in 0 until uvH) {
                for (col in 0 until w / 2) {
                    val uvOffset = row * uvRowStride + col * uvPixelStride
                    val nv21Offset = w * h + row * w + col * 2
                    nv21[nv21Offset] = vBuf[uvOffset]
                    nv21[nv21Offset + 1] = uBuf[uvOffset]
                }
            }
        }

        // Convert NV21 → JPEG → Bitmap (downscaled)
        jpegStream.reset()
        val yuv = YuvImage(nv21, ImageFormat.NV21, w, h, null)
        yuv.compressToJpeg(decodeRect, 70, jpegStream)

        val bytes = jpegStream.toByteArray()
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bitmapOptions)
        if (decoded != null) {
            // Scale to exact target size if inSampleSize didn't land exactly
            if (decoded.width != targetWidth || decoded.height != targetHeight) {
                val scaled = Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true)
                currentFrame = scaled
                if (decoded !== scaled) decoded.recycle()
                // Don't recycle old frame — render thread may still be drawing it.
                // These are small (~65 KB each at RGB_565) so GC handles them fine.
            } else {
                currentFrame = decoded
            }
        }
    }
}
