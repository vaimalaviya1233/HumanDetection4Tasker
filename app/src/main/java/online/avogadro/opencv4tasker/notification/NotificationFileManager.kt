package online.avogadro.opencv4tasker.notification

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class NotificationFileManager(private val context: Context) {

    companion object {
        private const val TAG = "NotificationFileManager"
        private const val TEMP_DIR = "notification_images"
        private const val FILE_PREFIX = "notification_"
        private const val FILE_EXTENSION = ".jpg"
        private const val CLEANUP_INTERVAL_HOURS = 24
        private const val JPEG_QUALITY = 85
    }

    private val tempDirectory: File by lazy {
        File(context.cacheDir, TEMP_DIR).also { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
                Log.d(TAG, "Created temp directory: ${dir.absolutePath}")
            }
        }
    }

    /**
     * Saves a bitmap to a temporary file and returns the file
     */
    fun saveImageToTemp(bitmap: Bitmap, packageName: String): File? {
        return try {
            // Create unique filename with timestamp and package name
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
            val sanitizedPackageName = packageName.replace(".", "_")
            val filename = "${FILE_PREFIX}${sanitizedPackageName}_${timestamp}${FILE_EXTENSION}"
            
            val file = File(tempDirectory, filename)
            
            // Save bitmap to file
            FileOutputStream(file).use { outputStream ->
                val success = bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
                if (success) {
                    Log.d(TAG, "Image saved successfully to: ${file.absolutePath} (${file.length()} bytes)")
                    file
                } else {
                    Log.e(TAG, "Failed to compress bitmap to file")
                    null
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error saving image to temp file", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error saving image", e)
            null
        }
    }

    /**
     * Cleans up old temporary files older than CLEANUP_INTERVAL_HOURS
     */
    fun cleanupOldFiles() {
        try {
            if (!tempDirectory.exists()) {
                Log.d(TAG, "Temp directory doesn't exist, nothing to clean up")
                return
            }

            val currentTime = System.currentTimeMillis()
            val cutoffTime = currentTime - (CLEANUP_INTERVAL_HOURS * 60 * 60 * 1000L)
            
            val files = tempDirectory.listFiles()
            if (files == null || files.isEmpty()) {
                Log.d(TAG, "No files to clean up")
                return
            }

            var deletedCount = 0
            var totalSize = 0L

            for (file in files) {
                if (file.isFile && file.lastModified() < cutoffTime) {
                    totalSize += file.length()
                    if (file.delete()) {
                        deletedCount++
                        Log.d(TAG, "Deleted old file: ${file.name}")
                    } else {
                        Log.w(TAG, "Failed to delete old file: ${file.name}")
                    }
                }
            }

            if (deletedCount > 0) {
                Log.i(TAG, "Cleanup completed: deleted $deletedCount files, freed ${formatFileSize(totalSize)}")
            } else {
                Log.d(TAG, "No old files to clean up")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    /**
     * Forces cleanup of all temporary files (useful for testing or manual cleanup)
     */
    fun cleanupAllFiles() {
        try {
            if (!tempDirectory.exists()) {
                Log.d(TAG, "Temp directory doesn't exist, nothing to clean up")
                return
            }

            val files = tempDirectory.listFiles()
            if (files == null || files.isEmpty()) {
                Log.d(TAG, "No files to clean up")
                return
            }

            var deletedCount = 0
            var totalSize = 0L

            for (file in files) {
                if (file.isFile) {
                    totalSize += file.length()
                    if (file.delete()) {
                        deletedCount++
                        Log.d(TAG, "Deleted file: ${file.name}")
                    } else {
                        Log.w(TAG, "Failed to delete file: ${file.name}")
                    }
                }
            }

            Log.i(TAG, "Force cleanup completed: deleted $deletedCount files, freed ${formatFileSize(totalSize)}")

        } catch (e: Exception) {
            Log.e(TAG, "Error during force cleanup", e)
        }
    }

    /**
     * Returns information about the temp directory
     */
    fun getTempDirectoryInfo(): String {
        return try {
            if (!tempDirectory.exists()) {
                "Temp directory doesn't exist"
            } else {
                val files = tempDirectory.listFiles()
                if (files == null || files.isEmpty()) {
                    "Temp directory is empty"
                } else {
                    val totalSize = files.filter { it.isFile }.sumOf { it.length() }
                    "Temp directory: ${files.size} files, ${formatFileSize(totalSize)}"
                }
            }
        } catch (e: Exception) {
            "Error getting temp directory info: ${e.message}"
        }
    }

    /**
     * Checks if temp directory has enough space (basic check)
     */
    fun hasEnoughSpace(requiredBytes: Long = 10 * 1024 * 1024): Boolean { // Default 10MB
        return try {
            tempDirectory.usableSpace > requiredBytes
        } catch (e: Exception) {
            Log.e(TAG, "Error checking available space", e)
            false
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}
