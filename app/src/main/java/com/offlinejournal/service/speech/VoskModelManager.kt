package com.offlinejournal.service.speech

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

sealed class ModelInstallState {
    data object NotInstalled : ModelInstallState()
    data object Checking : ModelInstallState()
    data class Installing(val message: String) : ModelInstallState()
    data class Installed(val modelPath: String, val modelName: String) : ModelInstallState()
    data class Error(val message: String) : ModelInstallState()
}

/**
 * Installs the bundled Vosk Persian model from APK assets to app storage on first use.
 * No internet download required — model ships inside the app.
 */
class VoskModelManager(private val context: Context) {

    private val _state = MutableStateFlow<ModelInstallState>(ModelInstallState.NotInstalled)
    val state: StateFlow<ModelInstallState> = _state.asStateFlow()

    private val modelsRoot = File(context.filesDir, "vosk-models")
    private val installedMarker = File(modelsRoot, ".installed_model")

    companion object {
        const val DEFAULT_MODEL_ID = "vosk-model-small-fa-0.5"
        private const val ASSET_MODEL_PATH = "model/vosk-model-small-fa-0.5"
    }

    fun getModelDirectory(): File? {
        val path = when (val s = _state.value) {
            is ModelInstallState.Installed -> s.modelPath
            else -> installedMarker.takeIf { it.exists() }?.readText()?.trim()
        }
        return path?.let { File(it) }?.takeIf { it.exists() && File(it, "am").exists() }
    }

    suspend fun ensureModelInstalled(): Result<File> = withContext(Dispatchers.IO) {
        _state.value = ModelInstallState.Checking

        val existing = getModelDirectory() ?: findInstalledModel()
        if (existing != null) {
            markInstalled(existing)
            return@withContext Result.success(existing)
        }

        installFromBundledAssets()
    }

    private fun findInstalledModel(): File? {
        val cached = File(modelsRoot, DEFAULT_MODEL_ID)
        if (cached.isDirectory && File(cached, "am").exists()) return cached
        if (!modelsRoot.exists()) return null
        return modelsRoot.listFiles()?.firstOrNull { it.isDirectory && File(it, "am").exists() }
    }

    private fun markInstalled(modelDir: File) {
        modelsRoot.mkdirs()
        installedMarker.writeText(modelDir.absolutePath)
        _state.value = ModelInstallState.Installed(modelDir.absolutePath, DEFAULT_MODEL_ID)
    }

    private fun installFromBundledAssets(): Result<File> {
        return try {
            val assets = context.assets.list(ASSET_MODEL_PATH) ?: emptyArray()
            if (assets.isEmpty()) {
                throw IllegalStateException("مدل فارسی در اپلیکیشن پیدا نشد")
            }

            _state.value = ModelInstallState.Installing("آماده‌سازی مدل تشخیص گفتار…")

            val target = File(modelsRoot, DEFAULT_MODEL_ID)
            if (target.exists()) target.deleteRecursively()
            copyAssetFolder(ASSET_MODEL_PATH, target)

            if (!File(target, "am").exists()) {
                throw IllegalStateException("مدل استخراج‌شده نامعتبر است")
            }

            markInstalled(target)
            Result.success(target)
        } catch (e: Exception) {
            _state.value = ModelInstallState.Error(
                e.message ?: "خطا در آماده‌سازی مدل تشخیص گفتار"
            )
            Result.failure(e)
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
}
