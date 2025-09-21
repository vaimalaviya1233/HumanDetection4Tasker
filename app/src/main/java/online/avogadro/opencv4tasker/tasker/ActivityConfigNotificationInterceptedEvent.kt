package online.avogadro.opencv4tasker.tasker

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import online.avogadro.opencv4tasker.databinding.ActivityConfigNotificationInterceptedEventBinding
import online.avogadro.opencv4tasker.notification.NotificationInterceptorService
import online.avogadro.opencv4tasker.app.SharedPreferencesHelper

class ActivityConfigNotificationInterceptedEvent : Activity(), TaskerPluginConfig<NotificationInterceptedEventInput> {

    companion object {
        private const val TAG = "NotificationEventConfig"
        private const val REQUEST_NOTIFICATION_ACCESS = 1001
    }

    private lateinit var binding: ActivityConfigNotificationInterceptedEventBinding

    override fun assignFromInput(input: TaskerInput<NotificationInterceptedEventInput>) {
        // Set the text field state based on the input
        binding.editTextAppFilter.setText(input.regular.appNameFilter)
    }

    override val inputForTasker: TaskerInput<NotificationInterceptedEventInput>
        get() = TaskerInput(NotificationInterceptedEventInput(
            appNameFilter = binding.editTextAppFilter.text.toString().trim()
        ))

    override val context get() = applicationContext
    private val taskerHelper by lazy { NotificationInterceptedEventHelper(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityConfigNotificationInterceptedEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonOK.setOnClickListener {
            // Check if notification listener permission is granted
            if (!isNotificationListenerEnabled()) {
                // Show dialog to grant permission
                showNotificationAccessDialog()
            } else {
                // Permission is granted, proceed
                finishConfiguration()
            }
        }

        taskerHelper.onCreate()
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val packageName = packageName
        val flat = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        
        if (flat.isNullOrEmpty()) {
            return false
        }
        
        val names = flat.split(":".toRegex()).dropLastWhile { it.isEmpty() }
        for (name in names) {
            val componentName = ComponentName.unflattenFromString(name)
            if (componentName != null) {
                if (packageName == componentName.packageName) {
                    Log.d(TAG, "Notification listener permission is granted")
                    return true
                }
            }
        }
        
        Log.d(TAG, "Notification listener permission is NOT granted")
        return false
    }

    private fun showNotificationAccessDialog() {
        try {
            Toast.makeText(
                this,
                "Please grant notification access to this app in the next screen",
                Toast.LENGTH_LONG
            ).show()
            
            // Open notification access settings
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivityForResult(intent, REQUEST_NOTIFICATION_ACCESS)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error opening notification access settings", e)
            Toast.makeText(
                this,
                "Please manually grant notification access in Android Settings > Apps > Special Access > Notification Access",
                Toast.LENGTH_LONG
            ).show()
            finishConfiguration()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQUEST_NOTIFICATION_ACCESS -> {
                // Check if permission was granted
                if (isNotificationListenerEnabled()) {
                    Toast.makeText(this, "Notification access granted!", Toast.LENGTH_SHORT).show()
                    finishConfiguration()
                } else {
                    Toast.makeText(
                        this,
                        "Notification access is required for this event to work",
                        Toast.LENGTH_LONG
                    ).show()
                    // Still finish configuration, user can enable it later
                    finishConfiguration()
                }
            }
        }
    }

    private fun finishConfiguration() {
        Log.d(TAG, "Finishing configuration with filter='${binding.editTextAppFilter.text.toString().trim()}'")

        taskerHelper.finishForTasker()
    }
}
