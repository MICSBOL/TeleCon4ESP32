package com.micsbol.telecon4esp32.data.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Encodes camera JPEG frames into an MP4 and publishes it under Movies/TeleCon.
 * Call [appendJpeg] and [finish] from one thread.
 */
class CameraJpegVideoRecorder(
    private val context: Context,
) {
    private var encoder: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var muxer: MediaMuxer? = null
    private var cacheFile: File? = null
    private var trackIndex = -1
    private var muxerStarted = false
    private var frameCount = 0
    private var width = 0
    private var height = 0
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val dest = Rect()

    fun appendJpeg(jpeg: ByteArray) {
        val bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) ?: return
        try {
            if (encoder == null && !start(bitmap.width, bitmap.height)) return
            val surface = inputSurface ?: return
            val canvas = surface.lockHardwareCanvas()
            canvas.drawColor(Color.BLACK)
            dest.set(0, 0, width, height)
            canvas.drawBitmap(bitmap, null, dest, paint)
            surface.unlockCanvasAndPost(canvas)
            frameCount++
            drain(endOfStream = false)
        } catch (error: RuntimeException) {
            Log.w(TAG, "Camera frame dropped: ${error.message}")
        } finally {
            bitmap.recycle()
        }
    }

    /** Stops the encoder. Returns Movies/TeleCon path when a video was saved. */
    fun finish(): String? {
        val wroteFrames = frameCount > 0
        if (wroteFrames) {
            runCatching { encoder?.signalEndOfInputStream() }
            drain(endOfStream = true)
        }
        releaseCodec()
        val file = cacheFile
        cacheFile = null
        if (!wroteFrames || file == null || !file.exists() || file.length() == 0L) {
            file?.delete()
            return null
        }
        return runCatching { publishToMovies(file) }
            .onFailure { Log.e(TAG, "Camera video save failed", it) }
            .getOrNull()
            .also { file.delete() }
    }

    private fun start(sourceWidth: Int, sourceHeight: Int): Boolean {
        width = sourceWidth.coerceAtLeast(2) and 1.inv()
        height = sourceHeight.coerceAtLeast(2) and 1.inv()
        val file = File(context.cacheDir, videoFileName())
        cacheFile = file
        return try {
            val format = MediaFormat.createVideoFormat(MIME, width, height).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
                )
                setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            val codec = MediaCodec.createEncoderByType(MIME)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = codec.createInputSurface()
            codec.start()
            encoder = codec
            muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            true
        } catch (error: RuntimeException) {
            Log.e(TAG, "Camera encoder start failed", error)
            releaseCodec()
            file.delete()
            false
        }
    }

    private fun drain(endOfStream: Boolean) {
        val codec = encoder ?: return
        val info = MediaCodec.BufferInfo()
        var spins = 0
        while (spins < 64) {
            val index = codec.dequeueOutputBuffer(info, if (endOfStream) 10_000 else 0)
            when {
                index == MediaCodec.INFO_TRY_AGAIN_LATER -> if (endOfStream) spins++ else return
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (muxerStarted) return
                    trackIndex = muxer?.addTrack(codec.outputFormat) ?: return
                    muxer?.start()
                    muxerStarted = true
                }
                index >= 0 -> {
                    val config = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                    if (!config && info.size > 0 && muxerStarted) {
                        codec.getOutputBuffer(index)?.let { buffer ->
                            muxer?.writeSampleData(trackIndex, buffer, info)
                        }
                    }
                    codec.releaseOutputBuffer(index, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
                else -> if (endOfStream) spins++ else return
            }
        }
    }

    private fun releaseCodec() {
        runCatching { inputSurface?.release() }
        inputSurface = null
        runCatching { encoder?.stop() }
        runCatching { encoder?.release() }
        encoder = null
        if (muxerStarted) {
            runCatching { muxer?.stop() }
        }
        runCatching { muxer?.release() }
        muxer = null
        muxerStarted = false
        trackIndex = -1
    }

    private fun publishToMovies(source: File): String {
        val fileName = source.name
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val relativePath = "${Environment.DIRECTORY_MOVIES}/TeleCon/"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = context.contentResolver.insert(collection, values)
                ?: error("Could not create $fileName")
            try {
                FileInputStream(source).use { input ->
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        input.copyTo(output)
                    } ?: error("Could not open $fileName")
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                return "$relativePath$fileName"
            } catch (error: Throwable) {
                context.contentResolver.delete(uri, null, null)
                throw error
            }
        }
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: File(context.filesDir, Environment.DIRECTORY_MOVIES)
        if (!directory.exists()) directory.mkdirs()
        val dest = File(directory, fileName)
        source.copyTo(dest, overwrite = true)
        return dest.absolutePath
    }

    private companion object {
        private const val TAG = "CameraJpegVideoRecorder"
        private const val MIME = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val FRAME_RATE = 10

        fun videoFileName(): String {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            return "TeleCon_CAM_$stamp.mp4"
        }
    }
}

/** Writes one camera JPEG into Pictures/TeleCon. Returns the stored path. */
internal fun saveCameraStillJpeg(context: Context, jpeg: ByteArray): String {
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val fileName = "TeleCon_CAM_$stamp.jpg"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val relativePath = "${Environment.DIRECTORY_PICTURES}/TeleCon/"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = context.contentResolver.insert(collection, values)
            ?: error("Could not create $fileName")
        try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(jpeg)
            } ?: error("Could not open $fileName")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            return "$relativePath$fileName"
        } catch (error: Throwable) {
            context.contentResolver.delete(uri, null, null)
            throw error
        }
    }
    val directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        ?: File(context.filesDir, Environment.DIRECTORY_PICTURES)
    if (!directory.exists()) directory.mkdirs()
    val dest = File(directory, fileName)
    dest.writeBytes(jpeg)
    return dest.absolutePath
}
