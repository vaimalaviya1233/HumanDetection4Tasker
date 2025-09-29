package online.avogadro.opencv4tasker.tasker

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.service.notification.NotificationListenerService
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultErrorWithOutput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import online.avogadro.opencv4tasker.databinding.ActivityConfigCancelNotificationBinding
import online.avogadro.opencv4tasker.notification.NotificationInterceptorService

class CancelNotificationActionHelper(config: TaskerPluginConfig<CancelNotificationInput>) :
    TaskerPluginConfigHelper<CancelNotificationInput, CancelNotificationOutput, CancelNotificationActionRunner>(config) {

    override val runnerClass: Class<CancelNotificationActionRunner>
        get() = CancelNotificationActionRunner::class.java

    override val inputClass = CancelNotificationInput::class.java

    override val outputClass = CancelNotificationOutput::class.java

    override fun addToStringBlurb(input: TaskerInput<CancelNotificationInput>, blurbBuilder: StringBuilder) {
        blurbBuilder.append(" cancel notification")
        input.regular.notificationKey?.let {
            blurbBuilder.append(" (Key: ${it.take(20)}...)")
        }
    }
}

class ActivityConfigCancelNotificationAction : Activity(), TaskerPluginConfig<CancelNotificationInput> {

    private lateinit var binding: ActivityConfigCancelNotificationBinding

    override fun assignFromInput(input: TaskerInput<CancelNotificationInput>) {
        binding?.editNotificationKey?.setText(input.regular.notificationKey ?: "")
    }

    override val inputForTasker: TaskerInput<CancelNotificationInput>
        get() {
            val notificationKey = binding?.editNotificationKey?.text?.toString()?.takeIf { it.isNotBlank() }
            return TaskerInput(CancelNotificationInput(notificationKey))
        }

    override val context get() = applicationContext

    private val taskerHelper by lazy { CancelNotificationActionHelper(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigCancelNotificationBinding.inflate(layoutInflater)

        binding.buttonOK.setOnClickListener {
            taskerHelper.finishForTasker()
        }

        setContentView(binding.root)
        taskerHelper.onCreate()
    }
}

class CancelNotificationActionRunner : TaskerPluginRunnerAction<CancelNotificationInput, CancelNotificationOutput>() {

    override fun run(context: Context, input: TaskerInput<CancelNotificationInput>): TaskerPluginResult<CancelNotificationOutput> {
        try {
            val notificationKey = input.regular.notificationKey

            // Check if we have the required parameter
            if (notificationKey.isNullOrBlank()) {
                return TaskerPluginResultSucess(CancelNotificationOutput(
                    false,
                    "Missing required parameter: notification key is required"
                ))
            }

            // Try to cancel the notification using the key
            val success = cancelNotificationByKey(notificationKey)

            val message = if (success) {
                "Notification canceled successfully"
            } else {
                "Failed to cancel notification. Make sure the notification service is enabled and has proper permissions."
            }

            return TaskerPluginResultSucess(CancelNotificationOutput(success, message))

        } catch (e: Exception) {
            return TaskerPluginResultErrorWithOutput(-1, "Error canceling notification: ${e.message}")
        }
    }

    private fun cancelNotificationByKey(notificationKey: String): Boolean {
        return NotificationInterceptorService.cancelNotificationByKey(notificationKey)
    }
}