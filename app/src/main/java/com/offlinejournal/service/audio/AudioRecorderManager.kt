package com.offlinejournal.service.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class AudioRecorderManager(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val isRecordingFlag = AtomicBoolean(false)
    private var currentFile: File? = null
    private var mediaPlayer: MediaPlayer? = null
    private var pcmListener: ((ByteArray) -> Unit)? = null
    private var bytesWritten = 0

    val isRecording: Boolean
        get() = isRecordingFlag.get()

    fun setPcmListener(listener: ((ByteArray) -> Unit)?) {
        pcmListener = listener
    }

    fun startRecording(): Result<File> {
        return try {
            stopPlayback()
            val minBuffer = AudioRecord.getMinBufferSize(
                PcmAudioFormat.SAMPLE_RATE,
                PcmAudioFormat.CHANNEL_CONFIG,
                PcmAudioFormat.AUDIO_FORMAT
            )
            if (minBuffer <= 0) {
                return Result.failure(IllegalStateException("میکروفون در دسترس نیست"))
            }

            val bufferSize = minBuffer * 4
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                PcmAudioFormat.SAMPLE_RATE,
                PcmAudioFormat.CHANNEL_CONFIG,
                PcmAudioFormat.AUDIO_FORMAT,
                bufferSize
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                return Result.failure(IllegalStateException("میکروفون مقداردهی نشد"))
            }

            val audioDir = File(context.filesDir, "recordings").apply { mkdirs() }
            val file = File(audioDir, "recording_${UUID.randomUUID()}.wav")
            val output = FileOutputStream(file)
            WavFileUtils.writeWavHeader(
                output,
                PcmAudioFormat.SAMPLE_RATE,
                channels = 1,
                bitsPerSample = 16
            )

            audioRecord = recorder
            currentFile = file
            bytesWritten = 0
            isRecordingFlag.set(true)

            recorder.startRecording()
            recordingThread = thread(name = "wav-recorder") {
                val buffer = ByteArray(bufferSize)
                try {
                    while (isRecordingFlag.get()) {
                        val read = recorder.read(buffer, 0, buffer.size)
                        if (read > 0) {
                            output.write(buffer, 0, read)
                            bytesWritten += read
                            pcmListener?.invoke(buffer.copyOf(read))
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    output.flush()
                    output.close()
                    WavFileUtils.finalizeWavFile(file, bytesWritten)
                }
            }
            Result.success(file)
        } catch (e: Exception) {
            cleanupRecording(deleteFile = true)
            Result.failure(e)
        }
    }

    fun stopRecording(): File? {
        if (!isRecordingFlag.get()) return currentFile
        isRecordingFlag.set(false)
        try {
            audioRecord?.stop()
        } catch (_: Exception) {
        }
        recordingThread?.join(3000)
        cleanupRecording(deleteFile = false)
        return currentFile
    }

    fun cancelRecording() {
        isRecordingFlag.set(false)
        recordingThread?.join(3000)
        cleanupRecording(deleteFile = true)
        currentFile = null
    }

    private fun cleanupRecording(deleteFile: Boolean) {
        audioRecord?.release()
        audioRecord = null
        recordingThread = null
        if (deleteFile) {
            currentFile?.delete()
        }
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

    fun getRecordingDurationMs(filePath: String): Long {
        return try {
            if (filePath.endsWith(".wav", ignoreCase = true)) {
                val file = File(filePath)
                val pcmBytes = file.length() - 44
                if (pcmBytes <= 0) return 0
                (pcmBytes * 1000) / (PcmAudioFormat.SAMPLE_RATE * PcmAudioFormat.BYTES_PER_SAMPLE)
            } else {
                MediaMetadataRetriever().run {
                    setDataSource(filePath)
                    val duration = extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    release()
                    duration?.toLongOrNull() ?: 0
                }
            }
        } catch (_: Exception) {
            0
        }
    }
}
