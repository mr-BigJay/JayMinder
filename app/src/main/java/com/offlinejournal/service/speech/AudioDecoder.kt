package com.offlinejournal.service.speech

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

internal object AudioDecoder {
    fun decodeToPcm16(context: Context, filePath: String, sampleRate: Int): ByteArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(filePath)

        var audioTrackIndex = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                break
            }
        }

        if (audioTrackIndex < 0) {
            extractor.release()
            return byteArrayOf()
        }

        extractor.selectTrack(audioTrackIndex)
        val format = extractor.getTrackFormat(audioTrackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: run {
            extractor.release()
            return byteArrayOf()
        }

        val sourceSampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        } else {
            sampleRate
        }
        val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        } else {
            1
        }

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val outputChunks = mutableListOf<ByteArray>()
        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false

        while (true) {
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex) ?: break
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

            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
            if (outputIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputIndex)
                if (outputBuffer != null && bufferInfo.size > 0) {
                    val chunk = ByteArray(bufferInfo.size)
                    outputBuffer.get(chunk)
                    outputChunks.add(chunk)
                }
                codec.releaseOutputBuffer(outputIndex, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    break
                }
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        val pcm = concatChunks(outputChunks)
        val mono = if (channelCount > 1) downmixToMono(pcm, channelCount) else pcm
        return if (sourceSampleRate != sampleRate) {
            resamplePcm16(mono, sourceSampleRate, sampleRate)
        } else {
            mono
        }
    }

    private fun concatChunks(chunks: List<ByteArray>): ByteArray {
        val totalSize = chunks.sumOf { it.size }
        val result = ByteArray(totalSize)
        var offset = 0
        for (chunk in chunks) {
            System.arraycopy(chunk, 0, result, offset, chunk.size)
            offset += chunk.size
        }
        return result
    }

    private fun downmixToMono(pcm: ByteArray, channels: Int): ByteArray {
        val shortCount = pcm.size / 2
        val frameCount = shortCount / channels
        val output = ByteArray(frameCount * 2)
        val buffer = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN)

        for (frame in 0 until frameCount) {
            var sum = 0
            for (ch in 0 until channels) {
                sum += buffer.getShort((frame * channels + ch) * 2).toInt()
            }
            val avg = (sum / channels).toShort()
            output[frame * 2] = (avg.toInt() and 0xFF).toByte()
            output[frame * 2 + 1] = ((avg.toInt() shr 8) and 0xFF).toByte()
        }
        return output
    }

    private fun resamplePcm16(pcm: ByteArray, fromRate: Int, toRate: Int): ByteArray {
        if (fromRate == toRate || pcm.isEmpty()) return pcm
        val inputSamples = pcm.size / 2
        val outputSamples = ((inputSamples.toLong() * toRate) / fromRate).toInt().coerceAtLeast(1)
        val input = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN)
        val output = ByteArray(outputSamples * 2)
        val outBuffer = ByteBuffer.wrap(output).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until outputSamples) {
            val srcIndex = (i.toDouble() * fromRate / toRate).roundToInt().coerceIn(0, inputSamples - 1)
            outBuffer.putShort(input.getShort(srcIndex * 2))
        }
        return output
    }
}
