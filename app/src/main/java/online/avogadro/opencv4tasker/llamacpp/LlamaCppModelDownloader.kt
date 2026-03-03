package online.avogadro.opencv4tasker.llamacpp

import android.content.Context
import android.os.StatFs
import android.util.Log
import online.avogadro.opencv4tasker.app.SharedPreferencesHelper
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object LlamaCppModelDownloader {
    private const val TAG = "LlamaCppModelDownloader"
    private const val BUFFER_SIZE = 8192
    private const val CONNECT_TIMEOUT = 30_000
    private const val READ_TIMEOUT = 60_000

    fun getModelsDir(context: Context): File {
        val dir = File(context.getExternalFilesDir("models"), "llamacpp")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun isModelDownloaded(context: Context, modelInfo: GgufModelInfo): Boolean {
        val dir = getModelsDir(context)
        return File(dir, modelInfo.modelFilename).exists() && File(dir, modelInfo.mmprojFilename).exists()
    }

    fun getModelPath(context: Context, modelInfo: GgufModelInfo): String {
        return File(getModelsDir(context), modelInfo.modelFilename).absolutePath
    }

    fun getMmprojPath(context: Context, modelInfo: GgufModelInfo): String {
        return File(getModelsDir(context), modelInfo.mmprojFilename).absolutePath
    }

    fun deleteModel(context: Context, modelInfo: GgufModelInfo) {
        val dir = getModelsDir(context)
        File(dir, modelInfo.modelFilename).delete()
        File(dir, modelInfo.mmprojFilename).delete()

        // Clear saved paths if they pointed to this model
        val savedModel = SharedPreferencesHelper.get(context, SharedPreferencesHelper.LLAMACPP_MODEL_PATH)
        if (savedModel == getModelPath(context, modelInfo)) {
            SharedPreferencesHelper.save(context, SharedPreferencesHelper.LLAMACPP_MODEL_PATH, "")
            SharedPreferencesHelper.save(context, SharedPreferencesHelper.LLAMACPP_MMPROJ_PATH, "")
        }
    }

    /**
     * Download model files. Must be called from a background thread.
     * @param progressCallback receives progress 0-100 (combines both files)
     * @return true on success
     */
    fun downloadModel(
        context: Context,
        modelInfo: GgufModelInfo,
        progressCallback: (Int) -> Unit
    ): Boolean {
        val dir = getModelsDir(context)

        // Check available space (need totalSizeMB + 100MB buffer)
        val stat = StatFs(dir.absolutePath)
        val availableMB = stat.availableBytes / (1024 * 1024)
        if (availableMB < modelInfo.totalSizeMB + 100) {
            Log.e(TAG, "Not enough space: ${availableMB}MB available, need ${modelInfo.totalSizeMB + 100}MB")
            return false
        }

        val totalSize = modelInfo.totalSizeMB.toLong() * 1024 * 1024
        var downloadedTotal = 0L

        // Download model file
        val modelFile = File(dir, modelInfo.modelFilename)
        val modelOk = downloadFile(modelInfo.modelUrl, modelFile) { bytesDownloaded ->
            downloadedTotal = bytesDownloaded
            progressCallback(((downloadedTotal * 100) / totalSize).toInt().coerceIn(0, 99))
        }
        if (!modelOk) return false

        val modelFileSize = modelFile.length()

        // Download mmproj file
        val mmprojFile = File(dir, modelInfo.mmprojFilename)
        val mmprojOk = downloadFile(modelInfo.mmprojUrl, mmprojFile) { bytesDownloaded ->
            downloadedTotal = modelFileSize + bytesDownloaded
            progressCallback(((downloadedTotal * 100) / totalSize).toInt().coerceIn(0, 99))
        }
        if (!mmprojOk) {
            modelFile.delete() // Clean up partial download
            return false
        }

        // Save paths to preferences
        SharedPreferencesHelper.save(context, SharedPreferencesHelper.LLAMACPP_MODEL_PATH, modelFile.absolutePath)
        SharedPreferencesHelper.save(context, SharedPreferencesHelper.LLAMACPP_MMPROJ_PATH, mmprojFile.absolutePath)

        progressCallback(100)
        return true
    }

    private fun downloadFile(
        urlStr: String,
        destFile: File,
        progressCallback: (Long) -> Unit
    ): Boolean {
        var connection: HttpURLConnection? = null
        try {
            val partialFile = File(destFile.absolutePath + ".part")
            var existingSize = 0L

            val url = URL(urlStr)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.instanceFollowRedirects = true

            // Resume support
            if (partialFile.exists()) {
                existingSize = partialFile.length()
                connection.setRequestProperty("Range", "bytes=$existingSize-")
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                Log.e(TAG, "Download failed: HTTP $responseCode for $urlStr")
                return false
            }

            // If server doesn't support range, restart from scratch
            if (responseCode == HttpURLConnection.HTTP_OK && existingSize > 0) {
                existingSize = 0L
                partialFile.delete()
            }

            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(partialFile, existingSize > 0)
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            var totalRead = existingSize

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                progressCallback(totalRead)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // Rename .part to final name
            if (destFile.exists()) destFile.delete()
            if (!partialFile.renameTo(destFile)) {
                Log.e(TAG, "Failed to rename partial file")
                return false
            }

            Log.i(TAG, "Downloaded: ${destFile.name} (${destFile.length()} bytes)")
            return true

        } catch (e: IOException) {
            Log.e(TAG, "Download error: ${e.message}", e)
            return false
        } finally {
            connection?.disconnect()
        }
    }
}
