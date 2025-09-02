package online.avogadro.opencv4tasker.notification

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.InputStream
import java.net.URL

class NotificationImageExtractor(private val context: Context) {

    companion object {
        private const val TAG = "NotificationImageExtractor"
    }

    /**
     * Extracts image from notification. Returns null if no image is found.
     * Prioritizes larger images when multiple versions are available.
     */
    fun extractImage(notification: Notification): Bitmap? {
        try {
            // Method 1: Check for BigPicture style images (largest)
            val pictureFromExtras = extractPictureFromExtras(notification)
            if (pictureFromExtras != null) {
                Log.d(TAG, "Found image in BigPicture extras")
                return pictureFromExtras
            }

            // Method 2: Check for EXTRA_LARGE_ICON_BIG (expanded large icon)
            val largeBigIcon = extractLargeBigIcon(notification)
            if (largeBigIcon != null) {
                Log.d(TAG, "Found image in EXTRA_LARGE_ICON_BIG")
                return largeBigIcon
            }

            // Method 3: Check for standard large icon
            val largeIcon = extractLargeIcon(notification)
            if (largeIcon != null) {
                Log.d(TAG, "Found image in large icon")
                return largeIcon
            }

            // Method 4: Check for other image sources in extras
            val otherImage = extractOtherImageFromExtras(notification)
            if (otherImage != null) {
                Log.d(TAG, "Found image in other extras")
                return otherImage
            }

            // Method 5: Check for URI-based images
            val uriImage = extractImageFromUri(notification)
            if (uriImage != null) {
                Log.d(TAG, "Found image from URI")
                return uriImage
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

    private fun extractLargeBigIcon(notification: Notification): Bitmap? {
        return try {
            val extras = notification.extras
            
            // Check for EXTRA_LARGE_ICON_BIG (android.largeIcon.big)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val largeBigIcon = extras.getParcelable<Icon>("android.largeIcon.big")
                if (largeBigIcon != null) {
                    return iconToBitmap(largeBigIcon)
                }
            }

            // Also check as bitmap fallback
            val largeBigBitmap = extras.getParcelable<Bitmap>("android.largeIcon.big")
            if (largeBigBitmap != null) {
                return largeBigBitmap
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting EXTRA_LARGE_ICON_BIG", e)
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

            // Skip background images as they are primarily aesthetic
            // Skip EXTRA_LARGE_ICON_BIG as it's handled in extractLargeBigIcon()

            // Check for other possible bitmap keys
            val possibleBitmapKeys = arrayOf(
                "android.rebuild.largeIcon",
                "android.icon", 
                "android.picture",
                "android.bigLargeIcon",
                "android.media.metadata.ART",  // MediaMetadata artwork
                "fcm_image",  // FCM custom image field
                "image_url",  // Common custom field
                "large_icon_url"  // Vendor-specific field
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

    private fun extractImageFromUri(notification: Notification): Bitmap? {
        return try {
            val extras = notification.extras
            
            // Check for URI-based image fields (excluding background images)
            val possibleUriKeys = arrayOf(
                "android.media.metadata.ALBUM_ART_URI",
                "image_url",
                "large_icon_url",
                "fcm_image_url"
            )

            for (key in possibleUriKeys) {
                val uriString = extras.getString(key)
                if (!uriString.isNullOrEmpty()) {
                    Log.d(TAG, "Found URI in key: $key = $uriString")
                    val bitmap = loadImageFromUri(uriString)
                    if (bitmap != null) {
                        return bitmap
                    }
                }
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting image from URI", e)
            null
        }
    }

    private fun loadImageFromUri(uriString: String): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            when (uri.scheme?.lowercase()) {
                "content" -> {
                    // Content URI - use ContentResolver
                    val inputStream = context.contentResolver.openInputStream(uri)
                    inputStream?.use { BitmapFactory.decodeStream(it) }
                }
                "file" -> {
                    // File URI
                    BitmapFactory.decodeFile(uri.path)
                }
                "http", "https" -> {
                    // HTTP/HTTPS URL - load from web (should be done in background thread)
                    Log.w(TAG, "HTTP URI found but not loading synchronously: $uriString")
                    null // Don't load HTTP images synchronously to avoid blocking
                }
                else -> {
                    Log.w(TAG, "Unsupported URI scheme: ${uri.scheme}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image from URI: $uriString", e)
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

            // Check for EXTRA_LARGE_ICON_BIG
            if (extras.containsKey("android.largeIcon.big")) {
                return true
            }

            // Check for URI-based images (excluding background images)
            val uriKeys = arrayOf(
                "android.media.metadata.ALBUM_ART_URI",
                "image_url",
                "large_icon_url",
                "fcm_image_url"
            )
            for (key in uriKeys) {
                if (extras.containsKey(key) && !extras.getString(key).isNullOrEmpty()) {
                    return true
                }
            }

            // Check for other bitmap fields
            val bitmapKeys = arrayOf(
                "android.media.metadata.ART",
                "fcm_image",
                "image_url",
                "large_icon_url"
            )
            for (key in bitmapKeys) {
                if (extras.containsKey(key)) {
                    return true
                }
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
