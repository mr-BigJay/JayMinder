package com.offlinejournal.service.audio

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavFileUtils {
    fun writeWavHeader(
        output: FileOutputStream,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
        dataSizePlaceholder: Int = 0
    ) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(36 + dataSizePlaceholder)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1) // PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(blockAlign.toShort())
        header.putShort(bitsPerSample.toShort())
        header.put("data".toByteArray())
        header.putInt(dataSizePlaceholder)
        output.write(header.array())
    }

    fun finalizeWavFile(file: File, dataSize: Int) {
        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(4)
            raf.write(intToLittleEndian(36 + dataSize))
            raf.seek(40)
            raf.write(intToLittleEndian(dataSize))
        }
    }

    fun readPcm16Mono(file: File): ByteArray {
        FileInputStream(file).use { fis ->
            val header = ByteArray(44)
            if (fis.read(header) < 44) return byteArrayOf()
            val dataSize = ByteBuffer.wrap(header, 40, 4).order(ByteOrder.LITTLE_ENDIAN).int
            val buffer = ByteArray(dataSize)
            var read = 0
            while (read < dataSize) {
                val count = fis.read(buffer, read, dataSize - read)
                if (count <= 0) break
                read += count
            }
            return buffer.copyOf(read)
        }
    }

    private fun intToLittleEndian(value: Int): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
}
