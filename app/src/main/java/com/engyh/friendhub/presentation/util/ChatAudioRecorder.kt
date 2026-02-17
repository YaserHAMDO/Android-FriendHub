package com.engyh.friendhub.presentation.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class ChatAudioRecorder(
    private val appContext: Context
) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startMs: Long = 0L

    val isRecording: Boolean
        get() = recorder != null

    fun start(): File {
        check(!isRecording) { "Recorder already started" }

        val file = File(appContext.externalCacheDir, "voice_${System.currentTimeMillis()}.3gp")
        outputFile = file
        startMs = System.currentTimeMillis()

        recorder = buildRecorder(file).apply {
            prepare()
            start()
        }

        return file
    }

    fun stop(): Result? {
        val file = outputFile

        val durationMs = (System.currentTimeMillis() - startMs).coerceAtLeast(0L)

        val stoppedOk = runCatching {
            recorder?.apply {
                try { stop() } catch (_: Exception) {}
                release()
            }
        }.isSuccess

        recorder = null

        if (!stoppedOk || file == null || !file.exists()) {
            cleanup()
            return null
        }

        outputFile = null
        startMs = 0L

        return Result(file = file, durationMs = durationMs)
    }

    fun cancel() {
        runCatching {
            recorder?.apply {
                try { stop() } catch (_: Exception) {}
                release()
            }
        }
        recorder = null
        cleanup()
    }

    fun release() {
        runCatching { recorder?.release() }
        recorder = null
        cleanup()
    }

    private fun cleanup() {
        runCatching { outputFile?.delete() }
        outputFile = null
        startMs = 0L
    }

    private fun buildRecorder(outFile: File): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(appContext).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outFile.absolutePath)
            }
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outFile.absolutePath)
            }
        }
    }

    data class Result(
        val file: File,
        val durationMs: Long
    )
}
