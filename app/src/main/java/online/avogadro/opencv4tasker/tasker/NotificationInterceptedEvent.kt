package online.avogadro.opencv4tasker.tasker

import android.content.Context
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.extensions.requestQuery
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess

/**
 * Input configuration for the Notification Intercepted event
 */
@TaskerInputRoot
class NotificationInterceptedEventInput @JvmOverloads constructor(
    @field:TaskerInputField("enabled") var enabled: Boolean = true,
    @field:TaskerInputField("appNameFilter") var appNameFilter: String = ""
)

/**
 * Output data for the Notification Intercepted event
 */
@TaskerInputRoot
@TaskerOutputObject
class NotificationInterceptedEvent(
    @get:TaskerOutputVariable("notification_title")
    val notificationTitle: String,
    
    @get:TaskerOutputVariable("notification_text")
    val notificationText: String,
    
    @get:TaskerOutputVariable("image_path")
    val imagePath: String,
    
    @get:TaskerOutputVariable("app_package")
    val appPackage: String,
    
    @get:TaskerOutputVariable("app_name")
    val appName: String
)

/**
 * Tasker Plugin Event Helper for Notification Intercepted
 */
class NotificationInterceptedEventHelper(config: TaskerPluginConfig<NotificationInterceptedEventInput>) : 
    TaskerPluginConfigHelper<NotificationInterceptedEventInput, NotificationInterceptedEvent, NotificationInterceptedEventRunner>(config) {
    
    override val runnerClass: Class<NotificationInterceptedEventRunner> 
        get() = NotificationInterceptedEventRunner::class.java
    
    override val inputClass = NotificationInterceptedEventInput::class.java
    
    override val outputClass = NotificationInterceptedEvent::class.java
    
    override fun addToStringBlurb(input: TaskerInput<NotificationInterceptedEventInput>, blurbBuilder: StringBuilder) {
        if (input.regular.enabled) {
            if (input.regular.appNameFilter.isNotEmpty()) {
                blurbBuilder.append(" monitoring notifications from apps containing '${input.regular.appNameFilter}'")
            } else {
                blurbBuilder.append(" monitoring notifications with images from all apps")
            }
        } else {
            blurbBuilder.append(" notification monitoring disabled")
        }
    }
}

/**
 * Event Runner - dummy implementation since events are triggered externally
 */
class NotificationInterceptedEventRunner : TaskerPluginRunnerAction<NotificationInterceptedEventInput, NotificationInterceptedEvent>() {
    
    override fun run(context: Context, input: TaskerInput<NotificationInterceptedEventInput>): TaskerPluginResult<NotificationInterceptedEvent> {
        // This is just a placeholder - the actual event triggering is handled by the NotificationInterceptorService
        // Return success to indicate the event configuration is valid
        return TaskerPluginResultSucess()
    }
}

fun Context.triggerTaskerEventNotificationIntercepted(bundle: Any?) = ActivityConfigNotificationInterceptedEvent::class.java.requestQuery(this, bundle)