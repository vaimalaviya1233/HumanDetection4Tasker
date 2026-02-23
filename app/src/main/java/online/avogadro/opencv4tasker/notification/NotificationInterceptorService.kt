package online.avogadro.opencv4tasker.notification

import android.app.Notification
import android.content.Intent
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import online.avogadro.opencv4tasker.app.OpenCV4TaskerApplication
import online.avogadro.opencv4tasker.app.SharedPreferencesHelper
import online.avogadro.opencv4tasker.tasker.NotificationInterceptedEvent
import online.avogadro.opencv4tasker.tasker.NotificationRaiser

class NotificationInterceptorService : NotificationListenerService() {

    private val DEBUG=false;

    companion object {
        private const val TAG = "NotificationInterceptor"
        private const val TEMP_DIR = "notification_images"
        const val ACTION_NOTIFICATION_INTERCEPTED = "online.avogadro.opencv4tasker.NOTIFICATION_INTERCEPTED"

        private var instance: NotificationInterceptorService? = null

        fun getInstance(): NotificationInterceptorService? = instance

        fun cancelNotificationByKey(notificationKey: String): Boolean {
            return try {
                instance?.cancelNotification(notificationKey)
                Log.d(TAG, "Successfully canceled notification with key: $notificationKey")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error canceling notification with key: $notificationKey", e)
                false
            }
        }
    }

    private lateinit var imageExtractor: NotificationImageExtractor
    private lateinit var fileManager: NotificationFileManager

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NotificationInterceptorService created")
        imageExtractor = NotificationImageExtractor(this)
        fileManager = NotificationFileManager(this)
        instance = this
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            Log.d(TAG, "Notification posted from ${sbn.packageName}")
            
            // Check if event is enabled
            val isEventEnabled = SharedPreferencesHelper.getBoolean(
                this, 
                SharedPreferencesHelper.NOTIFICATION_EVENT_ENABLED, 
                false
            )
            
            if (!isEventEnabled) {
                Log.d(TAG, "Notification event is disabled, ignoring")
                return
            }
            
            // Extract basic notification info
            val notification = sbn.notification
            val packageName = sbn.packageName
            val notificationKey = sbn.key

            // Get app name
            val appName = getApplicationName(packageName)

            // Extract notification text
            val title = notification.extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = notification.extras.getString(Notification.EXTRA_TEXT) ?: ""

            if (DEBUG) {
                // Trigger debug event with all notification.extras values
                val debugText = buildDebugText(sbn.notification)
                triggerTaskerEvent(title, debugText, "", packageName, appName, notificationKey)
                // triggerDebugEvent(debugText, sbn.packageName)
                return;
            }

            Log.d(TAG, "Title: $title, Text: $text")

            var imagePath = ""

            // Check if notification has an image and extract it if present
            if (imageExtractor.hasImage(notification)) {
                Log.d(TAG, "Image found in notification, extracting it")

                // Try to extract image from notification
                val imageBitmap = imageExtractor.extractImage(notification)

                if (imageBitmap != null) {
                    Log.d(TAG, "Image extracted, saving to temp file")

                    // Save image to temporary file
                    val imageFile = fileManager.saveImageToTemp(imageBitmap, packageName)

                    if (imageFile != null) {
                        Log.d(TAG, "Image saved to: ${imageFile.absolutePath}")
                        imagePath = imageFile.absolutePath
                    } else {
                        Log.e(TAG, "Failed to save image to temporary file")
                    }
                } else {
                    Log.d(TAG, "Failed to extract image from notification")
                }
            } else {
                Log.d(TAG, "No image found in notification")
            }

            // Trigger Tasker event for all notifications (with or without images)
            // The filtering based on image requirement will happen in the event condition logic
            triggerTaskerEvent(title, text, imagePath, packageName, appName, notificationKey)
            
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

    private fun buildDebugText(notification: Notification): String {
        val debugInfo = StringBuilder()
        val extras = notification.extras
        
        // Iterate through all extras and build debug text
        for (key in extras.keySet()) {
            val value = extras.get(key)
            debugInfo.append("$key: $value\n")
        }
        
        return debugInfo.toString().trimEnd('\n')
    }

    private fun triggerTaskerEvent(
        title: String,
        notificationText: String,
        imagePath: String,
        packageName: String,
        appName: String,
        notificationKey: String
    ) {
        try {
            Log.d(TAG, "Triggering Tasker event with data: title=$title, text=$notificationText, imagePath=$imagePath, packageName=$packageName, appName=$appName, key=$notificationKey")

            val notificationData = NotificationInterceptedEvent(title, notificationText, imagePath, appName, packageName, notificationKey)

            notificationData.notificationTitle = title
            notificationData.notificationText = notificationText
            notificationData.appPackage = packageName
            notificationData.appName = appName
            notificationData.imagePath = imagePath
            notificationData.notificationKey = notificationKey

            // NOTE: The broadcast implementation below is intentionally kept as dead code for future reference.
            // The active implementation uses NotificationRaiser (Tasker event approach).
            if (1==1) {
                NotificationRaiser.raiseAlarmEvent(OpenCV4TaskerApplication.getInstance(), notificationData)
                return;
            }

            // Broadcast approach — kept for reference, not currently used
            val intent = Intent(ACTION_NOTIFICATION_INTERCEPTED).apply {
                putExtra("notification_title", title)
                putExtra("notification_text", notificationText)
                putExtra("image_path", imagePath)
                putExtra("app_package", packageName)
                putExtra("app_name", appName)
                putExtra("notification_key", notificationKey)
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

        // Clear the instance reference
        instance = null
    }
}
