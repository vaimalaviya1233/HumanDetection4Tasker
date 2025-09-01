package online.avogadro.opencv4tasker.notification

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import online.avogadro.opencv4tasker.tasker.NotificationInterceptedEvent
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class NotificationInterceptorService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationInterceptor"
        private const val TEMP_DIR = "notification_images"
        const val ACTION_NOTIFICATION_INTERCEPTED = "online.avogadro.opencv4tasker.NOTIFICATION_INTERCEPTED"
    }

    private lateinit var imageExtractor: NotificationImageExtractor
    private lateinit var fileManager: NotificationFileManager

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NotificationInterceptorService created")
        imageExtractor = NotificationImageExtractor(this)
        fileManager = NotificationFileManager(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            Log.d(TAG, "Notification posted from ${sbn.packageName}")
            
            // Extract basic notification info
            val notification = sbn.notification
            val packageName = sbn.packageName
            
            // Get app name
            val appName = getApplicationName(packageName)
            
            // Extract notification text
            val title = notification.extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = notification.extras.getString(Notification.EXTRA_TEXT) ?: ""
            
            Log.d(TAG, "Title: $title, Text: $text")
            
            // Check if notification has an image before processing
            if (!imageExtractor.hasImage(notification)) {
                Log.d(TAG, "No image found in notification, ignoring")
                return
            }
            
            // Try to extract image from notification
            val imageBitmap = imageExtractor.extractImage(notification)
            
            if (imageBitmap != null) {
                Log.d(TAG, "Image found in notification, saving to temp file")
                
                // Save image to temporary file
                val imageFile = fileManager.saveImageToTemp(imageBitmap, packageName)
                
                if (imageFile != null) {
                    Log.d(TAG, "Image saved to: ${imageFile.absolutePath}")
                    
                    // Trigger Tasker event
                    triggerTaskerEvent(title, text, imageFile.absolutePath, packageName, appName)
                } else {
                    Log.e(TAG, "Failed to save image to temporary file")
                }
            } else {
                Log.d(TAG, "No image found in notification, ignoring")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d(TAG, "Notification removed from ${sbn.packageName}")
    }

    private fun getApplicationName(packageName: String): String {
        return try {
            val packageManager = packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Could not get app name for package: $packageName")
            packageName // Fallback to package name
        }
    }

    private fun triggerTaskerEvent(
        title: String,
        text: String,
        imagePath: String,
        packageName: String,
        appName: String
    ) {
        try {
            Log.d(TAG, "Triggering Tasker event with data: title=$title, text=$text, imagePath=$imagePath, packageName=$packageName, appName=$appName")
            
            // Create broadcast intent with notification data
            val intent = Intent(ACTION_NOTIFICATION_INTERCEPTED).apply {
                putExtra("notification_title", title)
                putExtra("notification_text", text)
                putExtra("image_path", imagePath)
                putExtra("app_package", packageName)
                putExtra("app_name", appName)
            }
            
            sendBroadcast(intent)
            
            Log.d(TAG, "Broadcast sent successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering Tasker event", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "NotificationInterceptorService destroyed")
        
        // Clean up old temporary files
        fileManager.cleanupOldFiles()
    }
}
