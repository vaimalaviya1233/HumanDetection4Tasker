package online.avogadro.opencv4tasker.notification

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log

class NotificationImageExtractor(private val context: Context) {

    companion object {
        private const val TAG = "NotificationImageExtractor"
    }

    /**
     * Extracts image from notification. Returns null if no image is found.
     */
    fun extractImage(notification: Notification): Bitmap? {
        try {
            // Method 1: Check for large icon
            val largeIcon = extractLargeIcon(notification)
            if (largeIcon != null) {
                Log.d(TAG, "Found image in large icon")
                return largeIcon
            }

            // Method 2: Check for picture in extras (BigPictureStyle)
            val pictureFromExtras = extractPictureFromExtras(notification)
            if (pictureFromExtras != null) {
                Log.d(TAG, "Found image in notification extras")
                return pictureFromExtras
            }

            // Method 3: Check for other image sources in extras
            val otherImage = extractOtherImageFromExtras(notification)
            if (otherImage != null) {
                Log.d(TAG, "Found image in other extras")
                return otherImage
            }

            Log.d(TAG, "No image found in notification")
            return null

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting image from notification", e)
            return null
        }
    }

    private fun extractLargeIcon(notification: Notification): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // For API 23+, use Icon
                val largeIcon = notification.getLargeIcon()
                if (largeIcon != null) {
                    iconToBitmap(largeIcon)
                } else {
                    null
                }
            } else {
                // For older versions, use deprecated largeIcon
                @Suppress("DEPRECATION")
                notification.largeIcon
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting large icon", e)
            null
        }
    }

    private fun extractPictureFromExtras(notification: Notification): Bitmap? {
        return try {
            val extras = notification.extras
            
            // Check for BigPictureStyle picture
            val picture = extras.getParcelable<Bitmap>(Notification.EXTRA_PICTURE)
            if (picture != null) {
                return picture
            }

            // Check for picture icon
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pictureIcon = extras.getParcelable<Icon>(Notification.EXTRA_PICTURE_ICON)
                if (pictureIcon != null) {
                    return iconToBitmap(pictureIcon)
                }
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting picture from extras", e)
            null
        }
    }

    private fun extractOtherImageFromExtras(notification: Notification): Bitmap? {
        return try {
            val extras = notification.extras

            // Check for background image bitmap
            val backgroundBitmap = extras.getParcelable<Bitmap>(Notification.EXTRA_BACKGROUND_IMAGE_URI)
            if (backgroundBitmap != null) {
                return backgroundBitmap
            }

            // Check for other possible bitmap keys
            val possibleBitmapKeys = arrayOf(
                "android.rebuild.largeIcon",
                "android.icon",
                "android.picture",
                "android.bigLargeIcon"
            )

            for (key in possibleBitmapKeys) {
                val bitmap = extras.getParcelable<Bitmap>(key)
                if (bitmap != null) {
                    Log.d(TAG, "Found bitmap in key: $key")
                    return bitmap
                }
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting other images from extras", e)
            null
        }
    }

    private fun iconToBitmap(icon: Icon): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val drawable = icon.loadDrawable(context)
                if (drawable is BitmapDrawable) {
                    drawable.bitmap
                } else if (drawable != null) {
                    // Convert drawable to bitmap
                    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
                    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
                    
                    val bitmap = Bitmap.createBitmap(
                        width,
                        height,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = android.graphics.Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bitmap
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting icon to bitmap", e)
            null
        }
    }

    /**
     * Checks if the notification likely contains an image based on style and extras
     */
    fun hasImage(notification: Notification): Boolean {
        try {
            // Quick check without actually extracting the image
            
            // Check for large icon
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (notification.getLargeIcon() != null) {
                    return true
                }
            } else {
                @Suppress("DEPRECATION")
                if (notification.largeIcon != null) {
                    return true
                }
            }

            // Check for BigPicture style
            val extras = notification.extras
            if (extras.containsKey(Notification.EXTRA_PICTURE) ||
                extras.containsKey(Notification.EXTRA_PICTURE_ICON)) {
                return true
            }

            // Check notification template/style
            val template = extras.getString(Notification.EXTRA_TEMPLATE)
            if (template != null && template.contains("BigPicture", ignoreCase = true)) {
                return true
            }

            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if notification has image", e)
            return false
        }
    }
}
