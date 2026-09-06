package com.offlinejournal.service.speech

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

sealed class ModelInstallState {
    data object NotInstalled : ModelInstallState()
    data object Checking : ModelInstallState()
    data class Downloading(val progress: Float, val message: String) : ModelInstallState()
    data class Installed(val modelPath: String, val modelName: String) : ModelInstallState()
    data class Error(val message: String) : ModelInstallState()
}

/**
 * Manages the offline Vosk Persian model.
 * Priority: bundled assets → previously installed files → one-time download.
 */
class VoskModelManager(private val context: Context) {

    private val _state = MutableStateFlow<ModelInstallState>(ModelInstallState.NotInstalled)
    val state: StateFlow<ModelInstallState> = _state.asStateFlow()

    private val modelsRoot = File(context.filesDir, "vosk-models")
    private val installedMarker = File(modelsRoot, ".installed_model")

    companion object {
        const val DEFAULT_MODEL_ID = "vosk-model-small-fa-0.5"
        private const val ASSET_MODEL_PATH = "model/vosk-model-small-fa-0.5"
        private const val DOWNLOAD_URL =
            "https://alphacephei.com/vosk/models/vosk-model-small-fa-0.5.zip"
    }

    fun getModelDirectory(): File? {
        val path = when (val s = _state.value) {
            is ModelInstallState.Installed -> s.modelPath
            else -> installedMarker.takeIf { it.exists() }?.readText()?.trim()
        }
        return path?.let { File(it) }?.takeIf { it.exists() && File(it, "am").exists() }
    }

    suspend fun ensureModelInstalled(allowDownload: Boolean = true): Result<File> = withContext(Dispatchers.IO) {
        _state.value = ModelInstallState.Checking

        val existing = getModelDirectory() ?: findInstalledModel()
        if (existing != null) {
            markInstalled(existing)
            return@withContext Result.success(existing)
        }

        val fromAssets = copyFromAssets()
        if (fromAssets != null) {
            markInstalled(fromAssets)
            return@withContext Result.success(fromAssets)
        }

        if (!allowDownload) {
            val error = ModelInstallState.Error(
                "مدل تشخیص گفتار نصب نیست. از تنظیمات، مدل فارسی را دانلود کنید."
            )
            _state.value = error
            return@withContext Result.failure(IllegalStateException(error.message))
        }

        downloadAndInstall()
    }

    private fun findInstalledModel(): File? {
        if (!modelsRoot.exists()) return null
        modelsRoot.listFiles()?.forEach { dir ->
            if (dir.isDirectory && File(dir, "am").exists()) return dir
        }
        return null
    }

    private fun markInstalled(modelDir: File) {
        modelsRoot.mkdirs()
        installedMarker.writeText(modelDir.absolutePath)
        _state.value = ModelInstallState.Installed(modelDir.absolutePath, DEFAULT_MODEL_ID)
    }

    private fun copyFromAssets(): File? {
        return try {
            val assets = context.assets.list(ASSET_MODEL_PATH) ?: return null
            if (assets.isEmpty()) return null

            val target = File(modelsRoot, DEFAULT_MODEL_ID)
            if (target.exists()) target.deleteRecursively()
            copyAssetFolder(ASSET_MODEL_PATH, target)
            if (File(target, "am").exists()) target else null
        } catch (_: Exception) {
            null
        }
    }

    private fun copyAssetFolder(assetPath: String, targetDir: File) {
        val files = context.assets.list(assetPath) ?: return
        if (files.isEmpty()) {
            targetDir.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                targetDir.outputStream().use { output -> input.copyTo(output) }
            }
            return
        }
        targetDir.mkdirs()
        for (name in files) {
            val childAsset = "$assetPath/$name"
            val childTarget = File(targetDir, name)
            val sub = context.assets.list(childAsset)
            if (sub != null && sub.isNotEmpty()) {
                copyAssetFolder(childAsset, childTarget)
            } else {
                context.assets.open(childAsset).use { input ->
                    childTarget.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }

    private suspend fun downloadAndInstall(): Result<File> {
        return try {
            _state.value = ModelInstallState.Downloading(0f, "شروع دانلود مدل فارسی…")

            val zipFile = File(context.cacheDir, "$DEFAULT_MODEL_ID.zip")
            downloadFile(DOWNLOAD_URL, zipFile) { progress ->
                _state.value = ModelInstallState.Downloading(
                    progress,
                    "دانلود مدل فارسی… ${(progress * 100).toInt()}٪"
                )
            }

            _state.value = ModelInstallState.Downloading(1f, "استخراج مدل…")
            val target = File(modelsRoot, DEFAULT_MODEL_ID)
            if (target.exists()) target.deleteRecursively()
            unzip(zipFile, modelsRoot)

            // Zip usually contains vosk-model-small-fa-0.5/ folder
            val modelDir = File(modelsRoot, DEFAULT_MODEL_ID).takeIf { File(it, "am").exists() }
                ?: modelsRoot.listFiles()?.firstOrNull { it.isDirectory && File(it, "am").exists() }

            zipFile.delete()

            if (modelDir == null) {
                throw IllegalStateException("ساختار مدل دانلود‌شده نامعتبر است")
            }

            markInstalled(modelDir)
            Result.success(modelDir)
        } catch (e: Exception) {
            _state.value = ModelInstallState.Error(
                "خطا در نصب مدل: ${e.message ?: "نامشخص"}"
            )
            Result.failure(e)
        }
    }

    private fun downloadFile(url: String, target: File, onProgress: (Float) -> Unit) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 30_000
        connection.readTimeout = 120_000
        connection.instanceFollowRedirects = true
        connection.connect()

        val total = connection.contentLengthLong.coerceAtLeast(1L)
        connection.inputStream.use { input ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(8192)
                var downloaded = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    downloaded += read
                    onProgress((downloaded.toFloat() / total).coerceIn(0f, 0.99f))
                }
            }
        }
        onProgress(1f)
    }

    private fun unzip(zipFile: File, destDir: File) {
        destDir.mkdirs()
        ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
