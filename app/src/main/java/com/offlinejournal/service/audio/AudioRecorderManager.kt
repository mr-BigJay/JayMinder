package com.offlinejournal.service.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.UUID

class AudioRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var mediaPlayer: MediaPlayer? = null

    val isRecording: Boolean
        get() = mediaRecorder != null

    fun startRecording(): Result<File> {
        return try {
            stopPlayback()
            val audioDir = File(context.filesDir, "recordings").apply { mkdirs() }
            val file = File(audioDir, "recording_${UUID.randomUUID()}.m4a")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioSamplingRate(16000)
            recorder.setAudioEncodingBitRate(128000)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            currentFile = file
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun stopRecording(): File? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            currentFile
        } catch (e: Exception) {
            mediaRecorder?.release()
            mediaRecorder = null
            null
        }
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
        }
        mediaRecorder = null
        currentFile?.delete()
        currentFile = null
    }

    fun playAudio(filePath: String, onComplete: () -> Unit = {}) {
        stopPlayback()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
            setOnCompletionListener {
                release()
                mediaPlayer = null
                onComplete()
            }
            start()
        }
    }

    fun stopPlayback() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    fun deleteAudioFile(filePath: String?) {
        if (filePath != null) {
            File(filePath).delete()
        }
    }
}
